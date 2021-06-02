<template>
  <v-container fluid>
    <v-row>
      <v-col
        cols="3"
        class="overflow-y-auto"
        style="height: calc(100vh - 70px)"
      >
        <repo-branch-selector></repo-branch-selector>
        <expandable-dimension-selection
          class="mt-3"
          :all-dimensions="allDimensions"
          :selected-dimensions.sync="selectedDimensions"
          :selector-type.sync="dimensionSelectorType"
        ></expandable-dimension-selection>
      </v-col>
      <v-col cols="9" style="height: calc(100vh - 70px)">
        <status-comparison-graph
          :datapoints="data"
          :baseline-point="data[1]"
          :selected-dimensions="selectedDimensions"
        ></status-comparison-graph>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import RepoBranchSelector from '@/components/graphs/comparison/RepoBranchSelector.vue'
import StatusComparisonGraph from '@/components/graphs/statuscomparison/StatusComparisonGraph.vue'
import { Dimension, StatusComparisonPoint } from '@/store/types'
import { vxm } from '@/store'
import ExpandableDimensionSelection from '@/components/graphs/helper/ExpandableDimensionSelection.vue'

@Component({
  components: {
    ExpandableDimensionSelection,
    StatusComparisonGraph,
    RepoBranchSelector
  }
})
export default class StatusComparison extends Vue {
  private data: StatusComparisonPoint[] = []

  private get allDimensions() {
    return vxm.repoModule.occuringDimensions(
      vxm.repoModule.allRepos.map(it => it.id)
    )
  }

  private get selectedDimensions() {
    return vxm.statusComparisonModule.selectedDimensions
  }

  private set selectedDimensions(dimensions: Dimension[]) {
    vxm.statusComparisonModule.selectedDimensions = dimensions
  }

  private get dimensionSelectorType() {
    return vxm.statusComparisonModule.selectedDimensionSelector
  }

  private set dimensionSelectorType(selector: 'tree' | 'matrix') {
    vxm.statusComparisonModule.selectedDimensionSelector = selector
  }

  private async mounted() {
    this.data = await vxm.statusComparisonModule.fetch()
  }
}
</script>

<style scoped></style>

<style></style>
