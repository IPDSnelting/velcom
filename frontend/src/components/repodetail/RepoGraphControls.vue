<template>
  <v-container fluid class="pa-0 ma-0">
    <v-row align="baseline" justify="center" no-gutters>
      <v-col>
        <v-card>
          <v-card-text>
            <v-container fluid class="ma-0 px-5 pb-0">
              <v-row align="start" justify="space-between" no-gutters>
                <v-col :md="useMatrixSelector ? '' : '5'" sm="12" cols="12">
                  <v-btn
                    @click="useMatrixSelector = !useMatrixSelector"
                    text
                    color="primary"
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
                </v-col>
                <v-col class="d-flex justify-end">
                  <v-btn
                    color="primary"
                    outlined
                    class="mr-4"
                    @click="
                      dayEquidistantGraphSelected = !dayEquidistantGraphSelected
                    "
                  >
                    <span v-if="dayEquidistantGraphSelected">
                      Disable Day-Equidistant Graph
                    </span>
                    <span v-else>Enable Day-Equidistant Graph</span>
                  </v-btn>
                  <v-btn
                    @click="yStartsAtZero = !yStartsAtZero"
                    color="primary"
                    outlined
                  >
                    <span v-if="yStartsAtZero">
                      Begin Y-Axis at minimum value
                    </span>
                    <span v-else>Begin Y-Axis at zero</span>
                  </v-btn>
                </v-col>
              </v-row>
            </v-container>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
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
export default class RepoGraphControls extends Vue {
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

  private get yStartsAtZero(): boolean {
    return vxm.detailGraphModule.beginYScaleAtZero
  }

  private set yStartsAtZero(startsAtZero: boolean) {
    vxm.detailGraphModule.beginYScaleAtZero = startsAtZero
  }

  private get dayEquidistantGraphSelected() {
    return vxm.detailGraphModule.dayEquidistantGraph
  }

  private set dayEquidistantGraphSelected(selected: boolean) {
    vxm.detailGraphModule.dayEquidistantGraph = selected
  }
}
</script>
