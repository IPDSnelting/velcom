<template>
  <v-container fluid>
    <v-data-table :headers="headers" :items="entries" item-key="key">
      <template #item.value="{ item, value }">
        <span v-if="value">{{ value }}</span>
        <span v-else class="error-message">{{ item.errorMessage }}</span>
      </template>
      <template #item.unit=" { value }">
        <span v-if="value">{{ value }}</span>
        <span v-else>-</span>
      </template>
    </v-data-table>
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
      throw new Error('I was given a run with no measurements!')
    }

    return this.run.measurements.map(measurement => ({
      key: measurement.id.benchmark + '|' + measurement.id.metric,
      change: this.changeByItem(measurement),
      ...measurement
    }))
  }

  private changeByItem(item: Measurement) {
    if (
      this.previousRun == null ||
      this.previousRun.measurements == null ||
      item.value == null
    ) {
      return '-'
    }

    let previousMeasurement = this.previousRun.measurements.find(measurement =>
      item.id.equals(measurement.id)
    )

    if (previousMeasurement && previousMeasurement.value) {
      return item.value - previousMeasurement.value
    }

    return '-'
  }
}
</script>

<style scoped>
.error-message {
  color: var(--v-error-base);
}
</style>
