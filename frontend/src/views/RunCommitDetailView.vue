<template>
  <v-container>
    <v-row v-if="!show404 && !run && !commit">
      <v-col>
        <v-skeleton-loader type="card"></v-skeleton-loader>
      </v-col>
    </v-row>
    <v-row v-if="run" no-gutters>
      <v-col>
        <run-detail :run="run"></run-detail>
      </v-col>
    </v-row>
    <v-row v-if="commit" no-gutters>
      <v-col>
        <commit-detail :commit="commit"></commit-detail>
        <run-timeline :runs="commit.runs"></run-timeline>
      </v-col>
    </v-row>
    <v-row v-if="show404">
      <page-404></page-404>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Commit, Run, CommitTaskSource } from '@/store/types'
import { vxm } from '@/store'
import NotFound404 from './NotFound404.vue'
import RunDetail from '@/components/rundetail/RunDetail.vue'
import CommitDetail from '@/components/rundetail/CommitDetail.vue'
import { Watch } from 'vue-property-decorator'
import RunTimeline from '@/components/rundetail/RunTimeline.vue'

@Component({
  components: {
    'page-404': NotFound404,
    'run-detail': RunDetail,
    'commit-detail': CommitDetail,
    'run-timeline': RunTimeline
  }
})
export default class RunCommitDetailView extends Vue {
  private run: Run | null = null
  private commit: Commit | null = null
  private show404: boolean = false

  private get firstComponent(): string {
    return this.$route.params['first']
  }

  private get secondComponent(): string | undefined {
    return this.$route.params['second']
  }

  @Watch('firstComponent')
  @Watch('secondComponent')
  private async fetchCommitAndRun() {
    this.show404 = false
    this.run = null
    this.commit = null

    if (this.firstComponent && !this.secondComponent) {
      // we have a run id
      this.run = await vxm.commitDetailComparisonModule.fetchRun(
        this.firstComponent
      )
      if (this.run.source instanceof CommitTaskSource) {
        this.commit = await vxm.commitDetailComparisonModule.fetchCommit({
          repoId: this.run.source.commitDescription.repoId,
          commitHash: this.run.source.commitDescription.hash
        })
      }
    } else if (this.firstComponent && this.secondComponent) {
      // we have a repo id and run id
      this.commit = await vxm.commitDetailComparisonModule.fetchCommit({
        repoId: this.firstComponent,
        commitHash: this.secondComponent
      })
      if (this.commit.runs.length > 0) {
        this.run = await vxm.commitDetailComparisonModule.fetchRun(
          this.commit.runs[0].runId
        )
      }
    } else {
      // We got weird things
      this.show404 = true
    }
  }

  mounted() {
    this.fetchCommitAndRun()
  }
}
</script>

<style scoped>
</style>
