<template>
  <v-container fluid style="max-width: 1185px">
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

    <v-data-table
      dense
      multi-sort
      :headers="headers"
      :items="items"
      :items-per-page="-1"
      class="measurement-table"
      @click:row="rowClicked"
    >
      <template #[`item.value`]="{ item, value }">
        <measurement-value
          v-if="item.error === undefined"
          :value="value"
        ></measurement-value>
        <v-btn
          v-else
          class="error-message error-message-tooltip"
          text
          outlined
          @click.stop="
            showDetailErrorDialog = true
            detailErrorDialogMessage = item.error
          "
        >
          {{ formatErrorShorthand(item.error) }}
        </v-btn>
      </template>
      <template
        v-for="{ slotName, tooltip } in headerFormats"
        v-slot:[slotName]="{ item, value }"
      >
        <span :key="slotName">
          <measurement-value
            :value="value"
            :tooltip-message="
              typeof tooltip === 'string' ? tooltip : tooltip(item)
            "
            :color="item.changeColor"
          ></measurement-value>
        </span>
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
  DimensionInterpretation,
  DimensionDifference,
  Dimension
} from '@/store/types'
import MeasurementValueDisplay from '@/components/rundetail/MeasurementValueDisplay.vue'

const numberFormat: Intl.NumberFormat = new Intl.NumberFormat(
  new Intl.NumberFormat().resolvedOptions().locale,
  { maximumFractionDigits: 3 }
)

class Item {
  readonly benchmark: string
  readonly metric: string
  readonly unit: string
  readonly value?: string
  readonly standardDeviation?: string
  readonly standardDeviationPercent?: string
  readonly change?: string
  readonly changePercent?: string
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

  private formatNumber(number?: number): string | undefined {
    if (number === undefined) {
      return undefined
    }
    if (Math.abs(number) === 0) {
      return '0'
    }
    return numberFormat.format(number)
  }

  private formatPercent(number?: number): string | undefined {
    if (number === undefined) {
      return this.formatNumber(number)
    }
    return this.formatNumber(number * 100) + '%'
  }

  private computeStandardDeviationPercent(
    value?: number,
    standardDeviation?: number
  ): string | undefined {
    if (value === undefined || standardDeviation === undefined) {
      return this.formatPercent(undefined)
    }
    // divide by zero :(
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

@Component({
  components: {
    'measurement-value': MeasurementValueDisplay
  }
})
export default class MeasurementsDisplay extends Vue {
  @Prop()
  private measurements!: Measurement[]

  @Prop()
  private differences?: DimensionDifference[]

  private showDetailErrorDialog: boolean = false
  private detailErrorDialogMessage: string = ''

  private get headerFormats() {
    return [
      {
        slotName: 'item.change',
        tooltip: 'No unambiguous parent commit found'
      },
      {
        slotName: 'item.changePercent',
        tooltip: (item: Item) => {
          if (!item.change) {
            return 'No unambiguous parent commit found'
          }
          return "The old value was zero. I can't divide by it :/"
        }
      },
      {
        slotName: 'item.standardDeviation',
        tooltip:
          'Not applicable as the benchmark script did not report enough values'
      },
      {
        slotName: 'item.standardDeviationPercent',
        tooltip:
          'Not applicable as the benchmark script did not report enough values'
      }
    ]
  }

  private get headers() {
    return [
      { text: 'Benchmark', value: 'benchmark', align: 'left' },
      { text: 'Metric', value: 'metric', align: 'left' },
      { text: 'Unit', value: 'unit', align: 'left' },
      { text: 'Value', value: 'value', align: 'right' },
      {
        text: 'Stddev',
        value: 'standardDeviation',
        align: 'right'
      },
      {
        text: 'Stddev %',
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
    const difference = this.differenceForDimension(measurement.dimension)
    return new Item(
      measurement.dimension.benchmark,
      measurement.dimension.metric,
      measurement.dimension.unit,
      measurement.dimension.interpretation,
      measurement.value,
      difference ? difference.stddev : undefined,
      difference ? difference.absDiff : undefined,
      difference ? difference.relDiff : undefined
    )
  }

  private differenceForDimension(
    dimension: Dimension
  ): DimensionDifference | undefined {
    if (!this.differences) {
      return undefined
    }
    return this.differences.find(it => it.dimension.equals(dimension))
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

  private rowClicked(item: Item) {
    const currentSelection = document.getSelection()
    if (currentSelection && currentSelection.toString()) {
      return
    }
    const measurement = this.measurements.find(
      it =>
        it.dimension.benchmark === item.benchmark &&
        it.dimension.metric === item.metric
    )!

    this.$emit('dimension-clicked', measurement.dimension)
  }
}
</script>

<!--suppress CssUnresolvedCustomProperty -->
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

<!--suppress CssUnresolvedCustomProperty -->
<style>
.measurement-table tbody tr:hover {
  cursor: pointer;
}

/* LIGHT THEME alternating colors */
.theme--light .measurement-table tbody tr:nth-child(even) {
  background-color: var(--v-rowHighlight-lighten1);
}
.theme--light .measurement-table tbody tr:hover {
  background-color: var(--v-rowHighlight-darken1) !important;
}

/* DARK THEME alternating colors */
.theme--dark .measurement-table tbody tr:nth-child(even) {
  background-color: var(--v-rowHighlight-darken1);
}
.theme--dark .measurement-table tbody tr:hover {
  background-color: var(--v-rowHighlight-lighten1) !important;
}
</style>
