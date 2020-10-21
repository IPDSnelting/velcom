<template>
  <v-card>
    <v-card-title>
      <v-toolbar dark color="primary">Runner output (StdErr)</v-toolbar>
    </v-card-title>
    <v-card-text>
      <v-alert type="error" class="mx-4" v-if="loadingError">
        No output received in my last request. Maybe the task finished
        executing?
      </v-alert>
      <div class="runner-output mx-2">
        <span
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
        </span>
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

  @Watch('taskId')
  private async update() {
    if (!this.taskId) {
      return
    }

    let taskId = '0e394e90-e895-4f12-8280-1e47e15c659d'
    if (vxm.queueModule.workers.length > 0) {
      taskId =
        vxm.queueModule.workers[0].workingOn ||
        '0e394e90-e895-4f12-8280-1e47e15c659d'
    }

    const newOutput = await vxm.queueModule.fetchRunnerOutput(taskId)
    if (newOutput !== null) {
      this.output = newOutput
    }
    this.loadingError = newOutput === null
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
.runner-output .line {
  display: block;
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