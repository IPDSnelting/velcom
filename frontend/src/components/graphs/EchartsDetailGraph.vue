<template>
  <v-container fluid>
    <v-row align="center" justify="center">
      <v-col>
        <datapoint-dialog
          v-if="pointDialogDatapoint"
          :dialogOpen="pointDialogOpen"
          :selectedDatapoint="pointDialogDatapoint"
          :dimension="pointDialogDimension"
          @close="pointDialogOpen = false"
          @compareCommits="pointDialogExecuteCompare"
        ></datapoint-dialog>
        <div id="chart-container">
          <v-chart
            :autoresize="true"
            :options="chartOptions"
            @datazoom="echartsZoomed"
            @contextmenu="echartsOpenContextMenu"
            @click="echartsClicked"
            @restore="echartsZoomReset"
          />
        </div>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import {
  DetailDataPoint,
  DetailDataPointValue,
  Dimension,
  DimensionId
} from '@/store/types'
import { EChartOption } from 'echarts'
import { Prop, Watch } from 'vue-property-decorator'
import EChartsComp from 'vue-echarts'

import 'echarts/lib/chart/line'
import 'echarts/lib/chart/graph'
import 'echarts/lib/component/polar'
import 'echarts/lib/component/tooltip'
import 'echarts/lib/component/legend'
import 'echarts/lib/component/dataZoomSlider'
import 'echarts/lib/component/dataZoom'
import 'echarts/lib/component/dataZoomInside'
import 'echarts/lib/component/toolbox'
import 'echarts/lib/component/brush'
import 'echarts/lib/component/markLine'
import 'echarts/lib/component/markPoint'
import { vxm } from '@/store'
import DetailDatapointDialog from '@/components/dialogs/DetailDatapointDialog.vue'
import { DimensionDetailPoint } from '@/store/modules/detailGraphStore'

type ValidEchartsSeries = EChartOption.SeriesLine | EChartOption.SeriesGraph
type SeriesGenerationFunction = (id: DimensionId) => ValidEchartsSeries

class EchartsDataPoint {
  // convenience methods for accessing the value
  readonly time: Date
  readonly dataValue: number

  // Used to display the symbol
  readonly symbol: string

  // Needs THIS NAME for Echarts.
  // A `get` method does not work for some reason
  /**
   * First entry is the {time}, second the {dataValue}
   */
  readonly value: [Date, number]

  // Used in the graph series display to identify a node
  /**
   * The hash of the point this point refers to
   */
  readonly name: string

  // Allows styling the individual item
  readonly itemStyle: {
    color: string
    borderColor: string
    borderWidth: number
  }

  readonly summary: string

  constructor(
    time: Date,
    dataValue: number,
    symbol: string,
    name: string,
    color: string,
    borderColor: string,
    summary: string
  ) {
    this.time = time
    this.dataValue = dataValue
    this.symbol = symbol
    this.value = [this.time, this.dataValue]
    this.name = name
    this.itemStyle = {
      color: color,
      borderColor: borderColor,
      borderWidth: 2
    }
    this.summary = summary
  }
}

@Component({
  components: {
    'datapoint-dialog': DetailDatapointDialog,
    'v-chart': EChartsComp
  }
})
export default class EchartsDetailGraph extends Vue {
  // <!--<editor-fold desc="PROPS">-->
  @Prop()
  private dimensions!: Dimension[]

  @Prop({ default: false })
  private beginYAtZero!: boolean
  // <!--</editor-fold>-->

  // <!--<editor-fold desc="FIELDS">-->
  private chartOptions: EChartOption = {}
  private seriesGenerator: SeriesGenerationFunction = this.buildLineSeries
  private zoomStartPercent: number = 0
  private zoomEndPercent: number = 1

  // >>>> Datapoint Dialog >>>>
  private pointDialogOpen: boolean = false
  private pointDialogDatapoint: DetailDataPoint | null = null
  private pointDialogDimension: Dimension | null = null
  // <<<< Datapoint Dialog <<<<
  //  <!--</editor-fold>-->

