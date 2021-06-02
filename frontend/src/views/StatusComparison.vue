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
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import RepoBranchSelector from '@/components/graphs/comparison/RepoBranchSelector.vue'
import StatusComparisonGraph from '@/components/graphs/statuscomparison/StatusComparisonGraph.vue'
import { StatusComparisonPoint } from '@/store/types'
import { vxm } from '@/store'

@Component({
  components: {
    StatusComparisonGraph,
    RepoBranchSelector
  }
})
export default class StatusComparison extends Vue {
  private data: StatusComparisonPoint[] = []

  private async mounted() {
    this.data = await vxm.statusComparisonModule.fetch()
  }
}
</script>

<style scoped></style>

<style></style>
