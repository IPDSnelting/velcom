<template>
  <v-container>
    <v-row v-if="!show404 && !runWithDifferences && !commit">
      <v-col>
        <v-skeleton-loader type="card"></v-skeleton-loader>
      </v-col>
    </v-row>
    <v-row v-if="commit" no-gutters>
      <v-col>
        <commit-detail :commit="commit"></commit-detail>
      </v-col>
    </v-row>
    <v-row v-if="runWithDifferences" no-gutters>
      <v-col>
        <run-detail
          @navigateToDetailGraph="navigateToDetailGraph"
          :runWithDifferences="runWithDifferences"
        ></run-detail>
      </v-col>
    </v-row>
    <v-row v-if="finishedLoading && !runWithDifferences">
      <v-col>
        <v-card>
          <v-card-title>
            <v-toolbar color="primary" dark>
              Benchmark Status: Not much's going on here
            </v-toolbar>
          </v-card-title>
          <v-card-text class="text-center">
            Hey, this commit was
            <span class="font-weight-bold">never benchmarked</span>. Maybe it is
            already in the queue, but if it is not, you can place it there.
            <br />To do so you need
            <span class="font-weight-bold">admin privileges</span>
            (or a way to ask your friendly admin!)
            <br />Then just click the
            <span class="font-weight-bold">icon next to the Repo name</span>
            and id that says "benchmark" when you hover over it :)
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
    <v-row v-if="show404">
      <page-404></page-404>
    </v-row>
    <v-row v-if="commit && runWithDifferences" no-gutters>
      <v-col>
        <run-timeline
          :selectedRunId="runWithDifferences ? runWithDifferences.run.id : null"
          :runs="commit.runs"
        ></run-timeline>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Watch } from 'vue-property-decorator'
import { vxm } from '@/store'
import {
  Commit,
  CommitTaskSource,
  Dimension,
  RunWithDifferences
} from '@/store/types'
import NotFound404 from './NotFound404.vue'
import RunDetail from '@/components/rundetail/RunDetail.vue'
import CommitDetail from '@/components/rundetail/CommitDetail.vue'
import RunTimeline from '@/components/rundetail/RunTimeline.vue'
import { NotFoundError } from '@/store/modules/commitDetailComparisonStore'
import { showCommitInDetailGraph } from '@/util/GraphNavigation'

@Component({
  components: {
    'page-404': NotFound404,
    'run-detail': RunDetail,
    'commit-detail': CommitDetail,
    'run-timeline': RunTimeline
  }
})
export default class RunCommitDetailView extends Vue {
  private runWithDifferences: RunWithDifferences | null = null
  private commit: Commit | null = null
  private show404: boolean = false
  private finishedLoading: boolean = false

  private get firstComponent(): string {
    return this.$route.params['first']
  }

  private get secondComponent(): string | undefined {
    return this.$route.params['second']
  }

  private async navigateToDetailGraph(dimension: Dimension) {
    if (!this.commit) {
      return
    }
    await showCommitInDetailGraph(
      dimension,
      this.commit.repoId,
      this.commit.hash,
      this.commit.authorDate,
      this.$router
    )
  }

  @Watch('firstComponent')
  @Watch('secondComponent')
  private async fetchCommitAndRun() {
    try {
      await this.fetchCommitAndRunImpl()
    } catch (e) {
      if (e instanceof NotFoundError) {
        this.show404 = true
        return
      }
      throw e
    }
  }

  private async fetchCommitAndRunImpl() {
    this.show404 = false
    this.runWithDifferences = null
    this.commit = null
    this.finishedLoading = false

    if (this.firstComponent && !this.secondComponent) {
      // we have a run id
      this.runWithDifferences = await vxm.commitDetailComparisonModule.fetchRun(
        this.firstComponent
      )
      const run = this.runWithDifferences.run

      if (run.source instanceof CommitTaskSource) {
        this.commit = await vxm.commitDetailComparisonModule.fetchCommit({
          repoId: run.source.commitDescription.repoId,
          commitHash: run.source.commitDescription.hash
        })
      }
    } else if (this.firstComponent && this.secondComponent) {
      // we have a repo id and commit hash
      this.commit = await vxm.commitDetailComparisonModule.fetchCommit({
        repoId: this.firstComponent,
        commitHash: this.secondComponent
      })
      if (this.commit.runs.length > 0) {
        this.runWithDifferences = await vxm.commitDetailComparisonModule.fetchRun(
          this.commit.runs[0].runId
        )
      }
    } else {
      // We got weird things
      this.show404 = true
    }

    this.finishedLoading = true
  }

  mounted(): void {
    this.fetchCommitAndRun()
  }
}
</script>

<style scoped></style>
