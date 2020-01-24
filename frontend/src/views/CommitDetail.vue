<template>
  <div class="commit-detail">
    <commit-information v-if="myRun" :run="myRun"></commit-information>
    <!-- <commit-info-table v-if="myRun" :run="myRun" :previousRun="previousRun"></commit-info-table> -->
  </div>
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

  get commit(): Commit | null {
    // TODO: Fetch real data
    return this.myRun ? this.myRun.commit : null
  }

  async created() {
    let comparison = await vxm.commitComparisonModule.fetchCommitComparison({
      repoId: this.repoID,
      first: undefined,
      second: this.hash
    })
    console.log(comparison)

    if (comparison.first) {
      this.previousRun = comparison.first
    }
    if (comparison.second) {
      this.myRun = comparison.second
    }
  }
}
</script>
