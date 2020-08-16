<template>
  <v-data-iterator
    class="full-width"
    :items="runs"
    :hide-default-footer="runs.length < defaultItemsPerPage"
    :items-per-page="defaultItemsPerPage"
    :footer-props="{ itemsPerPageOptions: itemsPerPageOptions }"
  >
    <template v-slot:default="props">
      <v-row>
        <v-col
          cols="12"
          class="my-1 py-0"
          v-for="(item, index) in props.items"
          :key="index"
        >
          <run-overview :run="run(item)">
            <template #content>
              <v-row no-gutters class="ma-0">
                <v-col
                  v-for="relevantChange in relevantChanges(item)"
                  :key="relevantChange.id.benchmark + relevantChange.id.metric"
                  cols="auto"
                  class="mr-2 mt-2"
                >
                  <v-chip
                    label
                    outlined
                    :color="relevantChange.color"
                    :input-value="true"
                  >
                    <v-icon left>{{ relevantChange.icon }}</v-icon>
                    {{ relevantChange.id.benchmark }} —
                    {{ relevantChange.id.metric }}
                    <span
                      class="font-weight-bold pl-3"
                      style="font-size: 1.1rem;"
                      >{{ relevantChange.change }}</span
                    >
                  </v-chip>
                </v-col>
              </v-row>
            </template>
          </run-overview>
        </v-col>
      </v-row>
    </template>
  </v-data-iterator>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import {
  Run,
  Commit,
  Measurement,
  Dimension,
  DimensionDifference,
  DimensionInterpretation,
  RunDescription,
  RunDescriptionWithDifferences
} from '../../store/types'
import RunOverview from './RunOverview.vue'
import {
  mdiChevronDown,
  mdiChevronDoubleDown,
  mdiChevronTripleDown,
  mdiChevronUp,
  mdiChevronDoubleUp,
  mdiChevronTripleUp
} from '@mdi/js'

const iconMappings = {
  up: {
    small: mdiChevronUp,
    middle: mdiChevronDoubleUp,
    large: mdiChevronTripleUp
  },
  down: {
    small: mdiChevronDown,
    middle: mdiChevronDoubleDown,
    large: mdiChevronTripleDown
  }
}

class RelevantChange {
  readonly id: Dimension
  readonly change: string
  readonly color: string
  readonly icon: string

  constructor(difference: DimensionDifference) {
    this.id = difference.dimension
    this.change =
      this.formatPercentage(difference.relDiff) + ` (${difference.stddev} σ)`
    this.color = this.changeColor(
      difference.absDiff,
      difference.dimension.interpretation
    )
    this.icon = this.changeIcon(difference.absDiff)
  }

  private formatPercentage(percentage: number): string {
    let scaled = Math.round(percentage * 100)
    return `${scaled}%`
  }

  private changeColor(change: number, interpretation: DimensionInterpretation) {
    if (Math.abs(change) === 0 || isNaN(change)) {
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
      return 'warning'
    }
    return 'success'
  }

  private changeIcon(change: number): string {
    const MIDDLE_CHANGE_THRESHOLD = 3
    const LARGE_CHANGE_THRESHOLD = 5
    const adjustedChange = Math.abs(Math.round(change * 100))

    let direction: 'up' | 'down' = change >= 0 ? 'up' : 'down'

    let magnitude: 'small' | 'middle' | 'large' = 'small'
    if (adjustedChange >= LARGE_CHANGE_THRESHOLD) {
      magnitude = 'large'
    } else if (adjustedChange >= MIDDLE_CHANGE_THRESHOLD) {
      magnitude = 'middle'
    }

    return iconMappings[direction][magnitude]
  }
}

@Component({
  components: {
    'run-overview': RunOverview
  }
})
export default class MultipleRunOverview extends Vue {
  @Prop()
  private runs!: RunDescription[] | RunDescriptionWithDifferences[]

  @Prop({ default: 3 })
  private numberOfChanges!: number

  private itemsPerPageOptions: number[] = [10, 20, 50, 100, 200, -1]
  private defaultItemsPerPage: number = 20

  private run(
    run: RunDescription | RunDescriptionWithDifferences
  ): RunDescription {
    return run instanceof RunDescriptionWithDifferences ? run.run : run
  }

  private relevantChanges(
    run: RunDescription | RunDescriptionWithDifferences
  ): RelevantChange[] {
    if (run instanceof RunDescription) {
      return []
    }
    console.log(run)

    return run.differences.map(it => new RelevantChange(it))
  }
}
</script>

<style scoped>
.full-width {
  width: 100%;
}
</style>
