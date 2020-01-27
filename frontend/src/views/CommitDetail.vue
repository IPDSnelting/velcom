<template>
  <v-container>
    <v-row>
      <commit-information v-if="commit" :commit="commit" :hasExistingBenchmark="hasRun"></commit-information>
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
            <span class="mx-1">{{ comparison.second.errorMessage }}</span>
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
            <v-toolbar dark color="primary">
              <v-toolbar-title>Benchmark results</v-toolbar-title>
            </v-toolbar>
          </v-card-title>
          <v-card-text>
            <commit-info-table
              v-if="!isError"
              :comparison="comparison"
              :compare="false"
            ></commit-info-table>
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
  MeasurementID
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

  get comparison(): CommitComparison | null {
    return vxm.commitComparisonModule.commitComparison(
      this.repoID,
      null,
      this.hash
    )
  }

  created() {
    vxm.commitComparisonModule.fetchCommitComparison({
      repoId: this.repoID,
      first: undefined,
      second: this.hash
    })
  }
}
</script>
