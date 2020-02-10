<template>
  <v-container fluid>
    <v-dialog max-width="600" v-model="showDetailErrorMessageDialog">
      <v-card>
        <v-toolbar dark color="primary">
          <v-toolbar-title>Full error message</v-toolbar-title>
        </v-toolbar>
        <v-card-text>
          <div class="ma-4 error-message">{{ detailMeasurementErrorMessage }}</div>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="error" text outlined @click="showDetailErrorMessageDialog = false">Close</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-data-table :headers="headers" :items="entries" item-key="key" multi-sort>
      <template #header.compareChange="{}">
        <span class="change-arrow">→</span>
      </template>
      <template #item.value="{ item, value }">
        <span v-if="value">{{ formatNumber(value) }}</span>
        <v-btn
          v-else
          class="error-message error-message-tooltip"
          text
          outlined
          @click="showDetailErrorMessageDialog = true; detailMeasurementErrorMessage = item.errorMessage"
        >{{ formatError(item.errorMessage) }}</v-btn>
      </template>
      <template #item.unit=" { value }">
        <span v-if="value">{{ value }}</span>
        <span v-else>-</span>
      </template>
      <template #item.change=" { item, value }">
        <span
          :style="{ color: changeColor(item, value) }"
        >{{ value === '-' ? '-': formatNumber(value) }}</span>
      </template>
      <template #item.compareChange=" { item, value }">
        <span :style="{ color: changeColor(item, value) }">{{ formatNumber(value) }}</span>
      </template>
    </v-data-table>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import {
  Run,
  Measurement,
  CommitComparison,
  MeasurementID
} from '../store/types'
import { Prop } from 'vue-property-decorator'

@Component
export default class CommitInfoTable extends Vue {
  @Prop()
  private comparison!: CommitComparison

  @Prop()
  private compare!: boolean

  private showDetailErrorMessageDialog: boolean = false
  private detailMeasurementErrorMessage: string = ''

  private numberFormat: Intl.NumberFormat = new Intl.NumberFormat(
    this.getLocaleString(),
    { maximumFractionDigits: 4 }
  )

  private getLocaleString() {
    return new Intl.NumberFormat().resolvedOptions().locale
  }

  private get headers() {
    if (this.compare) {
      if (this.comparison.firstCommit == null) {
        throw new Error('compare is true but firstCommit is null?!')
      }

      return [
        { text: 'Benchmark', value: 'benchmark', align: 'left' },
        { text: 'Metric', value: 'metric', align: 'left' },
        { text: 'Unit', value: 'unit', align: 'left' },
        {
          text: this.comparison.firstCommit.hash,
          value: 'firstVal',
          align: 'right'
        },
        {
          text: '->',
          value: 'compareChange',
          sortable: false,
          filterable: false,
          align: 'center'
        },
        {
          text: this.comparison.secondCommit.hash,
          value: 'secondVal',
          align: 'left'
        }
      ]
    } else {
      return [
        { text: 'Benchmark', value: 'id.benchmark', align: 'left' },
        { text: 'Metric', value: 'id.metric', align: 'left' },
        { text: 'Unit', value: 'unit', align: 'left' },
        { text: 'Value', value: 'value', align: 'right' },
        { text: 'Change', value: 'change', align: 'right' }
      ]
    }
  }

  private get entries() {
    if (this.compare) {
      if (this.comparison.firstCommit == null) {
        throw new Error('compare is true but firstCommit is null?!')
      }

      return this.comparison.differences.map(diff => ({
        key: diff.measurement.benchmark + '|' + diff.measurement.metric,
        benchmark: diff.measurement.benchmark,
        metric: diff.measurement.metric,
        unit: this.findUnit(diff.measurement),
        firstVal: this.findFirstVal(diff.measurement),
        secondVal: this.findSecondVal(diff.measurement),
        compareChange: this.findChange(diff.measurement),
        interpretation: this.findMeasurement(diff.measurement).interpretation
      }))
    } else {
      if (this.comparison.second == null) {
        throw new Error('I was given a run with no measurements!')
      }
      if (this.comparison.second.measurements == null) {
        throw new Error('I was given a run with no measurements!')
      }

      return this.comparison.second.measurements.map(measurement => ({
        key: measurement.id.benchmark + '|' + measurement.id.metric,
        change: this.findChange(measurement.id),
        ...measurement
      }))
    }
  }

