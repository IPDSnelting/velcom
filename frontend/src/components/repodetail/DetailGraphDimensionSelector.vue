<template>
  <v-card :class="{ fullscreen: isFullscreen }">
    <v-card-text>
      <div class="d-flex align-center justify-space-between">
        <v-btn
          @click="useMatrixSelector = !useMatrixSelector"
          color="primary"
          :outlined="!isFullscreen"
          :text="!isFullscreen"
          style="display: block; margin: auto"
        >
          <span v-if="useMatrixSelector">Use tree selector</span>
          <span v-if="!useMatrixSelector">Use matrix selector</span>
        </v-btn>

        <v-btn v-if="!isFullscreen" icon @click="$emit('expand')">
          <v-icon>{{ expandIcon }}</v-icon>
        </v-btn>
        <v-btn v-if="isFullscreen" icon @click="$emit('shrink')">
          <v-icon>{{ shrinkIcon }}</v-icon>
        </v-btn>
      </div>
      <matrix-dimension-selection
        v-if="useMatrixSelector"
        :selectedDimensions="selectedDimensions"
        :repoId="repoId"
      ></matrix-dimension-selection>
      <div>
        <tree-dimension-selection
          v-if="!useMatrixSelector"
          :selectedDimensions="selectedDimensions"
          :repoId="repoId"
        >
        </tree-dimension-selection>
      </div>
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
import { Prop, Watch } from 'vue-property-decorator'
import { mdiFullscreen, mdiFullscreenExit } from '@mdi/js'

@Component({
  components: {
    'matrix-dimension-selection': MatrixDimensionSelection,
    'tree-dimension-selection': DimensionSelection
  }
})
export default class DetailGraphDimensionSelector extends Vue {
  @Prop({ default: false })
  private readonly isFullscreen!: boolean

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

  // ICONS
  private readonly expandIcon = mdiFullscreen
  private readonly shrinkIcon = mdiFullscreenExit
}
</script>

<style scoped>
.fullscreen {
  background-color: rgba(255, 255, 255, 0.9);
  padding-top: 12px !important;
}
</style>
