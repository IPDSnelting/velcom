<template>
  <v-container fluid class="mt-0 pt-0">
    <v-row align="center" justify="center">
      <v-col>
        <datapoint-dialog
          v-if="pointDialogDatapoint"
          :dialogOpen="pointDialogOpen"
          :selectedDatapoint="pointDialogDatapoint"
          :dimension="pointDialogDimension"
          @close="pointDialogOpen = false"
        ></datapoint-dialog>
        <div id="chart-container" @wheel.capture="$emit('wheel', $event)">
          <v-chart
            ref="chart"
            :autoresize="true"
            :options="chartOptions"
            :theme="chartTheme"
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
  Dimension,
  DimensionId,
  dimensionIdEqual
} from '@/store/types'
import { Prop, Watch } from 'vue-property-decorator'
import EChartsComp from 'vue-echarts'

import {
  LineChart,
  LineSeriesOption,
  GraphChart,
  GraphSeriesOption
} from 'echarts/charts'
import {
  GridComponent,
  GridComponentOption,
  TooltipComponent,
  TooltipComponentOption,
  LegendComponent,
  LegendComponentOption,
  DataZoomSliderComponent,
  DataZoomInsideComponent,
  DataZoomComponentOption,
  ToolboxComponent,
  ToolboxComponentOption,
  BrushComponent,
  BrushComponentOption,
  MarkLineComponent,
  MarkLineComponentOption,
  MarkPointComponent,
  MarkPointComponentOption
} from 'echarts/components'
import { use, ComposeOption } from 'echarts/core'
import { vxm } from '@/store'
import DetailDatapointDialog from '@/components/dialogs/DetailDatapointDialog.vue'
import { DimensionDetailPoint } from '@/store/modules/detailGraphStore'
import { formatDate } from '@/util/TimeUtil'
import { escapeHtml } from '@/util/TextUtils'
import { CustomKeyEqualsMap } from '@/util/CustomKeyEqualsMap'

use([
  LineChart,
  GraphChart,
  GridComponent,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  DataZoomInsideComponent,
  DataZoomSliderComponent,
  ToolboxComponent,
  BrushComponent,
  MarkLineComponent,
  MarkPointComponent
])

// A minimal types for option is useful for checking if any components are missing.
type ECOption = ComposeOption<
  | LineSeriesOption
  | GraphSeriesOption
  | GridComponentOption
  | LegendComponentOption
  | DataZoomComponentOption
  | ToolboxComponentOption
  | TooltipComponentOption
  | BrushComponentOption
  | MarkLineComponentOption
  | MarkPointComponentOption
>

type ValidEchartsSeries = LineSeriesOption | GraphSeriesOption
type SeriesGenerationFunction = (id: DimensionId) => ValidEchartsSeries

type BenchmarkStatus = 'success' | 'failed' | 'unbenchmarked'

const crossIcon =
  'path://M24 20.188l-8.315-8.209 8.2-8.282-3.697-3.697-8.212 8.318-8.31-8.203-3.666 3.666 8.321 8.24-8.206 8.313 3.666 3.666 8.237-8.318 8.285 8.203z'

class EchartsDataPoint {
  // convenience methods for accessing the value
  readonly time: Date
  readonly committerDate: Date
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
  readonly author: string
  readonly benchmarkStatus: BenchmarkStatus

