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
                  :zoom-x-start-value.sync="zoomXStartValue"
                  :zoom-x-end-value.sync="zoomXEndValue"
                  :zoom-y-start-value.sync="zoomYStartValue"
                  :zoom-y-end-value.sync="zoomYEndValue"
                  :datapoints="datapoints"
                  :data-range-min.sync="dataRangeMin"
                  :data-range-max.sync="dataRangeMax"
                  :series-information="seriesInformation"
                  :visible-point-count="visiblePointCount"
                  :point-table-formatter="pointFormatter"
                  :reference-datapoint="referenceDatapoint"
                  :commit-to-compare="commitToCompare"
                  :begin-y-at-zero="yStartsAtZero"
                  :refresh-key="graphRefreshKey"
                  @wheel="overscrollToZoom.scrolled($event)"
                  @reset-zoom="resetZoom"
                ></component>
                <v-overlay
                  v-if="overlayText"
                  absolute
                  class="ma-0 pa-0"
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
import {
  AttributedDatapoint,
  DetailDataPoint,
  Dimension,
  DimensionId,
  SeriesInformation
} from '@/store/types'
import { Prop, Watch } from 'vue-property-decorator'
import { getInnerHeight } from '@/util/MeasurementUtils'
import { escapeHtml } from '@/util/TextUtils'
import { formatDate } from '@/util/TimeUtil'
import {
  roundDateDown,
  roundDateUp
} from '@/store/modules/comparisonGraphStore'

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
  private graphRefreshKey = 0

  @Prop()
  private readonly reloadGraphDataCounter!: number

  private get selectedDimensions(): Dimension[] {
    return vxm.detailGraphModule.selectedDimensions
  }

  private get availableGraphComponents() {
    return availableGraphComponents
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

  // <!--<editor-fold desc="Graph bindings">-->
  private resetZoom() {
    this.zoomYStartValue = null
    this.zoomYEndValue = null
    this.zoomXStartValue = this.dataRangeMin.getTime()
    this.zoomXEndValue = this.dataRangeMax.getTime()
    this.graphRefreshKey++
  }

  private pointFormatter(point: DetailDataPoint) {
    const committerDate = formatDate(point.committerDate)
    return `
            <tr>
              <td>Hash</td>
              <td>${escapeHtml(point.hash)}</td>
            </tr>
            </tr>
              <td>Message</td>
              <td>${escapeHtml(point.summary)}</td>
            </tr>
            <tr>
              <td>Author</td>
              <td>
                ${escapeHtml(point.author)} at ${committerDate}
              </td>
            </tr>
            `
  }

  private get datapoints(): DetailDataPoint[] {
    return vxm.detailGraphModule.detailGraph
  }

  private get seriesInformation(): SeriesInformation[] {
    return this.selectedDimensions.map(dimension => ({
      displayName: dimension.toString(),
      id: dimension.toString(),
      color: this.dimensionColor(dimension)
    }))
  }

  private dimensionColor(dimension: DimensionId) {
    return vxm.colorModule.colorByIndex(
      vxm.detailGraphModule.colorIndex(dimension)!
    )
  }

  private get visiblePointCount() {
    return vxm.detailGraphModule.visiblePoints
  }

  private get dataRangeMin() {
    return vxm.detailGraphModule.startTime
  }

  // noinspection JSUnusedLocalSymbols
  private set dataRangeMin(date: Date) {
    vxm.detailGraphModule.startTime = roundDateDown(date)
    vxm.detailGraphModule.fetchDetailGraph()
  }

  private get dataRangeMax() {
    return vxm.detailGraphModule.endTime
  }

  // noinspection JSUnusedLocalSymbols
  private set dataRangeMax(date: Date) {
    vxm.detailGraphModule.endTime = roundDateUp(date)
    vxm.detailGraphModule.fetchDetailGraph()
  }

  private get commitToCompare(): AttributedDatapoint | null {
    return vxm.detailGraphModule.commitToCompare
  }

  // noinspection JSUnusedLocalSymbols
  private set commitToCompare(commit: AttributedDatapoint | null) {
    vxm.detailGraphModule.commitToCompare = commit
  }

  private get referenceDatapoint(): AttributedDatapoint | null {
    return vxm.detailGraphModule.referenceDatapoint
  }

  // noinspection JSUnusedLocalSymbols
  private set referenceDatapoint(datapoint: AttributedDatapoint | null) {
    vxm.detailGraphModule.referenceDatapoint = datapoint
  }

  // <!--<editor-fold desc="Zoom boilerplate">-->
  private get zoomXStartValue(): number | null {
    return vxm.detailGraphModule.zoomXStartValue
  }

  // noinspection JSUnusedLocalSymbols
  private set zoomXStartValue(value: number | null) {
    vxm.detailGraphModule.zoomXStartValue = value
  }

  private get zoomXEndValue(): number | null {
    return vxm.detailGraphModule.zoomXEndValue
  }

  // noinspection JSUnusedLocalSymbols
  private set zoomXEndValue(value: number | null) {
    vxm.detailGraphModule.zoomXEndValue = value
  }

  private get zoomYStartValue(): number | null {
    return vxm.detailGraphModule.zoomYStartValue
  }

  // noinspection JSUnusedLocalSymbols
  private set zoomYStartValue(value: number | null) {
    vxm.detailGraphModule.zoomYStartValue = value
  }

  private get zoomYEndValue(): number | null {
    return vxm.detailGraphModule.zoomYEndValue
  }

  // noinspection JSUnusedLocalSymbols
  private set zoomYEndValue(value: number | null) {
    vxm.detailGraphModule.zoomYEndValue = value
  }
  // <!--</editor-fold>-->
  // <!--</editor-fold>-->
}
</script>
