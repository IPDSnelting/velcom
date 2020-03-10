<template>
  <v-container fluid class="ma-0 pa-0 mb-4">
    <table>
      <thead>
        <tr>
          <th></th>
          <th class="metric" v-for="metric in allMetrics" :key="metric">
            <div @click="toggleAllForMetric(metric)">
              <span>{{ metric }}</span>
            </div>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="benchmark in allBenchmarks" :key="benchmark">
          <th class="benchmark-header" @click="selectAllForBenchmark(benchmark)">{{ benchmark }}</th>
          <td v-for="metric in allMetrics" :key="benchmark + metric">
            <v-checkbox
              :input-value="isSelected(benchmark, metric)"
              v-if="combinationExists(benchmark, metric)"
              :color="metricColor(benchmark, metric)"
              dense
              hide-details
              class="pa-0 ma-0"
              @change="changed($event, benchmark, metric)"
            ></v-checkbox>
          </td>
        </tr>
      </tbody>
    </table>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { vxm } from '../../store'
import { Prop, Watch, Model } from 'vue-property-decorator'
import { MeasurementID } from '../../store/types'

@Component
export default class MatrixMeasurementIdSelection extends Vue {
  @Prop()
  private repoId!: string

  @Prop()
  private selectedMeasurements!: MeasurementID[]

  private get selectedMeasurementsSet(): Set<string> {
    return new Set(
      this.selectedMeasurements.map(it => it.benchmark + it.metric)
    )
  }

  private isSelected(benchmark: string, metric: string): boolean {
    return this.selectedMeasurementsSet.has(benchmark + metric)
  }

  private combinationExists(benchmark: string, metric: string) {
    return this.metricsFor(benchmark).includes(metric)
  }

  private get allBenchmarks(): string[] {
    return vxm.repoModule.occuringBenchmarks([this.repoId])
  }

  private metricsFor(benchmark: string): string[] {
    return vxm.repoModule.metricsForBenchmark(benchmark)
  }

  private toggleAllForMetric(metric: string) {
    let relevantBenchmarks = this.allBenchmarks.filter(it =>
      this.metricsFor(it).includes(metric)
    )

    let remainingMeasurements = []

    let allAreSelected = relevantBenchmarks.every(it =>
      this.selectedMeasurementsSet.has(it + metric)
    )

    if (allAreSelected) {
      // Keep all with other metrics
      remainingMeasurements = this.selectedMeasurements.filter(
        it => it.metric !== metric
      )
    } else {
      let benchmarksSet = new Set(relevantBenchmarks)

      // figure out already selected ones
      this.selectedMeasurements
        .filter(it => it.metric === metric)
        .forEach(it => benchmarksSet.delete(it.benchmark))

      // Concat new ones to it
      remainingMeasurements = Array.from(benchmarksSet)
        .map(it => new MeasurementID(it, metric))
        .concat(this.selectedMeasurements)
    }

    this.$emit('input', remainingMeasurements)
  }

  private selectAllForBenchmark(benchmark: string) {
    let relevantMetrics = this.metricsFor(benchmark)

    let remainingMeasurements = []

    let allSelected = relevantMetrics.every(metric =>
      this.selectedMeasurementsSet.has(benchmark + metric)
    )
    if (allSelected) {
      remainingMeasurements = this.selectedMeasurements.filter(
        it => it.benchmark !== benchmark
      )
    } else {
      let metricSet = new Set(relevantMetrics)

      // figure out already selected ones
      this.selectedMeasurements
        .filter(it => it.benchmark === benchmark)
        .forEach(it => metricSet.delete(it.metric))

      // Concat new ones to it
      remainingMeasurements = Array.from(metricSet)
        .map(metric => new MeasurementID(benchmark, metric))
        .concat(this.selectedMeasurements)
    }
    this.$emit('input', remainingMeasurements)
  }

  private get allMetrics(): string[] {
    return Array.from(
      new Set(this.allBenchmarks.flatMap(it => this.metricsFor(it))).values()
    ).sort()
  }

  private metricColor(benchmark: string, metric: string): string {
    if (this.selectedMeasurementsSet.has(benchmark + metric)) {
      return vxm.colorModule.colorByIndex(
        this.selectedMeasurements.findIndex(
          it => it.benchmark === benchmark && it.metric === metric
        )
      )
    }
    return 'accent'
  }

  private changed(checked: boolean, benchmark: string, metric: string) {
    if (checked) {
      this.$emit(
        'input',
        this.selectedMeasurements.concat([new MeasurementID(benchmark, metric)])
      )
    } else {
      this.$emit(
        'input',
        this.selectedMeasurements.filter(
          it => it.benchmark !== benchmark || it.metric !== metric
        )
      )
    }
  }
}
</script>

<style scoped>
/* https://css-tricks.com/rotated-table-column-headers/ */
th.metric {
  /* Something you can count on */
  height: 140px;
  white-space: nowrap;
}

th.metric > div {
  transform:
    /* Magic Numbers */ translate(0px, 51px)
    /* 45 is really 360 - 45 */ rotate(315deg);
  width: 30px;
}
th.metric > div > span {
  border-bottom: 1px solid #ccc;
  padding: 5px 10px;
}

.benchmark-header {
  text-align: end;
}

.benchmark-header,
th.metric {
  cursor: pointer;
}

/* HOVER ON ROW / COLUMN */

tbody tr:hover,
.hovered {
  background-color: #d3d3d3;
}
</style>
