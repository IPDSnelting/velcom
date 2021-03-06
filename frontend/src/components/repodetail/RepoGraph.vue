<template>
  <v-container fluid class="pa-0 ma-0">
    <v-row align="baseline" justify="center" no-gutters>
      <v-col class="ma-0 pa-0">
        <v-card>
          <v-card-title class="mb-0 pb-0">
            <v-row no-gutters align="center" justify="space-between">
              <v-col class="ma-0 pa-0">
                <v-btn-toggle
                  :value="selectedGraphComponent"
                  @change="setSelectedGraphComponent"
                  mandatory
                >
                  <v-btn
                    v-for="{ component, name } in availableGraphComponents"
                    :key="name"
                    :value="component"
                  >
                    {{ name }}
                  </v-btn>
                </v-btn-toggle>
              </v-col>
              <v-col cols="auto">
                <share-graph-link-dialog />
              </v-col>
            </v-row>
          </v-card-title>
          <v-card-text>
            <v-row no-gutters>
              <v-col style="position: relative">
                <component
                  ref="graphComponent"
                  :placeholderHeight="graphPlaceholderHeight"
                  :is="selectedGraphComponent"
                  :dimensions="selectedDimensions"
                  :beginYAtZero="yStartsAtZero"
                  :dayEquidistant="dayEquidistantGraphSelected"
                  @wheel="overscrollToZoom.scrolled($event)"
                ></component>
                <v-overlay
                  v-if="overlayText"
                  absolute
                  class="ma-0 pa-0"
                  z-index="20"
                  color="black"
                >
                  <span class="text-h6">{{ overlayText }}</span>
                </v-overlay>
              </v-col>
            </v-row>
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
import ShareGraphLinkDialog from '@/views/ShareGraphLinkDialog.vue'
import { vxm } from '@/store'
import EchartsDetailGraph from '@/components/graphs/EchartsDetailGraph.vue'
import DytailGraph from '@/components/graphs/DytailGraph.vue'
import OverscrollToZoom from '@/components/graphs/OverscrollToZoom'
import GraphPlaceholder from '@/components/graphs/GraphPlaceholder.vue'
import { Dimension } from '@/store/types'
import { Prop, Watch } from 'vue-property-decorator'
import { getInnerHeight } from '@/util/MeasurementUtils'

const availableGraphComponents = [
  {
    predicate: () => {
      // Do not care about zooming, only use echarts when he have only a handful of data points
      const points =
        vxm.detailGraphModule.detailGraph.length *
        vxm.detailGraphModule.selectedDimensions.length
      return points < 30_000
    },
    component: EchartsDetailGraph,
    name: 'Fancy'
  },
  {
    predicate: () => {
      // matches from first to last. this one is the fallback
      return true
    },
    component: DytailGraph,
    name: 'Fast'
  }
]

@Component({
  components: {
    'share-graph-link-dialog': ShareGraphLinkDialog,
    'matrix-dimension-selection': MatrixDimensionSelection,
    'normal-dimension-selection': DimensionSelection
  }
})
export default class RepoGraphs extends Vue {
  private graphPlaceholderHeight: number = 100
  private selectedGraphComponent: typeof Vue | null = GraphPlaceholder
  private overscrollToZoom = new OverscrollToZoom()

  @Prop()
  private readonly reloadGraphDataCounter!: number

  private get selectedDimensions(): Dimension[] {
    return vxm.detailGraphModule.selectedDimensions
  }

  private get availableGraphComponents() {
    return availableGraphComponents
  }

  private get dayEquidistantGraphSelected() {
    return vxm.detailGraphModule.dayEquidistantGraph
  }

  private get yStartsAtZero() {
    return vxm.detailGraphModule.beginYScaleAtZero
  }

  private setSelectedGraphComponent(component: typeof Vue) {
    if (this.selectedGraphComponent === GraphPlaceholder) {
      return
    }
    this.selectedGraphComponent = component
  }

  private get overlayText() {
    if (this.selectedDimensions.length === 0) {
      return 'Please select a benchmark and metric below.'
    }
    // We do not show an overlay if we have no datapoints as you can load more by zooming out
    return null
  }

  @Watch('reloadGraphDataCounter')
  private async retrieveGraphData(): Promise<void> {
    this.selectedGraphComponent = GraphPlaceholder

    if (this.$refs.graphComponent) {
      const element = (this.$refs.graphComponent as Vue).$el as HTMLElement
      this.graphPlaceholderHeight = getInnerHeight(element)
    }

    await vxm.detailGraphModule.fetchDetailGraph()
    const correctSeries = this.availableGraphComponents.find(it =>
      it.predicate()
    )
    if (correctSeries) {
      this.selectedGraphComponent = correctSeries.component
    }
  }

  private mounted() {
    this.retrieveGraphData()
  }
}
</script>