  private get detailDataPoints(): DetailDataPoint[] {
    return [
      new DetailDataPoint(
        'Commit1',
        [],
        'Peter3',
        new Date(new Date().getTime()),
        'this is an OG point',
        this.randomGarbage()
      ),
      new DetailDataPoint(
        'Commit2',
        ['Commit1'],
        'Peter',
        new Date(new Date().getTime() - 1000 * 60 * 60),
        'this is a point',
        this.randomGarbage(false)
      ),
      new DetailDataPoint(
        'Commit3',
        ['Commit1', 'Commit2'],
        'Peter2',
        new Date(new Date().getTime() - 1000 * 60 * 120),
        'this is a point!!',
        this.randomGarbage()
      )
    ]
    // return vxm.detailGraphModule.detailGraph
  }

  private get minDateValue(): number {
    const min = Math.min.apply(
      Math,
      this.detailDataPoints.map(it => it.authorDate.getTime())
    )
    return min || 0
  }

  private get maxDateValue(): number {
    const max = Math.max.apply(
      Math,
      this.detailDataPoints.map(it => it.authorDate.getTime())
    )
    return max || 0
  }

  private randomGarbage(
    successful: boolean = true
  ): Map<DimensionId, DetailDataPointValue> {
    const map: Map<DimensionId, DetailDataPointValue> = new Map()
    if (successful) {
      for (let i = 0; i < this.dimensions.length; i++) {
        map.set(this.dimensions[i], Math.random() * 20 - 5)
      }
    } else {
      const random = Math.random()
      for (let i = 0; i < this.dimensions.length; i++) {
        map.set(
          this.dimensions[i],
          random < 0.25
            ? 'NO_MEASUREMENT'
            : random < 0.5
            ? 'NO_RUN'
            : random < 0.75
            ? 'RUN_FAILED'
            : 'MEASUREMENT_FAILED'
        )
      }
    }
    return map
  }

  // <!--<editor-fold desc="ECHARTS GRAPH OPTIONS">-->
  private updateGraph() {
    console.log('UPDATED')

    this.selectAppropriateSeries()

    this.chartOptions = {
      backgroundColor: this.graphBackgroundColor,
      grid: {
        left: 20,
        right: 20,
        containLabel: true
      },
      xAxis: {
        type: 'time',
        min: 'dataMin',
        max: 'dataMax'
      },
      yAxis: {
        type: 'value',
        scale: this.beginYAtZero
      },
      toolbox: {
        left: 'center',
        show: true,
        showTitle: false, // hide the default text so they don't overlap each other
        feature: {
          restore: { show: true, title: 'Reset' },
          saveAsImage: { show: true, title: 'Save as Image' },
          dataZoom: {
            title: {
              zoom: 'Zoom (brush)',
              back: 'Reset zoom'
            },
            start: this.zoomStartPercent * 100,
            end: this.zoomEndPercent * 100
          }
        },
        tooltip: {
          show: true
        }
      },
      dataZoom: [
        {
          type: 'inside',
          // Start at the correct place when changing the series type
          start: this.zoomStartPercent * 100,
          end: this.zoomEndPercent * 100
        },
        {
          type: 'slider',
          // Start at the correct place when changing the series type
          start: this.zoomStartPercent * 100,
          end: this.zoomEndPercent * 100
        }
      ],
      tooltip: {
        trigger: 'axis',
        axisPointer: {},
        // TODO: Extract in own helper?
        formatter: this.tooltipFormatter
      },
      series: this.dimensions.map(this.seriesGenerator)
    }

    this.updateReferenceDatapoint()
  }

  private tooltipFormatter(
    params: EChartOption.Tooltip.Format | EChartOption.Tooltip.Format[]
  ) {
    const values = Array.isArray(params) ? params : [params]

    const dimensionRows = values.map(val => {
      const dimension = this.dimensions[val.seriesIndex || 0]
      const color = this.dimensionColor(dimension)
      const datapoint = val.data as EchartsDataPoint

      return `
                <tr>
                  <td>
                    <span class="color-preview" style="background-color: ${color}"></span>
                    ${dimension.benchmark} - ${dimension.metric}
                  </td>
                  <td>${this.numberFormat.format(datapoint.dataValue)}</td>
                </tr>
                `
    })

    const samplePoint = values[0].data as EchartsDataPoint

    return `
                <table class="echarts-tooltip-table">
                  <tr>
                    <td>Hash</td>
                    <td>${samplePoint.name}</td>
                  </tr>
                  </tr>
                    <td>Message</td>
                    <td>${samplePoint.summary}</td>
                  </tr>
                  ${dimensionRows.join('\n')}
                </table>
            `
  }

