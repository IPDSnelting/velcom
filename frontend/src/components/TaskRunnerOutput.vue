<template>
  <v-card>
    <v-card-title>
      <v-toolbar dark color="primary">Runner output (StdErr)</v-toolbar>
    </v-card-title>
    <v-card-text>
      <div class="runner-output" v-if="output">
        {{ output }}
      </div>
      <div v-else>Nothing there (yet)?</div>
    </v-card-text>
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop, Watch } from 'vue-property-decorator'
import { TaskId } from '@/store/types'
import { vxm } from '@/store'

@Component
export default class TaskRunnerOutput extends Vue {
  @Prop()
  private taskId!: TaskId

  private timer: number | null = null
  private output: string | null = null

  @Watch('taskId')
  private async update() {
    this.output = await vxm.queueModule.fetchRunnerOutput(this.taskId)
  }

  private mounted() {
    this.update()
    this.timer = setInterval(
      () => {
        this.taskId = vxm.queueModule.workers[0].workingOn!
        this.update()
      },
      1000,
      1000
    )
  }

  private destroyed() {
    if (this.timer !== null) {
      clearInterval(this.timer)
    }
  }
}
</script>

<style scoped>
.runner-output {
  font-family: monospace;
  white-space: pre-line;
}
</style>
