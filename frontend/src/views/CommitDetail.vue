<template>
  <div class="commit-detail">
    <h1>This is the detail page of commit {{ hash }} in repo {{ repoID }}</h1>
    <commit-information :commit="commit"></commit-information>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Commit, CommitComparison } from '../store/types'
import { vxm } from '../store'
import CommitInformation from '../components/CommitInformation.vue'

@Component({
  components: {
    'commit-information': CommitInformation
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
