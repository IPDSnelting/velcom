<template>
  <v-container>
    <v-row>
      <commit-information
        v-if="commit"
        :prevCommit="comparison.firstCommit"
        :nextCommit="nextCommit"
        :commit="commit"
        :hasExistingBenchmark="hasRun"
      ></commit-information>
    </v-row>
    <v-row v-if="hasRun && isError">
      <v-col>
        <v-card>
          <v-card-title>
            <v-toolbar dark color="error">
              <v-toolbar-title>Benchmarking this commit resulted in an error</v-toolbar-title>
            </v-toolbar>
          </v-card-title>
          <v-card-text v-if="comparison.second">
            <div class="title">Error message:</div>
            <div class="ma-5 error-message">
              <div v-for="{ isHeader, value} in errorMessageParts" :key="value" class="mb-4">
                <div v-if="isHeader" class="mt-2 error-message-header">{{ value }}</div>
                <div v-else>{{ value }}</div>
              </div>
            </div>
          </v-card-text>
          <v-card-text v-else>
            No data
            <em>and</em> no error message found :/
            Maybe the page hasn't fully loaded yet?
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
    <v-row v-if="hasRun && !isError">
      <v-col>
        <v-card>
          <v-card-title>
            <v-toolbar dense dark color="primary">
              <v-toolbar-title>Benchmark results</v-toolbar-title>
            </v-toolbar>
          </v-card-title>
          <v-card-text>
            <commit-info-table v-if="!isError" :comparison="comparison" :compare="false"></commit-info-table>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import {
  Commit,
  CommitComparison,
  Run,
  Measurement,
  MeasurementID,
  CommitInfo
} from '../store/types'
import { vxm } from '../store'
import CommitInformation from '../components/CommitInformation.vue'
import CommitInfoTable from '../components/CommitInfoTable.vue'

@Component({
  components: {
    'commit-information': CommitInformation,
    'commit-info-table': CommitInfoTable
  }
})
export default class CommitDetail extends Vue {
  get repoID() {
    return this.$route.params.repoID
  }

  get hash() {
    return this.$route.params.hash
  }

  get hasRun(): boolean {
    return !!this.comparison && !!this.comparison.second
  }

  get isError() {
    return this.hasRun && this.comparison!.second!.errorMessage
  }

  get commit(): Commit | null {
    return this.comparison ? this.comparison.secondCommit : null
  }

  get nextCommit(): Commit | null {
    return this.info ? this.info.nextCommit : null
  }

  get comparison(): CommitComparison | null {
    return this.info ? this.info.comparison : null
  }

  get info(): CommitInfo | null {
    return vxm.commitComparisonModule.commitInfo(this.repoID, null, this.hash)
  }

  get errorMessageParts(): { isHeader: boolean; value: string }[] {
    let message = this.comparison!.second!.errorMessage!
    let tempAccumulator: {
      parsingHeader: boolean
      tempArray: string[]
      result: string[][]
    } = { parsingHeader: false, tempArray: [], result: [] }

    message.split('\n').reduce((accumulated, next) => {
      if (next.startsWith('###')) {
        // Opening
        if (!accumulated.parsingHeader) {
          if (accumulated.tempArray.length !== 0) {
            accumulated.result.push(accumulated.tempArray)
          }
          accumulated.tempArray = []
          accumulated.tempArray.push(next)
        } else {
          // Closing
          accumulated.tempArray.push(next)
          if (accumulated.tempArray.length !== 0) {
            accumulated.result.push(accumulated.tempArray)
          }
          accumulated.tempArray = []
        }
        accumulated.parsingHeader = !accumulated.parsingHeader
        return accumulated
      }
      accumulated.tempArray.push(next)
      return accumulated
    }, tempAccumulator)

    if (tempAccumulator.tempArray.length > 0) {
      tempAccumulator.result.push(tempAccumulator.tempArray)
    }

    return tempAccumulator.result.map(array => ({
      isHeader: array[0].startsWith('###'),
      value: array.join('\n')
    }))
  }

  created() {
    vxm.commitComparisonModule.fetchCommitInfo({
      repoId: this.repoID,
      first: undefined,
      second: this.hash
    })
  }
}
</script>

<style scoped>
.error-message {
  font-family: monospace;
  white-space: pre-wrap;
  line-height: 1.2em;
  font-weight: bolder;
}
.error-message-header {
  color: green;
}
</style>