  // <!--</editor-fold>-->

  // <!--<editor-fold desc="SERIES GENERATION">-->
  private buildPointsForSingle(dimension: DimensionId) {
    const findFirstSuccessful = () => {
      for (let i = 0; i < this.detailDataPoints.length; i++) {
        const value = this.detailDataPoints[i].values.get(dimension)
        if (typeof value === 'number') {
          return value
        }
      }
      return 0
    }

    let lastSuccessfulValue: number = findFirstSuccessful()

    return this.detailDataPoints.map(point => {
      let symbol = 'circle'
      let color = this.dimensionColor(dimension)
      let borderColor = color

      let pointValue = point.values.get(dimension)
      if (typeof pointValue !== 'number') {
        pointValue = lastSuccessfulValue
      }
      lastSuccessfulValue = pointValue

      if (point.failed(dimension)) {
        // grey circle
        symbol = this.crossIcon
        color = this.graphFailedOrUnbenchmarkedColor
        borderColor = color
      } else if (point.unbenchmarked(dimension)) {
        // empty circle with outline
        color = this.graphBackgroundColor
        borderColor = this.graphFailedOrUnbenchmarkedColor
      }

      return new EchartsDataPoint(
        point.authorDate,
        pointValue,
        symbol,
        point.hash,
        color,
        borderColor,
        point.summary
      )
    })
  }

  private get echartsDataPoints(): Map<DimensionId, EchartsDataPoint[]> {
    const map: Map<DimensionId, EchartsDataPoint[]> = new Map()

    this.dimensions.forEach(dimension =>
      map.set(dimension, this.buildPointsForSingle(dimension))
    )

    return map
  }

  private buildLineSeries(dimension: DimensionId): EChartOption.SeriesLine {
    // noinspection JSMismatchedCollectionQueryUpdate
    const echartPoints: EchartsDataPoint[] = this.echartsDataPoints.get(
      dimension
    )!

    return {
      type: 'line',
      showSymbol: true,
      symbol: ((value: EchartsDataPoint) => value.symbol) as any,
      symbolSize: 6,
      lineStyle: {
        color: this.dimensionColor(dimension)
      },
      data: echartPoints as any
    }
  }

  private buildGraphSeries(dimension: DimensionId): EChartOption.SeriesGraph {
    // noinspection JSMismatchedCollectionQueryUpdate
    const echartPoints: EchartsDataPoint[] = this.echartsDataPoints.get(
      dimension
    )!
    const links: EChartOption.SeriesGraph.LinkObject[] = this.detailDataPoints.flatMap(
      point => {
        return point.parents.map(parent => ({
          source: point.hash,
          target: parent
        }))
      }
    )

    return {
      type: 'graph',
      coordinateSystem: 'cartesian2d',
      label: {
        show: false
      },
      emphasis: {
        label: {
          show: false
        }
      },
      edgeSymbol: ['none', 'arrow'],
      edgeSymbolSize: 6,
      symbol: ((value: EchartsDataPoint) => value.symbol) as any,
      symbolSize: 6,
      lineStyle: {
        color: this.dimensionColor(dimension)
      },
      links: links,
      data: echartPoints as any
    }
  }

  /**
   * Selects the correct series generator based on the number of points displayed:
   * If there are too many, the graph is not performant enough and a line graph will be drawn.
   * If the number is manageable, the graph type will be selected.
   */
  private selectAppropriateSeries(): 're-render' | 'unchanged' {
    const percentToAbsolute = (percent: number) =>
      (this.maxDateValue - this.minDateValue) * percent + this.minDateValue
    const startValue = percentToAbsolute(this.zoomStartPercent)
    const endValue = percentToAbsolute(this.zoomEndPercent)

    // TODO: Is this a performance problem? There might be 10.000+ items here
    // and this method is called every time the slider is dragged or the user
    // zooms using the mouse wheel
    let visibleDataPoints = 0
    for (const point of this.detailDataPoints) {
      if (
        point.authorDate.getTime() >= startValue &&
        point.authorDate.getTime() <= endValue
      ) {
        visibleDataPoints += this.dimensions.length
      }
    }

    const newGenerator: SeriesGenerationFunction =
      visibleDataPoints > 4 ? this.buildLineSeries : this.buildGraphSeries

    if (newGenerator !== this.seriesGenerator) {
      this.seriesGenerator = newGenerator
      return 're-render'
    }
    return 'unchanged'
  }
  // <!--</editor-fold>-->

