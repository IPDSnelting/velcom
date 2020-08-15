<template>
  <v-container fluid>
    <v-dialog v-model="showDetailErrorDialog">
      <v-card>
        <v-toolbar dark color="primary">
          <v-toolbar-title>Full error message</v-toolbar-title>
        </v-toolbar>
        <v-card-text>
          <div class="ma-4 error-message">{{ detailErrorDialogMessage }}</div>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
            color="error"
            text
            outlined
            @click="showDetailErrorDialog = false"
            >Close</v-btn
          >
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-data-table multi-sort :headers="headers" :items="items" :items-per-page="5">
      <template #[`item.value`]="{ item, value }">
        <span v-if="item.error === undefined">{{ value }}</span>
        <v-btn
          v-else
          class="error-message error-message-tooltip"
          text
          outlined
          @click="
            showDetailErrorDialog = true
            detailErrorDialogMessage = item.error
          "
          >{{ formatErrorShorthand(item.error) }}</v-btn
        >
      </template>
      <template #[`item.change`]=" { item, value }">
        <span :style="{ color: item.changeColor }">{{ value }}</span>
      </template>
      <template #[`item.changePercent`]=" { item, value }">
        <span :style="{ color: item.changeColor }">{{ value }}</span>
      </template>
    </v-data-table>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import {
  Measurement,
  MeasurementError,
  MeasurementSuccess,
  DimensionInterpretation
} from '@/store/types'

const numberFormat: Intl.NumberFormat = new Intl.NumberFormat(
  new Intl.NumberFormat().resolvedOptions().locale,
  { maximumFractionDigits: 3 }
)

class Item {
  readonly benchmark: string
  readonly metric: string
  readonly unit: string
  readonly value: string
  readonly standardDeviation: string
  readonly standardDeviationPercent: string
  readonly change: string
  readonly changePercent: string
  readonly changeColor: string
  readonly error?: string

  constructor(
    benchmark: string,
    metric: string,
    unit: string,
    interpretation: DimensionInterpretation,
    value?: number,
    standardDeviation?: number,
    change?: number,
    changePercent?: number,
    error?: string
  ) {
    this.benchmark = benchmark
    this.metric = metric
    this.unit = unit
    this.value = this.formatNumber(value)
    this.standardDeviation = this.formatNumber(standardDeviation)
    this.standardDeviationPercent = this.computeStandardDeviationPercent(
      value,
      standardDeviation
    )
    this.change = this.formatNumber(change)
    this.changePercent = this.formatPercent(changePercent)
    this.error = error
    this.changeColor = this.computeChangeColor(interpretation, change)
  }

  private formatNumber(number?: number): string {
    if (number === undefined) {
      return '-'
    }
    if (Math.abs(number) === 0) {
      return '0'
    }
    return numberFormat.format(number)
  }

  private formatPercent(number?: number): string {
    if (number === undefined) {
      return this.formatNumber(number)
    }
    return this.formatNumber(number * 100) + '%'
  }

  private computeStandardDeviationPercent(
    value?: number,
    standardDeviation?: number
  ): string {
    if (value === undefined || standardDeviation === undefined) {
      return this.formatPercent(undefined)
    }
    // devide by zero :(
    if (value === 0) {
      return this.formatPercent(undefined)
    }
    return this.formatPercent(standardDeviation / value)
  }

  private computeChangeColor(
    interpretation: DimensionInterpretation,
    change?: number
  ): string {
    if (change === undefined || Math.abs(change) === 0 || isNaN(change)) {
      return ''
    }

    if (interpretation === 'NEUTRAL') {
      return ''
    }

    let bad = false
    if (interpretation === 'LESS_IS_BETTER') {
      bad = change > 0
    } else if (interpretation === 'MORE_IS_BETTER') {
      bad = change < 0
    }

    if (bad) {
      return 'var(--v-warning-base)'
    }
    return 'var(--v-success-base)'
  }
}

@Component
export default class MeasurementsDisplay extends Vue {
  @Prop()
  private measurements!: Measurement[]

  private showDetailErrorDialog: boolean = false
  private detailErrorDialogMessage: string = ''

  private get headers() {
    return [
      { text: 'Benchmark', value: 'benchmark', align: 'left' },
      { text: 'Metric', value: 'metric', align: 'left' },
      { text: 'Unit', value: 'unit', align: 'left' },
      { text: 'Value', value: 'value', align: 'right' },
      {
        text: 'Standard Deviation',
        value: 'standardDeviation',
        align: 'right'
      },
      {
        text: 'Standard Deviation %',
        value: 'standardDeviationPercent',
        align: 'right'
      },
      { text: 'Change', value: 'change', align: 'right' },
      { text: 'Change %', value: 'changePercent', align: 'right' }
    ]
  }

  private get items(): Item[] {
    return this.measurements.map(it =>
      it instanceof MeasurementSuccess
        ? this.successToItem(it)
        : this.errorToItem(it)
    )
  }

  private successToItem(measurement: MeasurementSuccess): Item {
    return new Item(
      measurement.dimension.benchmark,
      measurement.dimension.metric,
      measurement.dimension.unit,
      measurement.dimension.interpretation,
      measurement.value,
      this.standardDeviation(measurement.values),
      // FIXME: Real change
      20,
      0.5
    )
  }

  private standardDeviation(numbers: number[]): number {
    let n = numbers.length
    if (n <= 1) {
      return 0
    }

    const mean =
      numbers.reduce((acc: number, next: number) => acc + next) / numbers.length
    const differenceSum = numbers
      .map(it => Math.pow(it - mean, 2))
      .reduce((a, b) => a + b)
    const stdDev = (1 / (n - 1)) * differenceSum

    return Math.sqrt(stdDev)
  }

  private errorToItem(measurementError: MeasurementError): Item {
    return new Item(
      measurementError.dimension.benchmark,
      measurementError.dimension.metric,
      measurementError.dimension.unit,
      measurementError.dimension.interpretation,
      undefined,
      undefined,
      undefined,
      undefined,
      measurementError.error
    )
  }

  private formatErrorShorthand(error: string) {
    const MAX_ERROR_LENGTH = 30
    if (error.length < MAX_ERROR_LENGTH) {
      return error
    }
    return error.substring(0, MAX_ERROR_LENGTH) + '…'
  }
}
</script>

<style scoped>
.error-message {
  color: var(--v-error-base);
  font-family: monospace;
  white-space: pre-line;
  overflow: hidden;
}

.error-message-tooltip {
  cursor: pointer;
}
</style>
