<template>
  <v-container>
    <v-data-table :headers="headers" :items="entries" item-key="key"/>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Run, Measurement } from '../store/types'
import { Prop } from 'vue-property-decorator'

@Component
export default class CommitInfoTable extends Vue {
  @Prop()
  private run!: Run

  @Prop()
  private previousRun!: Run

  private get headers() {
    return [
      { text: 'Benchmark', value: 'id.benchmark' },
      { text: 'Metric', value: 'id.metric' },
      { text: 'Value', value: 'value' },
      { text: 'Unit', value: 'unit' },
      { text: 'Change', value: 'change' }
    ]
  }

  private get entries() {
    if (this.run.measurements == null) {
      return null
    }

    return this.run.measurements.map(measurement => ({
      key: measurement.id.benchmark + '|' + measurement.id.metric,
      change: this.changeByItem(measurement),
      ...measurement
    }))
  }

  private changeByItem(item: Measurement) {
    if (this.previousRun == null || this.previousRun.measurements == null ||
      item.value == null) {
      return '-'
    }

    // find same measurement in previous run
    for (let prevMeasurement of this.previousRun.measurements) {
      if (!item.id.equals(prevMeasurement.id)) { continue }

      // found the measurement
      if (prevMeasurement.value == null) {
        return '-'
      } else {
        return item.value - prevMeasurement.value
      }
    }

    return '-'
  }
}
</script>

<style scoped>
</style>