  // <!--<editor-fold desc="LIFECYCLE HOOKS">-->
  mounted(): void {
    this.dimensions = [
      new Dimension('Random', 'metric', 'cats', 'LESS_IS_BETTER'),
      new Dimension('Random2', 'metric', 'cats', 'LESS_IS_BETTER')
    ]
    this.updateGraph()
  }
  // <!--</editor-fold>-->

  // <!--<editor-fold desc="REFERENCE LINE, COMPARE">-->
  private get commitToCompare(): DimensionDetailPoint | null {
    return vxm.detailGraphModule.commitToCompare
  }

  private get referenceDatapoint() {
    return vxm.detailGraphModule.referenceDatapoint
  }

  @Watch('referenceDatapoint')
  private updateReferenceDatapoint() {
    const series = this.chartOptions.series! as ValidEchartsSeries[]

    // noinspection JSMismatchedCollectionQueryUpdate
    let markLineData: { yAxis: number }[] = []
    if (this.referenceDatapoint !== null) {
      const reference = this.referenceDatapoint
      const referenceValue = reference.dataPoint.values.get(reference.dimension)
      if (typeof referenceValue === 'number') {
        markLineData = [{ yAxis: referenceValue }]
      }
    }
    const hasReferenceLine = markLineData.length > 0

    const markLineJson = {
      symbol: 'none',
      lineStyle: {
        type: 'dotted',
        width: 2
      },
      label: {
        show: true,
        formatter: () => {
          return 'Reference'
        }
      },
      silent: true,
      data: markLineData as any
    }

    // Set on one, delete on all (as the order might change)
    if (hasReferenceLine) {
      Vue.set(series[0], 'markLine', markLineJson)
    } else {
      series.forEach(it => Vue.set(it, 'markLine', markLineJson))
    }

    // Adjust padding so the label is not cut off on the right
    const grid = this.chartOptions.grid as EChartOption.Grid
    grid.right = hasReferenceLine ? 70 : 20
  }

  @Watch('commitToCompare')
  private updateCommitToCompare() {
    const series = this.chartOptions.series! as ValidEchartsSeries[]
    // noinspection JSMismatchedCollectionQueryUpdate
    const markPointData: any[] = []

    if (this.commitToCompare) {
      const point = this.commitToCompare.dataPoint

      markPointData.push({
        coord: [
          point.authorDate,
          point.values.get(this.commitToCompare.dimension)
        ],
        label: {
          show: true,
          position: 'inside',
          formatter: () => 'A'
        }
      })
    }

    const markPointJson = {
      silent: true,
      data: markPointData as any
    }

    // Set on one, delete on all (as the order might change)
    if (this.commitToCompare) {
      Vue.set(series[0], 'markPoint', markPointJson)
    } else {
      series.forEach(it => Vue.set(it, 'markPoint', markPointJson))
    }
  }
  // <!--</editor-fold>-->

  // <!--<editor-fold desc="DETAIL DIALOG EVENT HANDLER">-->
  private pointDialogExecuteCompare() {
    if (!vxm.detailGraphModule.commitToCompare || !this.pointDialogDatapoint) {
      return
    }
    const repoId = vxm.detailGraphModule.selectedRepoId
    const hashFrom = vxm.detailGraphModule.commitToCompare.dataPoint.hash
    const hashTo = this.pointDialogDatapoint.hash

    this.$router.push({
      name: 'run-comparison',
      params: { first: repoId, second: repoId },
      query: { hash1: hashFrom, hash2: hashTo }
    })
  }
  // <!--</editor-fold>-->

