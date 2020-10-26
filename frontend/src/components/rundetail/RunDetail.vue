<template>
  <v-container fluid class="pa-0 ma-0">
    <v-row no-gutters>
      <v-col v-if="error !== undefined">
        <v-card>
          <v-card-title>
            <v-toolbar dark :color="runColor">{{ errorType }} Error</v-toolbar>
          </v-card-title>
          <v-card-text class="mx-2 error-text" v-html="error"></v-card-text>
        </v-card>
      </v-col>
    </v-row>
    <v-row no-gutters v-if="measurements !== undefined">
      <v-col>
        <v-card>
          <v-card-title>
            <v-toolbar dark color="primary">Run Result</v-toolbar>
          </v-card-title>
          <v-card-text class="mx-2">
            <measurements-display
              :measurements="measurements"
              :differences="differences"
              @dimension-clicked="navigateToDetailGraph"
            ></measurements-display>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
    <v-row no-gutters class="mt-3">
      <v-col>
        <run-info :run="run"></run-info>
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
  RunWithDifferences,
  Dimension
} from '@/store/types'
import { Prop, Watch } from 'vue-property-decorator'
import MeasurementsDisplay from '@/components/rundetail/MeasurementsDisplay.vue'
import RunInfo from '@/components/rundetail/RunInfo.vue'
import { safeConvertAnsi } from '@/util/TextUtils'
import { vxm } from '@/store'

@Component({
  components: {
    'run-info': RunInfo,
    'measurements-display': MeasurementsDisplay
  }
})
export default class RunDetail extends Vue {
  @Prop()
  private runWithDifferences!: RunWithDifferences

  private get run(): Run {
    return this.runWithDifferences.run
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
    return safeConvertAnsi((this.run.result as RunResultVelcomError).error)
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

  private navigateToDetailGraph(dimension: Dimension) {
    this.$emit('navigate-to-detail-graph', dimension)
  }

  // noinspection JSUnusedLocalSymbols (Used by the watcher below)
  private get darkThemeSelected() {
    return vxm.userModule.darkThemeSelected
  }

  @Watch('darkThemeSelected')
  private onDarkThemeSelectionChanged() {
    // The ANSI conversion needs to be redone
    this.$forceUpdate()
  }
}
</script>

<style scoped>
.error-text {
  white-space: pre-wrap;
  font-family: monospace;
}
</style>
