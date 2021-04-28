<template>
  <v-card>
    <v-card-text style="height: 70vh">
      <v-row no-gutters style="height: 100%">
        <v-col style="position: relative; height: 100%">
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
          >
            <template
              #dialog="{
                dialogOpen,
                selectedDatapoint,
                seriesInformation,
                closeDialog
              }"
            >
              <graph-datapoint-dialog
                :dialog-open="dialogOpen"
                :selected-datapoint="selectedDatapoint"
                :series-id="seriesInformation.id"
                :commit-to-compare.sync="commitToCompare"
                :reference-datapoint.sync="referenceDatapoint"
                @close="closeDialog()"
              ></graph-datapoint-dialog>
            </template>
          </component>
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
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import MatrixDimensionSelection from '@/components/graphs/MatrixDimensionSelection.vue'
import DimensionSelection from '@/components/graphs/DimensionSelection.vue'
import { vxm } from '@/store'
import OverscrollToZoom from '@/components/graphs/OverscrollToZoom'
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
import GraphDatapointDialog from '@/components/dialogs/GraphDatapointDialog.vue'
import { selectGraphVariant } from '@/util/GraphVariantSelection'

@Component({
  components: {
    GraphDatapointDialog,
    'matrix-dimension-selection': MatrixDimensionSelection,
    'normal-dimension-selection': DimensionSelection
  }
})
export default class RepoGraphs extends Vue {
  private graphPlaceholderHeight: number = 100
  private graphRefreshKey = 0

  @Prop()
  private readonly reloadGraphDataCounter!: number

  @Prop()
  private readonly selectedGraphComponent!: typeof Vue

  private get selectedDimensions(): Dimension[] {
    return vxm.detailGraphModule.selectedDimensions
  }

  private get yStartsAtZero() {
    return vxm.detailGraphModule.beginYScaleAtZero
  }

  private get overscrollToZoom() {
    return new OverscrollToZoom(
      () => vxm.detailGraphModule.fetchDetailGraph(),
      vxm.detailGraphModule
    )
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
    if (this.$refs.graphComponent) {
      const element = (this.$refs.graphComponent as Vue).$el as HTMLElement
      this.graphPlaceholderHeight = getInnerHeight(element)
    }

    await vxm.detailGraphModule.fetchDetailGraph()
    const correctSeries = selectGraphVariant(
      vxm.detailGraphModule.visiblePoints *
        vxm.detailGraphModule.selectedDimensions.length
    )
    if (correctSeries) {
      this.$emit('selectedGraphComponent', correctSeries.component)
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
    const committerDate = formatDate(point.committerTime)
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
    vxm.detailGraphModule.startTime = date
    vxm.detailGraphModule.fetchDetailGraph()
  }

  private get dataRangeMax() {
    return vxm.detailGraphModule.endTime
  }

  // noinspection JSUnusedLocalSymbols
  private set dataRangeMax(date: Date) {
    vxm.detailGraphModule.endTime = date
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
