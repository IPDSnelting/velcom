<template>
  <div>
    <v-dialog
      fullscreen
      scrollable
      v-model="dimensionSelectionInFullscreen"
      style="margin-top: 64px !important"
      transition="dialog-transition"
    >
      <graph-dimension-selector
        @reload-graph-data="$emit('reload-graph-data')"
        @shrink="dimensionSelectionInFullscreen = false"
        @update:selectedDimensions="updateSelectedDimensions"
        @update:selectorType="updateSelectorType"
        :is-fullscreen="true"
        :all-dimensions="allDimensions"
        :selected-dimensions="selectedDimensions"
        :selector-type="selectorType"
      >
      </graph-dimension-selector>
    </v-dialog>
    <graph-dimension-selector
      @expand="dimensionSelectionInFullscreen = true"
      @reload-graph-data="$emit('reload-graph-data')"
      @update:selectedDimensions="updateSelectedDimensions"
      @update:selectorType="updateSelectorType"
      :is-fullscreen="false"
      :all-dimensions="allDimensions"
      :selected-dimensions="selectedDimensions"
      :selector-type="selectorType"
    ></graph-dimension-selector>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import GraphDimensionSelector from '@/components/repodetail/GraphDimensionSelector.vue'
import { Prop } from 'vue-property-decorator'
import { Dimension } from '@/store/types'

@Component({
  components: {
    GraphDimensionSelector
  }
})
export default class ExpandableDimensionSelection extends Vue {
  private dimensionSelectionInFullscreen = false

  @Prop()
  private readonly allDimensions!: Dimension[]

  @Prop()
  private readonly selectedDimensions!: Dimension[]

  @Prop({ default: 'matrix' })
  private readonly selectorType!: 'tree' | 'matrix'

  private updateSelectedDimensions(newDimensions: Dimension[]) {
    this.$emit('update:selectedDimensions', newDimensions)
  }

  private updateSelectorType(type: 'tree' | 'matrix') {
    this.$emit('update:selectorType', type)
  }
}
</script>
