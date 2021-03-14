<template>
  <v-card>
    <v-card-text>
      <echarts-detail-graph
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
        :commit-to-compare="commitToCompare"
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
      </echarts-detail-graph>
    </v-card-text>
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import EchartsDetailGraph from '@/components/graphs//EchartsDetailGraph.vue'
import {
  AttributedDatapoint,
  ComparisonDataPoint,
  SeriesId,
  SeriesInformation
} from '@/store/types'
import { vxm } from '@/store'
import GraphDatapointDialog from '@/components/dialogs/GraphDatapointDialog.vue'
import { Prop } from 'vue-property-decorator'
import {
  roundDateDown,
  roundDateUp
} from '@/store/modules/comparisonGraphStore'

@Component({
  components: {
    GraphDatapointDialog,
    EchartsDetailGraph
  }
})
export default class ComparisonGraph extends Vue {
  // noinspection JSMismatchedCollectionQueryUpdate
  @Prop()
  private comparisonDatapoints!: ComparisonDataPoint[]

  // <!--<editor-fold desc="Getters">-->
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
    vxm.comparisonGraphModule.fetchComparisonGraph()
  }

  private get dataRangeMax() {
    return vxm.comparisonGraphModule.endTime
  }

  // noinspection JSUnusedLocalSymbols
  private set dataRangeMax(date: Date) {
    vxm.comparisonGraphModule.endTime = roundDateUp(date)
    vxm.comparisonGraphModule.fetchComparisonGraph()
  }

  private get commitToCompare(): AttributedDatapoint | null {
    return vxm.comparisonGraphModule.commitToCompare
  }

  // noinspection JSUnusedLocalSymbols
  private set commitToCompare(commit: AttributedDatapoint | null) {
    vxm.comparisonGraphModule.commitToCompare = commit
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
        (startValue === null || point.positionTime >= startValue) &&
        (endValue === null || point.positionTime <= endValue)
      ) {
        visibleDataPoints++
      }
    }
    return visibleDataPoints
  }

  // <!--<editor-fold desc="Zoom boilerplate">-->
  private get zoomXStartValue(): Date | null {
    return vxm.comparisonGraphModule.zoomXStart
  }

  // noinspection JSUnusedLocalSymbols
  private set zoomXStartValue(value: Date | null) {
    vxm.comparisonGraphModule.zoomXStart = value
  }

  private get zoomXEndValue(): Date | null {
    return vxm.comparisonGraphModule.zoomXEnd
  }

  // noinspection JSUnusedLocalSymbols
  private set zoomXEndValue(value: Date | null) {
    vxm.comparisonGraphModule.zoomXEnd = value
  }

  private get zoomYStartValue(): Date | null {
    return vxm.comparisonGraphModule.zoomYStart
  }

  // noinspection JSUnusedLocalSymbols
  private set zoomYStartValue(value: Date | null) {
    vxm.comparisonGraphModule.zoomYStart = value
  }

  private get zoomYEndValue(): Date | null {
    return vxm.comparisonGraphModule.zoomYEnd
  }

  // noinspection JSUnusedLocalSymbols
  private set zoomYEndValue(value: Date | null) {
    vxm.comparisonGraphModule.zoomYEnd = value
  }
  // <!--</editor-fold>-->
  // <!--</editor-fold>-->
}
</script>
