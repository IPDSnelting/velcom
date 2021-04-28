<template>
  <v-card>
    <v-card-text>
      <v-btn
        @click="useMatrixSelector = !useMatrixSelector"
        color="primary"
        outlined
        text
        style="display: block; margin: auto"
      >
        <span v-if="useMatrixSelector">Use tree selector</span>
        <span v-if="!useMatrixSelector">Use matrix selector</span>
      </v-btn>
      <matrix-dimension-selection
        v-if="useMatrixSelector"
        :selectedDimensions="selectedDimensions"
        :repoId="repoId"
      ></matrix-dimension-selection>
      <tree-dimension-selection
        v-if="!useMatrixSelector"
        :selectedDimensions="selectedDimensions"
        :repoId="repoId"
      >
      </tree-dimension-selection>
    </v-card-text>
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import MatrixDimensionSelection from '@/components/graphs/MatrixDimensionSelection.vue'
import DimensionSelection from '@/components/graphs/DimensionSelection.vue'
import { Dimension } from '@/store/types'
import { vxm } from '@/store'
import { Watch } from 'vue-property-decorator'

@Component({
  components: {
    'matrix-dimension-selection': MatrixDimensionSelection,
    'tree-dimension-selection': DimensionSelection
  }
})
export default class DetailGraphDimensionSelector extends Vue {
  @Watch('selectedDimensions')
  @Watch('repoId')
  private reloadGraphData() {
    this.$emit('reload-graph-data')
  }

  private get repoId() {
    return vxm.detailGraphModule.selectedRepoId
  }

  private get useMatrixSelector() {
    return vxm.detailGraphModule.selectedDimensionSelector === 'matrix'
  }

  private set useMatrixSelector(useMatrixSelector: boolean) {
    if (useMatrixSelector) {
      vxm.detailGraphModule.selectedDimensionSelector = 'matrix'
    } else {
      vxm.detailGraphModule.selectedDimensionSelector = 'tree'
    }
  }

  private get selectedDimensions(): Dimension[] {
    return vxm.detailGraphModule.selectedDimensions
  }
}
</script>