  // <!--<editor-fold desc="ECHARTS EVENT HANDLER">-->
  private echartsZoomed(e: any) {
    let event: {
      start?: number
      end?: number
      startValue?: number
      endValue?: number
    }
    if (!e.batch || e.batch.length === 0) {
      event = e
    } else {
      event = e.batch[0]
    }

    let startPercent: number
    let endPercent: number

    // Batch and un-batched events set either the percent or absolute value
    // we normalize to percentages
    if (event.start !== undefined && event.end !== undefined) {
      startPercent = event.start / 100
      endPercent = event.end / 100
    } else if (event.startValue !== undefined && event.endValue !== undefined) {
      startPercent = event.startValue / (this.maxDateValue - this.minDateValue)
      endPercent = event.endValue / (this.maxDateValue - this.minDateValue)
    } else {
      return
    }

    this.zoomStartPercent = startPercent
    this.zoomEndPercent = endPercent

    if (this.selectAppropriateSeries() === 're-render') {
      this.updateGraph()
    }
  }

  private echartsZoomReset() {
    this.zoomStartPercent = 0
    this.zoomEndPercent = 1
    this.updateGraph()
  }

  private echartsOpenContextMenu(e: any) {
    if (!e.data) {
      return
    }

    const echartsPoint = e.data as EchartsDataPoint

    const detailPoint = this.detailDataPoints.find(
      it => it.hash === echartsPoint.name
    )
    const dimension = this.dimensions[e.seriesIndex]

    if (!detailPoint || !dimension) {
      return
    }

    // Hide browser right click context menu
    if ((e as any).event && (e as any).event.event) {
      const event = (e as any).event.event as Event
      event.preventDefault()
    }

    this.pointDialogDatapoint = detailPoint
    this.pointDialogDimension = dimension
    this.pointDialogOpen = true
  }

  private echartsClicked(e: any) {
    if (e.data === undefined) {
      return
    }

    const echartsPoint = e.data as EchartsDataPoint

    if ((e as any).event && (e as any).event.event) {
      const event = (e as any).event.event as MouseEvent
      if (event.ctrlKey) {
        const routeData = this.$router.resolve({
          name: 'run-detail',
          params: {
            first: vxm.detailGraphModule.selectedRepoId,
            second: echartsPoint.name
          }
        })
        window.open(routeData.href, '_blank')
        return
      }
    }

    this.$router.push({
      name: 'run-detail',
      params: {
        first: vxm.detailGraphModule.selectedRepoId,
        second: echartsPoint.name
      }
    })
  }
  // <!--</editor-fold>-->

  // <!--<editor-fold desc="THEME HELPER">-->
  private get numberFormat(): Intl.NumberFormat {
    return new Intl.NumberFormat(
      new Intl.NumberFormat().resolvedOptions().locale,
      { maximumFractionDigits: 3 }
    )
  }
  private get graphBackgroundColor() {
    return this.$vuetify.theme.currentTheme.graphBackground as string
  }
  private dimensionColor(dimension: DimensionId) {
    return vxm.colorModule.colorByIndex(
      this.dimensions.findIndex(it => it.equals(dimension))
    )
  }
  private get graphFailedOrUnbenchmarkedColor() {
    return this.$vuetify.theme.currentTheme.graphFailedOrUnbenchmarked as string
  }
  // <!--</editor-fold>-->

  private readonly crossIcon =
    'path://M24 20.188l-8.315-8.209 8.2-8.282-3.697-3.697-8.212 8.318-8.31-8.203-3.666 3.666 8.321 8.24-8.206 8.313 3.666 3.666 8.237-8.318 8.285 8.203z'
}
</script>

<style scoped>
#chart-container {
  position: relative;
  height: 80vh;
}
</style>

<style>
/*noinspection CssUnusedSymbol*/
.echarts {
  width: 100%;
  height: 100%;
}

.echarts-tooltip-table tr td {
  padding: 2px;
}

.echarts-tooltip-table tr td:nth-child(2) {
  font-family: monospace;
}
.echarts-tooltip-table tr td:first-child {
  padding-right: 10px;
}
.echarts-tooltip-table tr td:only-child {
  font-weight: bold;
  padding-top: 1em;
  font-size: 1.1em;
}

.echarts-tooltip-table .color-preview {
  width: 10px;
  height: 10px;
  border-radius: 25%;
  display: inline-block;
}
</style>
