<template>
  <div class="commit-detail">
    <h1>This is the detail page of commit {{ hash }} in repo {{ repoID }}</h1>
    <commit-information :commit="commit"></commit-information>
    <commit-info-table :run="run" :previousRun="prevRun"></commit-info-table>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Commit, CommitComparison, Run, Measurement, MeasurementID } from '../store/types'
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

  async fetchRun() {
    let comparison: CommitComparison = await vxm.commitComparisonModule.fetchCommitComparison(
      {
        repoId: this.repoID,
        first: this.hash,
        second: undefined
      }
    )
  }

  get run(): Run {
    // TODO: Fetch real data
    return new Run(
      this.commit,
      1579768184,
      1579768194,
      [
        new Measurement(
          new MeasurementID('a', 'b'),
          'cm',
          'LESS_IS_BETTER',
          [1, 2, 3, 4, 5],
          3
        ),
        new Measurement(
          new MeasurementID('c', 'd'),
          'cm',
          'LESS_IS_BETTER',
          [1, 2, 3, 4, 5],
          2
        )
      ]
    )
  }

  get prevRun(): Run {
    // TODO: Fetch real data
    return new Run(
      this.commit,
      1579768184,
      1579768194,
      [
        new Measurement(
          new MeasurementID('a', 'b'),
          'cm',
          'LESS_IS_BETTER',
          [1, 2, 3, 4, 5],
          2
        ),
        new Measurement(
          new MeasurementID('c', 'd'),
          'cm',
          'LESS_IS_BETTER',
          [1, 2, 3, 4, 5],
          10
        )
      ]
    )
  }

  get commit(): Commit {
    // TODO: Fetch real data
    return new Commit(
      'Test repo',
      'my hash',
      'Peter Doe',
      1579768184,
      'Hello commiter',
      1579768184,
      'THis is the message of the commit \n With multiple lines and a lot \n of fun',
      ['parent!']
    )
  }
}
</script>
