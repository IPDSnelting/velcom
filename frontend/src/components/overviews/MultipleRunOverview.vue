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
          v-for="( attributedRun, index) in props.items"
          :key="index"
        >
          <run-overview :commit="attributedRun.commit" :run="attributedRun.run">
            <template #content>
              <v-row no-gutters class="ma-0">
                <v-col
                  v-for="relevantChange in relevantChanges(attributedRun)"
                  :key="relevantChange.id.benchmark + relevantChange.id.metric"
                  cols="auto"
                  class="mr-2 mt-2"
                >
                  <v-chip label outlined :color="relevantChange.color" :input-value="true">
                    <v-icon left>{{ relevantChange.icon }}</v-icon>
                    {{ relevantChange.id.benchmark }} — {{ relevantChange.id.metric }}
                    <span
                      class="font-weight-bold pl-3"
                      style="font-size: 1.1rem"
                    >{{ formatPercentage(relevantChange.change) }}</span>
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
  CommitComparison,
  MeasurementID,
  Measurement
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

export class AttributedRun {
  readonly run: Run
  readonly commit: Commit
  readonly comparison: CommitComparison

  constructor(run: Run, commit: Commit, comparison: CommitComparison) {
    this.run = run
    this.commit = commit
    this.comparison = comparison
  }
}

let iconMappings = {
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
  id: MeasurementID
  change: number
  better: boolean

  constructor(id: MeasurementID, change: number, better: boolean) {
    this.id = id
    this.change = change
    this.better = better
  }

  get color() {
    return this.better ? 'success' : 'warning'
  }

  get icon() {
    const MIDDLE_CHANGE_THRESHOLD = 3
    const LARGE_CHANGE_THRESHOLD = 5
    const adjustedChange = Math.abs(Math.round(this.change * 100))

    let direction: 'up' | 'down' = this.change >= 0 ? 'up' : 'down'

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
  private runs!: AttributedRun[]

  @Prop({ default: 3 })
  private numberOfChanges!: number

  private itemsPerPageOptions: number[] = [10, 20, 50, 100, 200, -1]
  private defaultItemsPerPage: number = 20

  private relevantChanges(run: AttributedRun): RelevantChange[] {
    if (!run.run.measurements) {
      return []
    }
    let findMeasurement: (
      id: MeasurementID
    ) => Measurement | undefined = id => {
      return run.run.measurements!.find(it => it.id.equals(id))
    }
    let changes = run.comparison.differences
      .filter(difference => findMeasurement(difference.measurement))
      .map(difference => {
        let measurement = findMeasurement(difference.measurement)!
        let value = measurement.value!
        let valueDifference = difference.difference
        let differencePercentage = value === 0 ? 0 : valueDifference / value

        let isNowBetter =
          (measurement.interpretation === 'LESS_IS_BETTER' &&
            valueDifference < 0) ||
          (measurement.interpretation === 'MORE_IS_BETTER' &&
            valueDifference > 0)

        return new RelevantChange(
          measurement.id,
          differencePercentage,
          isNowBetter
        )
      })
      .filter(it => Math.round(it.change * 100) !== 0)
      .sort((a, b) => Math.abs(b.change) - Math.abs(a.change))

    changes = changes.slice(0, this.numberOfChanges)

    return changes
  }

  private formatPercentage(percentage: number) {
    let scaled = Math.round(percentage * 100)
    return `${scaled}%`
  }
}
</script>

<style scoped>
.full-width {
  width: 100%;
}
</style>
