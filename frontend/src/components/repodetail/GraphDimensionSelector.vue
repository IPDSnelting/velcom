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
        :selected-dimensions="selectedDimensions"
        @update:selectedDimensions="updateSelectedDimensions"
        :all-dimensions="allDimensions"
      ></matrix-dimension-selection>
      <div>
        <tree-dimension-selection
          v-if="!useMatrixSelector"
          :selected-dimensions="selectedDimensions"
          @update:selectedDimensions="updateSelectedDimensions"
          :all-dimensions="allDimensions"
        >
        </tree-dimension-selection>
      </div>
    </v-card-text>
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import MatrixDimensionSelection from '@/components/graphs/detail/MatrixDimensionSelection.vue'
import TreeDimensionSelection from '@/components/graphs/detail/TreeDimensionSelection.vue'
import { Dimension } from '@/store/types'
import { Prop, Watch } from 'vue-property-decorator'
import { mdiFullscreen, mdiFullscreenExit } from '@mdi/js'

@Component({
  components: {
    'matrix-dimension-selection': MatrixDimensionSelection,
    'tree-dimension-selection': TreeDimensionSelection
  }
})
export default class GraphDimensionSelector extends Vue {
  @Prop({ default: false })
  private readonly isFullscreen!: boolean

  @Prop()
  private readonly allDimensions!: Dimension[]

  @Prop()
  private readonly selectedDimensions!: Dimension[]

  @Prop({ default: 'matrix' })
  private readonly selectorType!: 'tree' | 'matrix'

  @Watch('selectedDimensions')
  @Watch('repoId')
  private reloadGraphData() {
    this.$emit('reload-graph-data')
  }

  private updateSelectedDimensions(newDimensions: Dimension[]) {
    this.$emit('update:selectedDimensions', newDimensions)
  }

  private get useMatrixSelector() {
    return this.selectorType === 'matrix'
  }

  private set useMatrixSelector(useMatrixSelector: boolean) {
    this.$emit('update:selectorType', useMatrixSelector ? 'matrix' : 'tree')
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
