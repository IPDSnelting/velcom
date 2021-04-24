<template>
  <v-card :style="{ height: height }">
    <v-card-text style="height: 100%" class="py-0 my-0">
      <component
        :is="graphComponent"
        :zoom-x-start-value.sync="zoomXStartValue"
        :zoom-x-end-value.sync="zoomXEndValue"
        :zoom-y-start-value.sync="zoomYStartValue"
        :zoom-y-end-value.sync="zoomYEndValue"
        :datapoints="comparisonDatapoints"
        :data-range-min.sync="dataRangeMin"
        :data-range-max.sync="dataRangeMax"
        :series-information="seriesInformation"
        :visible-point-count="visiblePointCount"
        :reference-datapoint="referenceDatapoint"
        :begin-y-at-zero="beginYAtZero"
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
            :reference-datapoint.sync="referenceDatapoint"
            :no-compare="true"
            @close="closeDialog()"
          ></graph-datapoint-dialog>
        </template>
      </component>
    </v-card-text>
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import EchartsDetailGraph from '@/components/graphs//EchartsDetailGraph.vue'
import DytailGraph from '@/components/graphs/DytailGraph.vue'
import {
  AttributedDatapoint,
  ComparisonDataPoint,
  SeriesId,
  SeriesInformation
} from '@/store/types'
import { vxm } from '@/store'
import GraphDatapointDialog from '@/components/dialogs/GraphDatapointDialog.vue'
import { Prop } from 'vue-property-decorator'
import OverscrollToZoom from '@/components/graphs/OverscrollToZoom'
import { roundDateDown, roundDateUp } from '@/util/TimeUtil'

@Component({
  components: {
    GraphDatapointDialog,
    EchartsDetailGraph,
    DytailGraph
  }
})
export default class ComparisonGraph extends Vue {
  private graphRefreshKey: number = 0

  // noinspection JSMismatchedCollectionQueryUpdate
  @Prop()
  private readonly comparisonDatapoints!: ComparisonDataPoint[]

  @Prop()
  private readonly graphComponent!: typeof Vue

  @Prop()
  private readonly beginYAtZero!: boolean

  @Prop({ default: '80vh' })
  private readonly height!: string

  // <!--<editor-fold desc="Getters">-->
  private get overscrollToZoom() {
    return new OverscrollToZoom(() => {
      // Not needed as the RepoComparison will handle refresh
      // FIXME: Also move repo detail refreshes to parent component and remove this parameter
      return Promise.resolve()
    }, vxm.comparisonGraphModule)
  }

  private get seriesInformation(): SeriesInformation[] {
    return Array.from(vxm.comparisonGraphModule.selectedBranches.keys()).map(
      repoId => ({
        id: repoId as SeriesId,
        color: vxm.colorModule.colorByIndex(
          vxm.repoModule.allRepos.findIndex(it => it.id === repoId)
        ),
        displayName: vxm.repoModule.repoById(repoId)!.name
      })
    )
  }

  private get dataRangeMin() {
    return vxm.comparisonGraphModule.startTime
  }

  // noinspection JSUnusedLocalSymbols
  private set dataRangeMin(date: Date) {
    vxm.comparisonGraphModule.startTime = roundDateDown(date)
  }

  private get dataRangeMax() {
    return vxm.comparisonGraphModule.endTime
  }

  // noinspection JSUnusedLocalSymbols
  private set dataRangeMax(date: Date) {
    vxm.comparisonGraphModule.endTime = roundDateUp(date)
  }

  private get referenceDatapoint(): AttributedDatapoint | null {
    return vxm.comparisonGraphModule.referenceDatapoint
  }

  // noinspection JSUnusedLocalSymbols
  private set referenceDatapoint(datapoint: AttributedDatapoint | null) {
    vxm.comparisonGraphModule.referenceDatapoint = datapoint
  }

  private get visiblePointCount() {
    const startValue = this.zoomXStartValue
    const endValue = this.zoomXEndValue

    // TODO: Is this a performance problem? There might be 10.000+ items here
    // and this method is called every time the slider is dragged or the user
    // zooms using the mouse wheel
    let visibleDataPoints = 0
    for (const point of this.comparisonDatapoints) {
      if (
        (startValue === null || point.positionTime.getTime() >= startValue) &&
        (endValue === null || point.positionTime.getTime() <= endValue)
      ) {
        visibleDataPoints++
      }
    }
    return visibleDataPoints
  }

  // <!--<editor-fold desc="Zoom boilerplate">-->
  private get zoomXStartValue(): number | null {
    return vxm.comparisonGraphModule.zoomXStartValue
  }

  // noinspection JSUnusedLocalSymbols
  private set zoomXStartValue(value: number | null) {
    vxm.comparisonGraphModule.zoomXStartValue = value
  }

  private get zoomXEndValue(): number | null {
    return vxm.comparisonGraphModule.zoomXEndValue
  }

  // noinspection JSUnusedLocalSymbols
  private set zoomXEndValue(value: number | null) {
    vxm.comparisonGraphModule.zoomXEndValue = value
  }

  private get zoomYStartValue(): number | null {
    return vxm.comparisonGraphModule.zoomYStartValue
  }

  // noinspection JSUnusedLocalSymbols
  private set zoomYStartValue(value: number | null) {
    vxm.comparisonGraphModule.zoomYStartValue = value
  }

  private get zoomYEndValue(): number | null {
    return vxm.comparisonGraphModule.zoomYEndValue
  }

  // noinspection JSUnusedLocalSymbols
  private set zoomYEndValue(value: number | null) {
    vxm.comparisonGraphModule.zoomYEndValue = value
  }
  // <!--</editor-fold>-->
  // <!--</editor-fold>-->

  private resetZoom() {
    this.zoomYStartValue = null
    this.zoomYEndValue = null
    this.zoomXStartValue = this.dataRangeMin.getTime()
    this.zoomXEndValue = this.dataRangeMax.getTime()
    this.graphRefreshKey++
  }
}
</script>
