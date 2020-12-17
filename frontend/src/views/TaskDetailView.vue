<template>
  <v-container>
    <v-row v-if="!show404 && taskInfo === null && commit === null">
      <v-col>
        <v-skeleton-loader type="card"></v-skeleton-loader>
      </v-col>
    </v-row>
    <v-row no-gutters v-if="nudgeToRunDetail">
      <v-col>
        <v-snackbar timeout="-1" multi-line :value="true" shaped color="info">
          This task no longer exists, but it looks like a run exists for it!
          Click "to the run" on the right to navigate there.
          <template v-slot:action="{ attrs }">
            <v-btn
              color="ping"
              text
              dark
              v-bind="attrs"
              @click="navigateToRespectiveRun"
            >
              To the run
            </v-btn>
          </template>
        </v-snackbar>
      </v-col>
    </v-row>
    <v-row v-if="commit" no-gutters>
      <v-col>
        <commit-detail :commit="commit"></commit-detail>
      </v-col>
    </v-row>
    <v-row v-if="tarSource" justify="center">
      <v-col cols="auto">
        <tar-overview :tar-source="tarSource"></tar-overview>
      </v-col>
    </v-row>
    <v-row v-if="taskInfo" no-gutters>
      <v-col>
        <task-runner-output
          :task-id="taskId"
          @loading-failed="loadingOutputFailed"
        >
          <template #toolbar-right v-if="taskInfo">
            <v-spacer></v-spacer>
            <v-container
              fluid
              class="ma-0 pa-0"
              style="flex: 0 0 0; flex-basis: content"
            >
              <v-row no-gutters justify="end">
                <v-col class="ma-0 pa-0" cols="auto">
                  #{{ taskInfo.position + 1 }}
                </v-col>
              </v-row>
              <v-row no-gutters justify="end">
                <v-col class="ma-0 pa-0" cols="auto">
                  <span v-if="queuedSinceText" class="text-sm-body-1">
                    {{ queuedSinceText }}
                  </span>
                </v-col>
              </v-row>
            </v-container>
          </template>
        </task-runner-output>
      </v-col>
    </v-row>
    <v-row v-if="show404">
      <page-404
        title="Task not found"
        subtitle="Maybe try refreshing or add it to the queue"
      ></page-404>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import NotFound404 from '@/views/NotFound404.vue'
import {
  Commit,
  CommitTaskSource,
  TarTaskSource,
  TaskSource
} from '@/store/types'
import { Watch } from 'vue-property-decorator'
import { vxm } from '@/store'
import CommitDetail from '@/components/rundetail/CommitDetail.vue'
import TaskRunnerOutput from '@/components/TaskRunnerOutput.vue'
import InlineMinimalRepoDisplay from '@/components/InlineMinimalRepoDisplay.vue'
import { TaskInfo } from '@/store/modules/queueStore'
import { formatDurationShort } from '@/util/TimeUtil'
import TarOverview from '@/components/overviews/TarOverview.vue'

@Component({
  components: {
    'tar-overview': TarOverview,
    'inline-minimal-repo-display': InlineMinimalRepoDisplay,
    'task-runner-output': TaskRunnerOutput,
    'page-404': NotFound404,
    'commit-detail': CommitDetail
  }
})
export default class TaskDetailView extends Vue {
  private show404: boolean = false

  private taskInfo: TaskInfo | null = null
  private commit: Commit | null = null
  private nudgeToRunDetail: boolean = false

  private timerIds: number[] = []
  private queuedSinceText: string | null = null

  private get taskId() {
    return this.$route.params.taskId
  }

  private get tarSource() {
    if (!this.taskInfo || !this.taskInfo.task) {
      return null
    }
    if (this.taskInfo.task.source instanceof TarTaskSource) {
      return this.taskInfo.task.source
    }
    return null
  }

  @Watch('taskId')
  private async update() {
    this.show404 = false

    this.taskInfo = await vxm.queueModule.fetchTaskInfo(this.taskId)

    if (!this.taskInfo) {
      await this.handleTaskNotFound()
      return
    }
    await this.handleSource(this.taskInfo.task.source)
  }

  private async handleSource(source: TaskSource) {
    if (source instanceof CommitTaskSource) {
      this.commit = await vxm.commitDetailComparisonModule.fetchCommit({
        repoId: source.commitDescription.repoId,
        commitHash: source.commitDescription.hash
      })
    }
  }

  private async handleTaskNotFound() {
    try {
      const run = await vxm.commitDetailComparisonModule.fetchRun(this.taskId)
      this.nudgeToRunDetail = true
      await this.handleSource(run.run.source)
    } catch (e) {
      this.show404 = true
    }
  }

  private loadingOutputFailed() {
    if (this.taskInfo === null) {
      this.handleTaskNotFound()
    }
  }

  private updateDuration() {
    if (!this.taskInfo) {
      this.queuedSinceText = null
      return
    }

    if (this.taskInfo.runningSince) {
      this.queuedSinceText =
        'running for ' +
        formatDurationShort(this.taskInfo.runningSince, new Date())
    } else {
      this.queuedSinceText =
        'queued for  ' +
        formatDurationShort(this.taskInfo.task.since, new Date())
    }
  }

  private navigateToRespectiveRun() {
    this.$router.push({
      name: 'run-detail',
      params: {
        first: this.taskId
      }
    })
  }

  private mounted() {
    const updateTaskTimer = setInterval(() => {
      this.update()
    }, 10_000)
    const updateDurationTimer = setInterval(() => this.updateDuration(), 1000)

    this.timerIds = [updateTaskTimer, updateDurationTimer]
    this.update()
  }

  private destroyed() {
    this.timerIds.forEach(clearInterval)
  }
}
</script>
