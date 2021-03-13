<template>
  <v-card>
    <v-card-text>
      <echarts-detail-graph
        :zoom-x-start-value.sync="zoomXStartValue"
        :zoom-x-end-value.sync="zoomXEndValue"
        :zoom-y-start-value.sync="zoomYStartValue"
        :zoom-y-end-value.sync="zoomYEndValue"
        :datapoints="comparisonDatapoints"
        :data-range-min="dataRangeMin"
        :data-range-max="dataRangeMax"
        :series-information="seriesInformation"
        :visible-point-count="visiblePointCount"
        :point-table-formatter="pointFormatter"
      ></echarts-detail-graph>
    </v-card-text>
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import EchartsDetailGraph from '@/components/graphs//EchartsDetailGraph.vue'
import { ComparisonDataPoint, SeriesInformation } from '@/store/types'
import { vxm } from '@/store'
import { formatDate } from '@/util/TimeUtil'
import { escapeHtml } from '@/util/TextUtils'

@Component({
  components: {
    EchartsDetailGraph
  }
})
export default class ComparisonGraph extends Vue {
  private comparisonDatapoints: ComparisonDataPoint[] = []

  // <!--<editor-fold desc="Getters">-->
  private get seriesInformation(): SeriesInformation[] {
    return Array.from(vxm.comparisonGraphModule.selectedBranches.keys()).map(
      repoId => ({
        id: repoId,
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

  private get dataRangeMax() {
    return vxm.comparisonGraphModule.endTime
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

  private get visiblePointCount() {
    const startValue = this.zoomXStartValue
    const endValue = this.zoomXEndValue

    // TODO: Is this a performance problem? There might be 10.000+ items here
    // and this method is called every time the slider is dragged or the user
    // zooms using the mouse wheel
    let visibleDataPoints = 0
    for (const point of this.comparisonDatapoints) {
      if (
        (startValue === null || point.time >= startValue) &&
        (endValue === null || point.time <= endValue)
      ) {
        visibleDataPoints++
      }
    }
    return visibleDataPoints
  }
  // <!--</editor-fold>-->

  private pointFormatter(point: ComparisonDataPoint) {
    const time = formatDate(point.time)
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
                ${escapeHtml(point.author)} at ${time}
              </td>
            </tr>
            `
  }

  private async mounted() {
    vxm.comparisonGraphModule.toggleRepoBranch({
      repoId: '44bb5c8d-b20d-4bef-bdad-c92767dfa489',
      branch: 'main'
    })
    vxm.comparisonGraphModule.selectedDimension = vxm.repoModule
      .repoById('44bb5c8d-b20d-4bef-bdad-c92767dfa489')!
      .dimensions.find(
        it => it.benchmark === 'backend' && it.metric === 'build_time'
      )!
    this.comparisonDatapoints = await vxm.comparisonGraphModule.fetchComparisonGraph()
  }
}
</script>
