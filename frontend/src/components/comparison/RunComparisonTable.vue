<template>
  <v-data-table :headers="headers" :items="items" multi-sort>
    <template #[`item.difference`]=" { item, value }">
      <span :style="{ color: item.changeColor }">{{ value }}</span>
    </template>

    <template #[`header.difference`]="{}">
      <span class="change-arrow">→</span>
    </template>
  </v-data-table>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import {
  Run,
  RunCompareDifference,
  Dimension,
  RunResultSuccess,
  MeasurementSuccess,
  DimensionInterpretation,
  CommitTaskSource,
  CommitDescription,
  TarTaskSource
} from '@/store/types'
import { Prop } from 'vue-property-decorator'

const numberFormat: Intl.NumberFormat = new Intl.NumberFormat(
  new Intl.NumberFormat().resolvedOptions().locale,
  { maximumFractionDigits: 3 }
)

class TableItem {
  readonly benchmark: string
  readonly metric: string
  readonly unit: string
  readonly valueFirst: string
  readonly difference: string
  readonly valueSecond: string
  readonly changeColor: string

  constructor(
    benchmark: string,
    metric: string,
    unit: string,
    interpretation: DimensionInterpretation,
    valueFirst?: number,
    difference?: number,
    valueSecond?: number
  ) {
    this.benchmark = benchmark
    this.metric = metric
    this.unit = unit
    this.valueFirst = this.formatNumber(valueFirst)
    this.difference = this.formatNumber(difference)
    this.valueSecond = this.formatNumber(valueSecond)
    this.changeColor = this.computeChangeColor(interpretation, difference)
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
export default class RunComparisonTable extends Vue {
  @Prop()
  private first!: Run

  @Prop()
  private second!: Run

  @Prop({ default: () => [] })
  private differences!: RunCompareDifference[]

  private get headers() {
    return [
      { text: 'Benchmark', value: 'benchmark', align: 'left' },
      { text: 'Metric', value: 'metric', align: 'left' },
      { text: 'Unit', value: 'unit', align: 'left' },
      { text: this.first.id, value: 'valueFirst', align: 'right' },
      {
        text: '->',
        value: 'difference',
        sortable: false,
        filterable: false,
        align: 'center'
      },
      { text: this.second.id, value: 'valueSecond', align: 'left' }
    ]
  }

  private get items(): TableItem[] {
    return this.differences.map(
      it =>
        new TableItem(
          it.dimension.benchmark,
          it.dimension.metric,
          it.dimension.unit,
          it.dimension.interpretation,
          this.getValue(this.first, it.dimension),
          it.difference,
          this.getValue(this.second, it.dimension)
        )
    )
  }

  private getValue(run: Run, dimension: Dimension): number | undefined {
    if (run.result instanceof RunResultSuccess) {
      const relevantMeasurement = run.result.measurements.find(it =>
        it.dimension.equals(dimension)
      )
      if (!relevantMeasurement) {
        return undefined
      }
      if (relevantMeasurement instanceof MeasurementSuccess) {
        return relevantMeasurement.value
      }
    }
    return undefined
  }
}
</script>

<style scoped>
.change-arrow {
  font-size: 1.75em;
}
</style>
