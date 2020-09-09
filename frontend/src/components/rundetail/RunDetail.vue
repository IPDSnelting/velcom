<template>
  <v-container fluid class="pa-0 ma-0">
    <v-row no-gutters>
      <v-col v-if="error !== undefined">
        <v-card>
          <v-card-title>
            <v-toolbar dark :color="runColor">{{ errorType }} Error</v-toolbar>
          </v-card-title>
          <v-card-text class="mx-2 error-text">{{ error }}</v-card-text>
        </v-card>
      </v-col>
    </v-row>
    <v-row no-gutters>
      <v-col v-if="measurements !== undefined">
        <v-card>
          <v-card-title>
            <v-toolbar dark color="primary">Run Result</v-toolbar>
          </v-card-title>
          <v-card-text class="mx-2">
            <measurements-display
              :measurements="measurements"
              :differences="differences"
            ></measurements-display>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
    <v-row no-gutters class="mt-3">
      <v-col>
        <v-card>
          <v-card-title>
            <v-toolbar dark color="primary">
              Run information
            </v-toolbar>
          </v-card-title>
          <v-card-text class="py-0">
            <v-container fluid class="ma-0 pa-0">
              <v-row align="center" justify="space-around">
                <v-col
                  :lg="item.alwaysAuto ? 'auto' : '2'"
                  md="auto"
                  sm="auto"
                  class="pt-0"
                  v-for="item in runInfoItems"
                  :key="item.header"
                >
                  <v-card outlined>
                    <v-card-title class="pb-1">
                      <v-icon left dense>{{ item.icon }}</v-icon>
                      {{ item.header }}
                    </v-card-title>
                    <v-card-text>
                      <span :class="item.bodyClass">{{ item.body }}</span>
                    </v-card-text>
                  </v-card>
                </v-col>
              </v-row>
            </v-container>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import {
  Run,
  RunResultScriptError,
  RunResultVelcomError,
  RunResultSuccess,
  Measurement,
  DimensionDifference,
  RunWithDifferences
} from '@/store/types'
import { Prop } from 'vue-property-decorator'
import { formatDate, formatDuration } from '@/util/TimeUtil'
import MeasurementsDisplay from '@/components/rundetail/MeasurementsDisplay.vue'
import {
  mdiFlash,
  mdiCameraTimer,
  mdiAlarmCheck,
  mdiClockFast,
  mdiRobot
} from '@mdi/js'

@Component({
  components: {
    'measurements-display': MeasurementsDisplay
  }
})
export default class RunDetail extends Vue {
  @Prop()
  private runWithDifferences!: RunWithDifferences

  private formatDate(date: Date) {
    return formatDate(date)
  }

  private formatDuration(start: Date, end: Date) {
    return formatDuration(start, end)
  }

  private get run(): Run {
    return this.runWithDifferences.run
  }

  private get runInfoItems() {
    return [
      {
        header: 'Trigger',
        icon: this.iconTrigger,
        body: `${this.run.source.type.toLocaleLowerCase()} by ${
          this.run.author
        }`
      },
      {
        header: 'Started',
        icon: this.iconStarted,
        body: this.formatDate(this.run.startTime)
      },
      {
        header: 'Finished',
        icon: this.iconFinished,
        body: this.formatDate(this.run.stopTime)
      },
      {
        header: 'Duration',
        icon: this.iconDuration,
        body: this.formatDuration(this.run.startTime, this.run.stopTime)
      },
      {
        header: this.run.runnerName,
        icon: this.iconRunner,
        body: this.run.runnerInfo,
        bodyClass: 'worker-description',
        alwaysAuto: true
      }
    ]
  }

  private get runColor() {
    if (this.run.result instanceof RunResultScriptError) {
      return 'warning'
    } else if (this.run.result instanceof RunResultVelcomError) {
      return 'error'
    }
    return 'primary'
  }

  private get error(): string | undefined {
    if (this.errorType === undefined) {
      return undefined
    }
    return (this.run.result as RunResultVelcomError).error
  }

  private get errorType(): string | undefined {
    if (this.run.result instanceof RunResultScriptError) {
      return 'Benchmark-Script'
    }
    if (this.run.result instanceof RunResultVelcomError) {
      return 'VelCom'
    }
    return undefined
  }

  private get measurements(): Measurement[] | undefined {
    if (this.run.result instanceof RunResultSuccess) {
      return this.run.result.measurements
    }
    return undefined
  }

  private get differences(): DimensionDifference[] | undefined {
    if (this.run.result instanceof RunResultSuccess) {
      return this.runWithDifferences.differences
    }
    return undefined
  }

  // ICONS
  private iconTrigger = mdiFlash
  private iconStarted = mdiClockFast
  private iconFinished = mdiAlarmCheck
  private iconDuration = mdiCameraTimer
  private iconRunner = mdiRobot
}
</script>

<style scoped>
.worker-description,
.error-text {
  white-space: pre-wrap;
  font-family: monospace;
}
</style>