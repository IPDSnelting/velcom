<template>
  <v-data-table
    :headers="headers"
    :items="items"
    :items-per-page="-1"
    multi-sort
    dense
    class="compare-table"
  >
    <template #[`item.difference`]="{ item, value }">
      <span :style="{ color: item.changeColor }">{{
        item.formatNumber(value)
      }}</span>
    </template>

    <template
      v-for="slotName in ['item.valueFirst', 'item.valueSecond']"
      v-slot:[slotName]="{ item, value }"
    >
      <span :key="slotName">{{ item.formatNumber(value) }}</span>
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
  DimensionDifference,
  Dimension,
  RunResultSuccess,
  MeasurementSuccess,
  DimensionInterpretation
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
  readonly valueFirst?: number
  readonly difference?: number
  readonly valueSecond?: number
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
    this.valueFirst = valueFirst
    this.difference = difference
    this.valueSecond = valueSecond
    this.changeColor = this.computeChangeColor(interpretation, difference)
  }

  formatNumber(number?: number): string {
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
  private differences!: DimensionDifference[]

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
    const allDimensions = this.getDimensionsForRun(this.first).concat(
      this.getDimensionsForRun(this.second)
    )

    const uniqueDimensions: Dimension[] = []
    allDimensions.forEach(dim => {
      if (!uniqueDimensions.find(existing => existing.equals(dim))) {
        uniqueDimensions.push(dim)
      }
    })

    return uniqueDimensions.map(
      dimension =>
        new TableItem(
          dimension.benchmark,
          dimension.metric,
          dimension.unit,
          dimension.interpretation,
          this.getValue(this.first, dimension),
          this.getDifference(dimension),
          this.getValue(this.second, dimension)
        )
    )
  }

  private getDimensionsForRun(run: Run): Dimension[] {
    if (run.result instanceof RunResultSuccess) {
      return run.result.measurements.map(it => it.dimension)
    }
    return []
  }

  private getDifference(dimension: Dimension): number | undefined {
    const difference = this.differences.find(it =>
      it.dimension.equals(dimension)
    )
    if (difference) {
      return difference.absDiff
    }
    return undefined
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

<!--suppress CssUnresolvedCustomProperty -->
<style>
.compare-table tbody tr:hover {
  cursor: pointer;
}

/* LIGHT THEME alternating colors */
.theme--light .compare-table tbody tr:nth-child(even) {
  background-color: var(--v-rowHighlight-lighten1);
}
.theme--light .compare-table tbody tr:hover {
  background-color: var(--v-rowHighlight-darken1) !important;
}

/* DARK THEME alternating colors */
.theme--dark .compare-table tbody tr:nth-child(even) {
  background-color: var(--v-rowHighlight-darken1);
}
.theme--dark .compare-table tbody tr:hover {
  background-color: var(--v-rowHighlight-lighten1) !important;
}
</style>
