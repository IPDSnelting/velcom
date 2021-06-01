<template>
  <v-container fluid>
    <v-row>
      <v-col cols="3">
        <repo-branch-selector></repo-branch-selector>
      </v-col>
      <v-col cols="9" style="height: 70vh">
        <status-comparison-graph
          :datapoints="data"
          :baseline-point="data[1]"
        ></status-comparison-graph>
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="3"></v-col>
      <v-col cols="9">
        <matrix-dimension-selection
          :repo-id="rip"
          :selected-dimensions="[]"
        ></matrix-dimension-selection>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import RepoBranchSelector from '@/components/graphs/comparison/RepoBranchSelector.vue'
import { vxm } from '@/store'
import MatrixDimensionSelection from '@/components/graphs/detail/MatrixDimensionSelection.vue'
import StatusComparisonGraph from '@/components/graphs/statuscomparison/StatusComparisonGraph.vue'

@Component({
  components: {
    StatusComparisonGraph,
    MatrixDimensionSelection,
    RepoBranchSelector
  }
})
export default class StatusComparison extends Vue {
  private data = []

  private get rip() {
    return vxm.repoModule.allRepos[0].id
  }
}
</script>

<style scoped></style>

<style></style>
