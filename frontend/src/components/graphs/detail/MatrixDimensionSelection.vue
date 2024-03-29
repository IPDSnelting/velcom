<template>
  <v-container fluid class="ma-0 pa-0 mb-4 container">
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
          <th
            class="benchmark-header"
            @click="toggleAllForBenchmark(benchmark)"
          >
            {{ benchmark }}
          </th>
          <td
            v-for="[metric, exists] in benchmarkRow(benchmark)"
            :key="benchmark + metric"
          >
            <v-checkbox
              v-if="exists"
              :input-value="isSelected(benchmark, metric)"
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
import { vxm } from '@/store'
import { Prop } from 'vue-property-decorator'
import { Dimension } from '@/store/types'
import { distinct, locallySorted } from '@/util/Arrays'

@Component
export default class MatrixMeasurementIdSelection extends Vue {
  @Prop()
  private allDimensions!: Dimension[]

  @Prop()
  private selectedDimensions!: Dimension[]

  private updateSelectedDimensions(newSelected: Dimension[]) {
    this.$emit('update:selectedDimensions', newSelected)
  }

  private get selectedDimensionSet(): Set<string> {
    return new Set(this.selectedDimensions.map(it => it.toString()))
  }

  private isSelected(benchmark: string, metric: string): boolean {
    return this.selectedDimensionSet.has(benchmark + ' - ' + metric)
  }

  private get benchmarkToMetrics(): Map<string, Set<string>> {
    const map: Map<string, Set<string>> = new Map()
    for (const dim of this.allDimensions) {
      if (!map.has(dim.benchmark)) {
        map.set(dim.benchmark, new Set())
      }
      map.get(dim.benchmark)!.add(dim.metric)
    }
    return map
  }

  private benchmarkRow(benchmark: string): [string, boolean][] {
    const metrics = this.benchmarkToMetrics.get(benchmark)!

    return this.allMetrics.map(metric => [metric, metrics.has(metric)])
  }

  private get allBenchmarks(): string[] {
    const benchmarks = this.allDimensions.map(it => it.benchmark)
    return locallySorted(distinct(benchmarks))
  }

  private get allMetrics(): string[] {
    const allMetrics: Set<string> = new Set()
    for (const metrics of this.benchmarkToMetrics.values()) {
      for (const metric of metrics.values()) {
        allMetrics.add(metric)
      }
    }
    return locallySorted(Array.from(allMetrics.values()))
  }

  private metricsFor(benchmark: string): Set<string> {
    return this.benchmarkToMetrics.get(benchmark)!
  }

  private toggleAllForMetric(metric: string) {
    const relevantBenchmarks: string[] = this.allBenchmarks.filter(benchmark =>
      this.metricsFor(benchmark).has(metric)
    )

    let resultingSelectedDimensions: Dimension[]

    const allAreSelected = relevantBenchmarks.every(benchmark =>
      this.selectedDimensionSet.has(benchmark + ' - ' + metric)
    )

    if (allAreSelected) {
      // deselect dimensions with given metric, but keep all with other metrics
      resultingSelectedDimensions = this.selectedDimensions.filter(
        dimension => dimension.metric !== metric
      )
    } else {
      // select all dimensions with given metric

      // figure out which dimensions need to bee added
      const notYetSelectedDimensions: Dimension[] = this.allDimensions
        .filter(it => it.metric === metric)
        .filter(dimension => !this.selectedDimensions.includes(dimension))

      // add them to the already selected ones
      resultingSelectedDimensions = this.selectedDimensions.concat(
        notYetSelectedDimensions
      )
    }

    this.updateSelectedDimensions(resultingSelectedDimensions)
  }

  private toggleAllForBenchmark(benchmark: string) {
    const relevantMetrics = Array.from(this.metricsFor(benchmark))

    let resultingSelectedDimensions: Dimension[]

    const allAreSelected = relevantMetrics.every(metric =>
      this.selectedDimensionSet.has(benchmark + ' - ' + metric)
    )
    if (allAreSelected) {
      // deselect dimensions with given benchmark, but keep all with other benchmarks

      resultingSelectedDimensions = this.selectedDimensions.filter(
        it => it.benchmark !== benchmark
      )
    } else {
      // select all dimensions with given benchmark

      // figure out which dimensions need to bee added
      const notYetSelectedDimensions: Dimension[] = this.allDimensions
        .filter(it => it.benchmark === benchmark)
        .filter(dimension => !this.selectedDimensions.includes(dimension))

      // add them to the already selected ones
      resultingSelectedDimensions = this.selectedDimensions.concat(
        notYetSelectedDimensions
      )
    }
    this.updateSelectedDimensions(resultingSelectedDimensions)
  }

  private metricColor(benchmark: string, metric: string): string {
    if (this.selectedDimensionSet.has(benchmark + ' - ' + metric)) {
      return vxm.colorModule.colorForDetailDimension({
        benchmark,
        metric
      })
    }
    return 'accent'
  }

  private changed(checked: boolean, benchmark: string, metric: string) {
    if (checked) {
      const newlyCheckedDimension: Dimension | undefined =
        this.allDimensions.find(
          it => it.benchmark === benchmark && it.metric === metric
        )

      if (newlyCheckedDimension) {
        this.updateSelectedDimensions(
          this.selectedDimensions.concat(newlyCheckedDimension)
        )
      }
    } else {
      this.updateSelectedDimensions(
        this.selectedDimensions.filter(
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

.container {
  overflow-x: auto;
  overflow-y: hidden;
}

.benchmark-header,
th.metric {
  cursor: pointer;
}

/* HOVER ON ROW / COLUMN */

table {
  border-collapse: collapse;
  border-spacing: 0;
}

/*noinspection CssUnresolvedCustomProperty*/
tbody tr:hover,
.hovered {
  background: var(--v-rowHighlight-base);
}
</style>
