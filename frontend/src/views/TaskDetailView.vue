<template>
  <v-container>
    <v-row v-if="!show404 && task === null && commit === null">
      <v-col>
        <v-skeleton-loader type="card"></v-skeleton-loader>
      </v-col>
    </v-row>
    <v-row no-gutters v-if="nudgeToRunDetail">
      <v-col>
        <v-snackbar timeout="-1" multi-line :value="true" shaped color="info">
          This task no longer exists, but it looks like a run exists for it! Do
          you want to navigate there?
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
    <v-row v-if="task" no-gutters>
      <v-col>
        <task-runner-output
          :task-id="taskId"
          @loading-failed="loadingOutputFailed"
        ></task-runner-output>
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
import { Commit, CommitTaskSource, Task, TaskSource } from '@/store/types'
import { Watch } from 'vue-property-decorator'
import { vxm } from '@/store'
import CommitDetail from '@/components/rundetail/CommitDetail.vue'
import TaskRunnerOutput from '@/components/TaskRunnerOutput.vue'

@Component({
  components: {
    'task-runner-output': TaskRunnerOutput,
    'page-404': NotFound404,
    'commit-detail': CommitDetail
  }
})
export default class TaskDetailView extends Vue {
  private show404: boolean = false

  private task: Task | null = null
  private commit: Commit | null = null
  private nudgeToRunDetail: boolean = false

  private get taskId() {
    return this.$route.params.taskId
  }

  @Watch('taskId')
  private async update() {
    this.show404 = false

    await vxm.queueModule.fetchQueue()

    const myTask = vxm.queueModule.openTasks.find(it => it.id === this.taskId)
    this.task = myTask || null

    if (!this.task) {
      await this.handleTaskNotFound()
      return
    }
    await this.handleSource(this.task.source)
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
    if (!vxm.queueModule.openTasks.find(it => it.id === this.taskId)) {
      this.handleTaskNotFound()
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
    this.update()
  }
}
</script>