  constructor(
    time: Date,
    committerDate: Date,
    dataValue: number,
    benchmarkStatus: BenchmarkStatus,
    name: string,
    color: string,
    borderColor: string,
    summary: string,
    author: string
  ) {
    this.time = time
    this.committerDate = committerDate
    this.dataValue = dataValue
    this.value = [this.time, this.dataValue]
    this.benchmarkStatus = benchmarkStatus
    this.name = name
    this.itemStyle = {
      color: color,
      borderColor: borderColor,
      borderWidth: 2
    }
    this.summary = summary
    this.author = author

    if (benchmarkStatus === 'success' || benchmarkStatus === 'unbenchmarked') {
      this.symbol = 'circle'
    } else {
      this.symbol = crossIcon
    }
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
  // noinspection JSMismatchedCollectionQueryUpdate
  @Prop()
  private dimensions!: Dimension[]

  @Prop({ default: false })
  private beginYAtZero!: boolean

  @Prop({ default: true })
  private dayEquidistant!: boolean
  // <!--</editor-fold>-->

  // <!--<editor-fold desc="FIELDS">-->
  private chartOptions: ECOption = {}
  private seriesGenerator: SeriesGenerationFunction = this.buildLineSeries

  // >>>> Datapoint Dialog >>>>
  private pointDialogOpen: boolean = false
  private pointDialogDatapoint: DetailDataPoint | null = null
  private pointDialogDimension: Dimension | null = null
  // <<<< Datapoint Dialog <<<<
  //  <!--</editor-fold>-->

  private get detailDataPoints(): DetailDataPoint[] {
    return vxm.detailGraphModule.detailGraph
  }

  // <!--<editor-fold desc="ECHARTS GRAPH OPTIONS">-->
  @Watch('detailDataPoints')
  @Watch('beginYAtZero')
  @Watch('dayEquidistant')
  @Watch('graphFailedOrUnbenchmarkedColor') // DataPoints need adjusting, they store the color
  private updateGraph() {
    console.log('Echarts updated')

    this.selectAppropriateSeries()

    this.chartOptions = {
      grid: {
        left: 20,
        right: 20,
        containLabel: true
      },
      xAxis: {
        type: 'time',
        min: vxm.detailGraphModule.startTime.getTime(),
        max: vxm.detailGraphModule.endTime.getTime(),
        axisLabel: {
          formatter: {
            year: '{MMM}\n{yyyy}',
            month: '{MMM}\n{yyyy}',
            day: '{dd}.{MM}.\n{yyyy}'
          }
        }
      },
      yAxis: {
        type: 'value',
        scale: !this.beginYAtZero
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
            }
          }
        },
        tooltip: {
          show: true
        }
      },
      dataZoom: [
        // DO NOT REORDER THE FIRST TWO OR echartsZoomed WILL BREAK!
        {
          id: 'x',
          xAxisIndex: [0],
          type: 'inside',
          // Start at the correct place when changing the series type
          startValue: vxm.detailGraphModule.zoomXStartValue || undefined,
          endValue: vxm.detailGraphModule.zoomXEndValue || undefined
        },
        {
          id: 'y',
          type: 'inside',
          yAxisIndex: [0],
          startValue: vxm.detailGraphModule.zoomYStartValue || undefined,
          endValue: vxm.detailGraphModule.zoomYEndValue || undefined,
          zoomOnMouseWheel: false
        },
        {
          id: 'xAxis',
          type: 'slider',
          // Start at the correct place when changing the series type
          startValue: vxm.detailGraphModule.zoomXStartValue || undefined,
          endValue: vxm.detailGraphModule.zoomXEndValue || undefined
        }
      ],
      tooltip: {
        trigger: 'axis',
        axisPointer: {},
        // TODO: Extract in own helper?
        formatter: this.tooltipFormatter,
        confine: true
      },
      series: this.dimensions.map(this.seriesGenerator)
    }

    this.updateReferenceDatapoint()
    this.updateMarkPoints()
  }

  // The correct type is not exposed sadly
  private tooltipFormatter(params: any) {
    const values = Array.isArray(params) ? params.slice() : [params]
    // Sort them so the order corresponds to the order of the lines
    values.sort((a, b) => {
      const first = a.data as EchartsDataPoint
      const second = b.data as EchartsDataPoint

      return second.dataValue - first.dataValue
    })

    const dimensionRows = values.map(val => {
      const dimension = this.dimensions[val.seriesIndex || 0]
      const color = this.dimensionColor(dimension)
      const datapoint = val.data as EchartsDataPoint
      const safeBenchmark = escapeHtml(dimension.benchmark)
      const safeMetric = escapeHtml(dimension.metric)

      let value: string
      if (datapoint.benchmarkStatus === 'success') {
        value = this.numberFormat.format(datapoint.dataValue)
      } else if (datapoint.benchmarkStatus === 'unbenchmarked') {
        value = 'Unbenchmarked'
      } else {
        value = 'Failed'
      }

      return `
                <tr>
                  <td>
                    <span class="color-preview" style="background-color: ${color}"></span>
                    ${safeBenchmark} - ${safeMetric}
                  </td>
                  <td>${value}</td>
                </tr>
                `
    })

    const samplePoint = values[0].data as EchartsDataPoint

    const committerDate = formatDate(samplePoint.committerDate)
    return `
                <table class="echarts-tooltip-table">
                  <tr>
                    <td>Hash</td>
                    <td>${escapeHtml(samplePoint.name)}</td>
                  </tr>
                  </tr>
                    <td>Message</td>
                    <td>${escapeHtml(samplePoint.summary)}</td>
                  </tr>
                  <tr>
                    <td>Author</td>
                    <td>
                      ${escapeHtml(samplePoint.author)} at ${committerDate}
                    </td>
                  </tr>
                  ${dimensionRows.join('\n')}
                </table>
            `
  }

  // <!--</editor-fold>-->

  // <!--<editor-fold desc="SERIES GENERATION">-->
  private findFirstSuccessful = (dimension: DimensionId) => {
    const point = this.detailDataPoints.find(it => it.successful(dimension))

    if (point) {
      return point.values.get(dimension) as number
    }
    return 0
  }

  private buildPointsForSingleDimension(dimension: DimensionId) {
    let lastSuccessfulValue: number = this.findFirstSuccessful(dimension)
    return this.detailDataPoints.map(point => {
      let benchmarkStatus: BenchmarkStatus = 'success'
      let color = this.dimensionColor(dimension)
      let borderColor = color

      let pointValue = point.values.get(dimension)
      if (typeof pointValue !== 'number') {
        pointValue = lastSuccessfulValue
      }
      lastSuccessfulValue = pointValue

      if (point.failed(dimension)) {
        // grey circle
        benchmarkStatus = 'failed'
        color = this.graphFailedOrUnbenchmarkedColor
        borderColor = color
      } else if (point.unbenchmarked(dimension)) {
        benchmarkStatus = 'unbenchmarked'
        // empty circle with outline
        color = this.graphBackgroundColor
        borderColor = this.graphFailedOrUnbenchmarkedColor
      }

      return new EchartsDataPoint(
        point.positionDate,
        point.committerDate,
        pointValue,
        benchmarkStatus,
        point.hash,
        color,
        borderColor,
        point.summary,
        point.author
      )
    })
  }

  private get echartsDataPoints(): Map<DimensionId, EchartsDataPoint[]> {
    const map: Map<DimensionId, EchartsDataPoint[]> = new CustomKeyEqualsMap(
      [],
      dimensionIdEqual
    )

    this.dimensions.forEach(dimension => {
      map.set(dimension, this.buildPointsForSingleDimension(dimension))
    })

    return map
  }

  private buildLineSeries(dimension: DimensionId): LineSeriesOption {
    // noinspection JSMismatchedCollectionQueryUpdate
    const echartPoints: EchartsDataPoint[] = this.echartsDataPoints.get(
      dimension
    )!

    return {
      type: 'line',
      showSymbol: false,
      symbol: ((value: EchartsDataPoint) => value.symbol) as any,
      symbolSize: 6,
      lineStyle: {
        color: this.dimensionColor(dimension)
      },
      data: echartPoints as any
    }
  }

  private buildGraphSeries(dimension: DimensionId): GraphSeriesOption {
    // noinspection JSMismatchedCollectionQueryUpdate
    const echartPoints: EchartsDataPoint[] = this.echartsDataPoints.get(
      dimension
    )!
    const links = this.detailDataPoints.flatMap(point => {
      return point.parents.map(parent => ({
        source: point.hash,
        target: parent
      }))
    })

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
    const visibleDataPoints = vxm.detailGraphModule.visiblePoints
    const newGenerator: SeriesGenerationFunction =
      visibleDataPoints > 200 ? this.buildLineSeries : this.buildGraphSeries

    if (newGenerator !== this.seriesGenerator) {
      this.seriesGenerator = newGenerator
      return 're-render'
    }
    return 'unchanged'
  }
  // <!--</editor-fold>-->

  // <!--<editor-fold desc="LIFECYCLE HOOKS">-->
  mounted(): void {
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

  private get showReferenceMarkers() {
    if (!this.referenceDatapoint) {
      return false
    }
    const dimensions = this.referenceDatapoint.dimension

    return this.dimensions.find(it => it.equals(dimensions))
  }

  private get showCommitToCompareMarker() {
    if (!this.commitToCompare) {
      return false
    }
    const dimensions = this.commitToCompare.dimension

    return this.dimensions.find(it => it.equals(dimensions))
  }

  @Watch('referenceDatapoint')
  @Watch('commitToCompare')
  private updateReferenceDatapoint() {
    const series = this.chartOptions.series! as ValidEchartsSeries[]

    // noinspection JSMismatchedCollectionQueryUpdate
    const markLineData: any[] = []
    if (this.showReferenceMarkers) {
      const reference = this.referenceDatapoint!
      const referenceValue = reference.dataPoint.values.get(reference.dimension)
      if (typeof referenceValue === 'number') {
        markLineData.push({ yAxis: referenceValue, name: 'Reference' })
      }
    }
    if (this.showCommitToCompareMarker) {
      // Day equidistant points might move the point and its author date
      const displayedPoint = this.echartsDataPoints
        .get(this.commitToCompare!.dimension)!
        .find(it => it.name === this.commitToCompare!.dataPoint.hash)

      if (displayedPoint) {
        markLineData.push({
          xAxis: displayedPoint.time,
          name: 'Comparing…'
        })
      }
    }
    const hasReferenceLine = markLineData.length > 0

    const markLineJson = {
      symbol: 'none',
      lineStyle: {
        type: 'dotted',
        color: this.themeColor('error'),
        width: 2
      },
      label: {
        show: true,
        color: this.themeColor('error'),
        formatter: (it: any) => {
          return it.name as string
        }
      },
      silent: true,
      data: markLineData as any
    }

    // Set on one, delete on all (as the order might change)
    if (hasReferenceLine && series.length > 0) {
      Vue.set(series[0], 'markLine', markLineJson)
    } else {
      series.forEach(it => Vue.set(it, 'markLine', markLineJson))
    }

    // Adjust padding so the label is not cut off on the right
    const grid = this.chartOptions.grid as GridComponentOption
    grid.right = hasReferenceLine ? 70 : 20
  }

  @Watch('referenceDatapoint')
  private updateMarkPoints() {
    const series = this.chartOptions.series! as ValidEchartsSeries[]
    // noinspection JSMismatchedCollectionQueryUpdate
    const markPointData: any[] = []

    if (this.showReferenceMarkers) {
      const point = this.referenceDatapoint!.dataPoint
      const displayedPoint = this.echartsDataPoints
        .get(this.referenceDatapoint!.dimension)!
        .find(it => it.name === point.hash)

      markPointData.push({
        coord: [
          displayedPoint!.time,
          point.values.get(this.referenceDatapoint!.dimension)
        ],
        label: {
          show: true,
          position: 'inside',
          formatter: () => 'R'
        }
      })
    }

    const markPointJson = {
      silent: true,
      data: markPointData as any
    }

    // Set on one, delete on all (as the order might change)
    if (this.commitToCompare && series.length > 0) {
      Vue.set(series[0], 'markPoint', markPointJson)
    } else {
      series.forEach(it => Vue.set(it, 'markPoint', markPointJson))
    }
  }
  // <!--</editor-fold>-->

  // <!--<editor-fold desc="ECHARTS EVENT HANDLER">-->
  private echartsZoomed(e: any) {
    if (!e.batch || e.batch.length === 0) {
      this.setZoomOnCorrectAxis(e.dataZoomId)
    } else {
      e.batch.forEach((batch: any) => {
        this.setZoomOnCorrectAxis(batch.dataZoomId)
      })
    }

    if (this.selectAppropriateSeries() === 're-render') {
      this.updateGraph()
    }
  }

  private setZoomOnCorrectAxis(seriesId: string) {
    const actualOptions: ECOption = (this.$refs['chart'] as any).computedOptions

    const orNull = (zoom: DataZoomComponentOption, start: 'start' | 'end') => {
      const value =
        start === 'start'
          ? (zoom.startValue as number | undefined | null)
          : (zoom.endValue as number | undefined | null)
      if (value === null || value === undefined) {
        return null
      }

      return value
    }

    const dataZooms = actualOptions.dataZoom as DataZoomComponentOption[]

    if (seriesId === 'x' || seriesId.includes('xAxis')) {
      vxm.detailGraphModule.zoomXStartValue = orNull(dataZooms[0], 'start')
      vxm.detailGraphModule.zoomXEndValue = orNull(dataZooms[0], 'end')
    } else {
      vxm.detailGraphModule.zoomYStartValue = orNull(dataZooms[1], 'start')
      vxm.detailGraphModule.zoomYEndValue = orNull(dataZooms[1], 'end')
    }
  }

  private echartsZoomReset() {
    vxm.detailGraphModule.zoomXStartValue = vxm.detailGraphModule.startTime.getTime()
    vxm.detailGraphModule.zoomXEndValue = vxm.detailGraphModule.endTime.getTime()
    vxm.detailGraphModule.zoomYStartValue = null
    vxm.detailGraphModule.zoomYEndValue = null
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
      vxm.detailGraphModule.colorIndex(dimension)!
    )
  }

  private get graphFailedOrUnbenchmarkedColor() {
    return this.$vuetify.theme.currentTheme.graphFailedOrUnbenchmarked as string
  }

  private get themeColor(): (key: string) => string {
    return key => this.$vuetify.theme.currentTheme[key] as string
  }

  private get chartTheme() {
    const axisSettings = () => ({
      axisLine: {
        lineStyle: {
          color: 'currentColor'
        }
      },
      axisTick: {
        lineStyle: {
          color: 'currentColor'
        }
      },
      axisLabel: {
        textStyle: {
          color: 'currentColor'
        }
      },
      splitLine: {
        lineStyle: {
          color: this.themeColor('rowHighlight')
        }
      },
      splitArea: {
        areaStyle: {
          color: this.themeColor('rowHighlight')
        }
      }
    })
    return {
      backgroundColor: this.graphBackgroundColor,
      valueAxis: axisSettings(),
      timeAxis: axisSettings(),
      dataZoom: {
        textStyle: {
          color: 'currentColor'
        }
      },
      toolbox: {
        iconStyle: {
          normal: {
            borderColor: 'currentColor'
          }
        }
      }
    }
  }
  // <!--</editor-fold>-->
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

/*noinspection CssUnusedSymbol*/
.echarts-tooltip-table .color-preview {
  width: 10px;
  height: 10px;
  border-radius: 25%;
  display: inline-block;
}
</style>
