<template>
  <v-card>
    <v-card-title>
      <v-toolbar dark color="primary">
        Runner output (standard error stream)
      </v-toolbar>
    </v-card-title>
    <v-card-text>
      <v-alert
        :type="taskInProgress ? 'error' : 'warning'"
        class="mx-4"
        v-if="loadingError"
      >
        No output received in the last request.
        <span v-if="!taskInProgress && taskInQueue">
          It looks like the task is not scheduled on a runner right now?
        </span>
        <span v-if="!taskInQueue">
          It looks like the task is no longer in the queue? Maybe it is
          finished?
        </span>
      </v-alert>
      <div class="runner-output mx-2">
        <div
          v-for="{ lineNumber, text, classes } in lines"
          :key="lineNumber"
          class="line"
          :class="classes"
        >
          <span
            class="mr-2 font-weight-bold align-end text-right d-inline-block"
            style="min-width: 4ch; user-select: none"
            >{{ lineNumber }}</span
          >
          {{ text }}
        </div>
      </div>
      <v-row align="center" justify="center">
        <v-col cols="3">
          <v-progress-linear
            indeterminate
            :color="loadingError ? 'error' : 'primary'"
            rounded
          ></v-progress-linear>
        </v-col>
      </v-row>
    </v-card-text>
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop, Watch } from 'vue-property-decorator'
import { StreamedRunnerOutput, TaskId } from '@/store/types'
import { vxm } from '@/store'

@Component
export default class TaskRunnerOutput extends Vue {
  @Prop()
  private taskId!: TaskId

  private timer: number | null = null
  private output: StreamedRunnerOutput | null = null
  private loadingError: boolean = false
  private taskInQueue: boolean = true

  private get taskInProgress(): boolean {
    return !!vxm.queueModule.workers.find(it => it.workingOn === this.taskId)
  }

  @Watch('taskId')
  private async update() {
    if (!this.taskId) {
      return
    }

    const newOutput = await vxm.queueModule.fetchRunnerOutput(this.taskId)
    if (newOutput !== null) {
      this.output = newOutput
    }
    this.loadingError = newOutput === null

    if (this.loadingError) {
      await vxm.queueModule.fetchQueue()
      this.taskInQueue = !!vxm.queueModule.openTasks.find(
        it => it.id === this.taskId
      )
      this.$emit('loading-failed')
      return
    }

    const element = this.$el.getElementsByClassName('runner-output')[0]
    if (element.scrollHeight - element.scrollTop === element.clientHeight) {
      Vue.nextTick(() => {
        element.scrollTop = element.scrollHeight
      })
    }
  }

  private get lines(): {
    lineNumber: number
    text: string
    classes: string[]
  }[] {
    const output = this.output
    if (!output) {
      return []
    }
    const levelRegex = [
      {
        level: 'warning',
        regex: this.startsRoughlyWithLevel('warning')
      },
      {
        level: 'error',
        regex: this.startsRoughlyWithLevel('error')
      }
    ]
    return output.outputLines.map((line, index) => ({
      lineNumber: index + output.indexOfFirstLine + 1,
      text: line,
      classes: levelRegex.map(it =>
        it.regex.exec(line) !== null ? `text--${it.level}` : ''
      )
    }))
  }

  private startsRoughlyWithLevel(level: string): RegExp {
    const regexString = `^\\s*((\\[${level}\\])|${level})`
    return new RegExp(regexString, 'iu')
  }

  private mounted() {
    this.update()
    this.timer = setInterval(() => {
      this.update()
    }, 5000)
  }

  private destroyed() {
    if (this.timer !== null) {
      clearInterval(this.timer)
    }
  }
}
</script>

<!--suppress CssUnresolvedCustomProperty -->
<style scoped>
.runner-output {
  font-family: monospace;
  white-space: pre-line;
  max-height: 90vh;
  overflow-y: scroll;
}
.theme--light .runner-output .line:hover {
  background-color: var(--v-rowHighlight-lighten1) !important;
}
.theme--dark .runner-output .line:hover {
  background-color: var(--v-rowHighlight-darken1) !important;
}

.text--warning {
  color: var(--v-warning-base);
}
.text--error {
  color: var(--v-error-base);
}
</style>
