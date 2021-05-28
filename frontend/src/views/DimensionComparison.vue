<template>
  <v-container style="height: 70vh">
    <dimension-comparison-graph
      :datapoints="data"
      :baseline-point="data[1]"
    ></dimension-comparison-graph>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { DimensionComparisonPoint } from '@/store/types'
import DimensionComparisonGraph from '@/components/graphs/benchmark_comparison/DimensionComparisonGraph.vue'

@Component({
  components: {
    DimensionComparisonGraph
  }
})
export default class DimensionComparison extends Vue {
  private data = [
    new DimensionComparisonPoint(
      'VelCom',
      new Map([
        ['size', 20],
        ['speed', 30],
        ['foo', 1],
        ['bar', 25]
      ])
    ),
    new DimensionComparisonPoint(
      'PFERD',
      new Map([
        ['size', 10],
        ['speed', 40],
        ['foo', 5],
        ['bar', 50]
      ])
    )
  ]
}
</script>

<style scoped></style>

<style></style>
