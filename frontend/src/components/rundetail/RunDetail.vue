<template>
  <v-container>
    <v-row>
      <v-col>
        <v-card>
          <v-card-title>
            <v-toolbar dark color="primary">
              Run information
            </v-toolbar>
          </v-card-title>
          <v-card-text>
            <v-container fluid class="ma-0 pa-0">
              <v-row align="center" justify="space-around">
                <v-col cols="2" class="pt-0">
                  <v-card>
                    <v-card-title>Trigger </v-card-title>
                    <v-card-text>
                      <span class="text-capitalize">{{
                        run.source.type.toLocaleLowerCase()
                      }}</span>
                      by {{ run.author }}
                    </v-card-text>
                  </v-card>
                </v-col>
                <v-col cols="2" class="pt-0">
                  <v-card>
                    <v-card-title>Started</v-card-title>
                    <v-card-text>
                      {{ formatDate(run.startTime) }}
                    </v-card-text>
                  </v-card>
                </v-col>
                <v-col cols="2" class="pt-0">
                  <v-card>
                    <v-card-title>Finished</v-card-title>
                    <v-card-text>
                      {{ formatDate(run.stopTime) }}
                    </v-card-text>
                  </v-card>
                </v-col>
                <v-col cols="2" class="pt-0">
                  <v-card>
                    <v-card-title>Duration</v-card-title>
                    <v-card-text>
                      {{ formatDuration(run.startTime, run.stopTime) }}
                    </v-card-text>
                  </v-card>
                </v-col>
                <v-col cols="2" class="pt-0">
                  <v-card>
                    <v-card-title>{{ run.runnerName }}</v-card-title>
                    <v-card-text>
                      <span class="worker-description">{{
                        run.runnerInfo
                      }}</span>
                    </v-card-text>
                  </v-card>
                </v-col>
              </v-row>
            </v-container>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
    <v-row>
      <v-col v-if="error !== undefined">
        <v-card>
          <v-card-title>
            <v-toolbar dark :color="runColor">{{ errorType }} Error</v-toolbar>
          </v-card-title>
          <v-card-text class="mx-2 error-text">{{ error }}</v-card-text>
        </v-card>
      </v-col>
    </v-row>
    <v-row>
      <v-col v-if="measurements !== undefined">
        <v-card>
          <v-card-title>
            <v-toolbar dark color="primary">Run Result</v-toolbar>
          </v-card-title>
          <v-card-text class="mx-2">
            <measurements-display
              :measurements="measurements"
            ></measurements-display>
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
  CommitTaskSource,
  CommitDescription,
  RunResultScriptError,
  RunResultVelcomError,
  RunResultSuccess,
  Measurement,
  MeasurementError,
  Dimension,
  MeasurementSuccess
} from '@/store/types'
import { Prop } from 'vue-property-decorator'
import { formatDate, formatDuration } from '@/util/TimeUtil'
import MeasurementsDisplay from '@/components/rundetail/MeasurementsDisplay.vue'

@Component({
  components: {
    'measurements-display': MeasurementsDisplay
  }
})
export default class RunDetail extends Vue {
  @Prop()
  private run!: Run

  private formatDate(date: Date) {
    return formatDate(date)
  }

  private formatDuration(start: Date, end: Date) {
    return formatDuration(start, end)
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

  created() {
    this.run = new Run(
      'my id',
      'I Al Istannen',
      'My runner',
      'Cool\n20 cores',
      new Date(new Date().getTime() - 20 * 60 * 1000),
      new Date(),
      new CommitTaskSource(
        new CommitDescription(
          'my repo id',
          'my commiut hash',
          'I Al Istannen',
          new Date(new Date().getTime() - 20 * 60 * 1000),
          'The commit summary!'
        )
      ),
      new RunResultSuccess([
        new MeasurementError(
          new Dimension('Benchmark', 'Metric', 'Unit', 'NEUTRAL'),
          'This is my error! It is really long\nand maginificent! This should overflooow' +
            '\noverflooow'.repeat(30)
        ),
        new MeasurementSuccess(
          new Dimension('Benchmark 2', 'Successful', 'cats', 'LESS_IS_BETTER'),
          21,
          [20, 21, 23, 24, 17]
        ),
        new MeasurementSuccess(
          new Dimension('Benchmark 2', 'Successful', 'cats', 'MORE_IS_BETTER'),
          21,
          [20, 21, 23, 24, 17]
        )
      ])
    )
  }
}
</script>

<style scoped>
.worker-description,
.error-text {
  white-space: pre-wrap;
  font-family: monospace;
}
</style>
