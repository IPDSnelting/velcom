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
          @compare-commits="pointDialogExecuteCompare"
        ></datapoint-dialog>
        <div id="chart-container">
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
import { DetailDataPoint, Dimension, DimensionId } from '@/store/types'
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
import { formatDate } from '@/util/TimeUtil'

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
  readonly author: string

  constructor(
    time: Date,
    dataValue: number,
    symbol: string,
    name: string,
    color: string,
    borderColor: string,
    summary: string,
    author: string
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
    this.author = author
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
  private chartOptions: EChartOption = {}
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

  // <!--<editor-fold desc="ECHARTS GRAPH OPTIONS">-->
  @Watch('detailDataPoints')
  @Watch('beginYAtZero')
  @Watch('graphFailedOrUnbenchmarkedColor') // DataPoints need adjusting, they store the color
  private updateGraph() {
    console.log('UPDATED')

    this.selectAppropriateSeries()

    this.chartOptions = {
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
        formatter: this.tooltipFormatter
      },
      series: this.dimensions.map(this.seriesGenerator)
    }

    this.updateReferenceDatapoint()
    this.updateMarkPoints()
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
                  <tr>
                    <td>Author</td>
                    <td>
                      ${samplePoint.author} at ${formatDate(samplePoint.time)}
                    </td>
                  </tr>
                  ${dimensionRows.join('\n')}
                </table>
            `
  }

  // <!--</editor-fold>-->

  // <!--<editor-fold desc="SERIES GENERATION">-->

  // https://stackoverflow.com/questions/14446511/most-efficient-method-to-groupby-on-an-array-of-objects
  private groupBy<K, V>(list: K[], keyGetter: (key: K) => V) {
    const map = new Map()
    list.forEach(item => {
      const key = keyGetter(item)
      const collection = map.get(key)
      if (!collection) {
        map.set(key, [item])
      } else {
        collection.push(item)
      }
    })
    return map
  }

  // <!--<editor-fold desc="SERIES GENERATION">-->
  private findFirstSuccessful = (dimension: DimensionId) => {
    const point = this.detailDataPoints.find(it => it.successful(dimension))

    if (point) {
      return point.values.get(dimension) as number
    }
    return 0
  }

  private buildDayEquidistantPointsForSingle(dimension: DimensionId) {
    let lastSuccessfulValue: number = this.findFirstSuccessful(dimension)

    const millisInDay = 1000 * 60 * 60 * 24

    const dayGroups: Map<number, DetailDataPoint[]> = this.groupBy(
      this.detailDataPoints,
      key =>
        // round to day
        Math.floor(key.authorDate.getTime() / millisInDay) * millisInDay
    )

    return Array.from(dayGroups.entries()).flatMap(([day, points]) => {
      const spacingBetweenElementsMillis = millisInDay / points.length

      return points.map((point, index) => {
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
          new Date(day + spacingBetweenElementsMillis * index),
          pointValue,
          symbol,
          point.hash,
          color,
          borderColor,
          point.summary,
          point.author
        )
      })
    })
  }

  private buildNormalPointsForSingle(dimension: DimensionId) {
    let lastSuccessfulValue: number = this.findFirstSuccessful(dimension)
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
        point.summary,
        point.author
      )
    })
  }

  private get echartsDataPoints(): Map<DimensionId, EchartsDataPoint[]> {
    const map: Map<DimensionId, EchartsDataPoint[]> = new Map()

    console.time('complete setup')
    console.log(this.dimensions)
    this.dimensions.forEach(dimension => {
      if (this.dayEquidistant) {
        map.set(dimension, this.buildDayEquidistantPointsForSingle(dimension))
      } else {
        map.set(dimension, this.buildNormalPointsForSingle(dimension))
      }
    })
    console.log('\n\n===\n\n')
    console.timeEnd('complete setup')

    return map
  }

  private buildLineSeries(dimension: DimensionId): EChartOption.SeriesLine {
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
    if (this.commitToCompare !== null) {
      markLineData.push({
        xAxis: this.commitToCompare.dataPoint.authorDate,
        name: 'Comparing…'
      })
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
    const grid = this.chartOptions.grid as EChartOption.Grid
    grid.right = hasReferenceLine ? 70 : 20
  }

  @Watch('referenceDatapoint')
  private updateMarkPoints() {
    const series = this.chartOptions.series! as ValidEchartsSeries[]
    // noinspection JSMismatchedCollectionQueryUpdate
    const markPointData: any[] = []

    if (this.showReferenceMarkers) {
      const point = this.referenceDatapoint!.dataPoint

      markPointData.push({
        coord: [
          point.authorDate,
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
    const actualOptions: EChartOption = (this.$refs['chart'] as any)
      .computedOptions

    const orNull = (zoom: EChartOption.DataZoom, start: 'start' | 'end') => {
      const value =
        start === 'start'
          ? (zoom.startValue as number | undefined | null)
          : (zoom.endValue as number | undefined | null)
      if (value === null || value === undefined) {
        return null
      }

      // We are fully zoomed out ==> set that to null
      const zoomPercent = start === 'start' ? zoom.start : zoom.end
      if (zoomPercent === 0 || zoomPercent === 100) {
        return null
      }

      return value
    }

    if (seriesId === 'x' || seriesId.includes('xAxis')) {
      vxm.detailGraphModule.zoomXStartValue = orNull(
        actualOptions.dataZoom![0],
        'start'
      )
      vxm.detailGraphModule.zoomXEndValue = orNull(
        actualOptions.dataZoom![0],
        'end'
      )
    } else {
      vxm.detailGraphModule.zoomYStartValue = orNull(
        actualOptions.dataZoom![1],
        'start'
      )
      vxm.detailGraphModule.zoomYEndValue = orNull(
        actualOptions.dataZoom![1],
        'end'
      )
    }
  }

  private echartsZoomReset() {
    vxm.detailGraphModule.zoomXStartValue = this.minDateValue
    vxm.detailGraphModule.zoomXEndValue = this.maxDateValue
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

/*noinspection CssUnusedSymbol*/
.echarts-tooltip-table .color-preview {
  width: 10px;
  height: 10px;
  border-radius: 25%;
  display: inline-block;
}
</style>
