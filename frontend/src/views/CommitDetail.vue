<template>
  <v-container>
    <v-row>
      <commit-information v-if="myRun" :run="myRun"></commit-information>
    </v-row>
    <v-row v-if="isError">
      <v-col>
        <v-card>
          <v-card-title>
            <v-toolbar dark color="error">
              <v-toolbar-title>Benchmarking this commit resulted in an error</v-toolbar-title>
            </v-toolbar>
          </v-card-title>
          <v-card-text v-if="myRun">
            <div class="title">Error message:</div>
            <span class="mx-1">{{ myRun.errorMessage }}</span>
          </v-card-text>
          <v-card-text v-else>
            No data
            <em>and</em> no error message found :/
            Maybe the page hasn't fully loaded yet?
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
    <v-row v-if="!isError">
      <v-col>
        <v-card>
          <v-card-title>
            <v-toolbar dark color="primary">
              <v-toolbar-title>Benchmark results</v-toolbar-title>
            </v-toolbar>
          </v-card-title>
          <v-card-text>
            <commit-info-table v-if="!isError" :run="myRun" :previousRun="previousRun"></commit-info-table>
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
  private myRun: Run | null = null
  private previousRun: Run | null = null

  get repoID() {
    return this.$route.params.repoID
  }

  get hash() {
    return this.$route.params.hash
  }

  get isError() {
    return !this.myRun || this.myRun.errorMessage
  }

  get commit(): Commit | null {
    return this.myRun ? this.myRun.commit : null
  }

  async created() {
    let comparison = await vxm.commitComparisonModule.fetchCommitComparison({
      repoId: this.repoID,
      first: undefined,
      second: this.hash
    })

    if (comparison.first) {
      this.previousRun = comparison.first
    }
    if (comparison.second) {
      this.myRun = comparison.second
    }
  }
}
</script>