  private findFirstVal(measId: MeasurementID) {
    if (this.comparison.first == null) {
      // What kind of comparison is this?
      return '-'
    } else if (this.comparison.first.measurements == null) {
      return this.comparison.first.errorMessage
    }

    let measurement = this.comparison.first.measurements.find(m =>
      m.id.equals(measId)
    )

    if (measurement) {
      if (measurement.value != null) {
        return this.formatNumber(measurement.value)
      } else {
        return measurement.errorMessage
      }
    } else {
      throw new Error('I failed to find the measurement: ' + measId)
    }
  }

  private findSecondVal(measId: MeasurementID) {
    if (this.comparison.second == null) {
      return '-'
    } else if (this.comparison.second.measurements == null) {
      return this.comparison.second.errorMessage
    }

    let measurement = this.comparison.second.measurements.find(m =>
      m.id.equals(measId)
    )

    if (measurement) {
      if (measurement.value != null) {
        return this.formatNumber(measurement.value)
      } else {
        return measurement.errorMessage
      }
    } else {
      throw new Error('I failed to find the measurement: ' + measId)
    }
  }

  private findUnit(measId: MeasurementID): string | null {
    return this.findMeasurement(measId).unit
  }

  private findMeasurement(measId: MeasurementID): Measurement {
    let measurements: Measurement[] | null

    if (this.comparison.second != null && this.comparison.second.measurements) {
      measurements = this.comparison.second.measurements
    } else if (
      this.comparison.first != null &&
      this.comparison.first.measurements
    ) {
      measurements = this.comparison.first.measurements
    } else {
      throw new Error('I was given two runs that were both null?!')
    }

    // The measurement must exist since this wall called with a
    // measID that came from a commit difference which means that
    // a difference exists which means that the measurement must exist
    // in either first or second run
    if (measurements == null) {
      throw new Error('I was given two runs with no measurements')
    }

    let measurement = measurements.find(m => m.id.equals(measId))
    if (measurement) {
      return measurement
    } else {
      throw new Error('I failed to find the right measurement: ' + measId)
    }
  }

  private findChange(measId: MeasurementID): number | '-' {
    // 1.) Find measurement in first run
    if (
      this.comparison.first == null ||
      this.comparison.first.measurements == null
    ) {
      return '-'
    }

    let firstMeas = this.comparison.first.measurements.find(measurement =>
      measId.equals(measurement.id)
    )

    if (firstMeas === undefined || firstMeas.value == null) {
      return '-'
    }

    // 2.) Find measurement in second run
    if (
      this.comparison.second == null ||
      this.comparison.second.measurements == null
    ) {
      return '-'
    }

    let secondMeas = this.comparison.second.measurements.find(measurement =>
      measId.equals(measurement.id)
    )

    if (secondMeas === undefined || secondMeas.value == null) {
      return '-'
    }

    // 3.) Return difference
    return secondMeas.value - firstMeas.value
  }

  private formatNumber(number: number): string {
    if (Math.abs(number) === 0) {
      return '0'
    }
    return this.numberFormat.format(number)
  }

  private formatError(error: string) {
    const MAX_ERROR_LENGTH = 30
    if (error.length < MAX_ERROR_LENGTH) {
      return error
    }
    return error.substring(0, MAX_ERROR_LENGTH) + '…'
  }

  private changeColor(item: Measurement, change: number): string {
    if (Math.abs(change) === 0 || isNaN(change)) {
      return ''
    }

    if (change < 0 && item.interpretation === 'LESS_IS_BETTER') {
      return 'var(--v-success-base)'
    }
    return 'var(--v-warning-base)'
  }
}
</script>

<style scoped>
.error-message {
  color: var(--v-error-base);
  font-family: monospace;
  white-space: pre-line;
}

.error-message-tooltip {
  cursor: pointer;
}

.change-arrow {
  font-size: 1.75em;
}
</style>
