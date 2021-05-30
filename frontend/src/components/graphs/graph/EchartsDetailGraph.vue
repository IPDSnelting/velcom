<template>
  <v-container fluid class="mt-0 pt-0 full-height">
    <v-row align="center" justify="center" class="full-height" no-gutters>
      <v-col class="full-height">
        <slot
          name="dialog"
          v-if="pointDialogDatapoint"
          :dialog-open="pointDialogOpen"
          :selected-datapoint="pointDialogDatapoint"
          :series-information="pointDialogSeries"
          :closeDialog="() => (pointDialogOpen = false)"
        ></slot>
        <div
          id="chart-container"
          @wheel.capture="$emit('wheel', $event)"
          class="full-height"
        >
          <v-chart
            ref="chart"
            :autoresize="true"
            :options="chartOptions"
            :theme="chartTheme"
            @datazoom="echartsZoomed"
            @contextmenu="echartsOpenContextMenu"
            @click="echartsClicked"
            @restore="$emit('reset-zoom')"
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
  AttributedDatapoint,
  GraphDataPoint,
  SeriesId,
  SeriesInformation
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
import { escapeHtml } from '@/util/Texts'
import { formatDate } from '@/util/Times'

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
type SeriesGenerationFunction = (
  seriesInformation: SeriesInformation
) => ValidEchartsSeries

type BenchmarkStatus = 'success' | 'failed' | 'no-such-metric' | 'unbenchmarked'

const crossIcon =
  'path://M24 20.188l-8.315-8.209 8.2-8.282-3.697-3.697-8.212 8.318-8.31-8.203-3.666 3.666 8.321 8.24-8.206 8.313 3.666 3.666 8.237-8.318 8.285 8.203z'

/*
FIXME: Delete this
Echarts needs:
  1. X-axis value (time)
  2. **Y-axis value** (based on the *series*)
  3. A symbol / benchmark status
  4. A unique name used by the graph series
  5. A color
  6. Enough for a **tooltip** (author / summary right now)

Echarts datapoints are grouped by dimension. We could introduce a more general "Series[Key]" type for that.

That could then also tell you how to extract a y value and color.

Tooltips are too custom, they'd need to be done differently. Inject a tooltip formatter as property?

We need some shared state with 2-way binding for the dialog and graph that should be persisted:
  - Reference datapoint (series key, value)
  - Comparison datapoint (series key, name)
  - Zoom levels (X / Y axis)
 */

class EchartsDataPoint {
  // convenience methods for accessing the value
  readonly positionTime: Date
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

  readonly benchmarkStatus: BenchmarkStatus

  constructor(
    positionTime: Date,
    dataValue: number,
    benchmarkStatus: BenchmarkStatus,
    name: string,
    color: string,
    borderColor: string
  ) {
    this.positionTime = positionTime
    this.dataValue = dataValue
    this.value = [this.positionTime, this.dataValue]
    this.benchmarkStatus = benchmarkStatus
    this.name = name
    this.itemStyle = {
      color: color,
      borderColor: borderColor,
      borderWidth: 2
    }

    if (benchmarkStatus === 'failed') {
      this.symbol = crossIcon
    } else {
      this.symbol = 'circle'
    }
  }
}

@Component({
  components: {
    'v-chart': EChartsComp
  }
})
export default class EchartsDetailGraph extends Vue {
  // <!--<editor-fold desc="PROPS">-->
  @Prop({ default: false })
  private readonly beginYAtZero!: boolean

  @Prop()
  private readonly zoomXStartValue!: number | null

  @Prop()
  private readonly zoomXEndValue!: number | null

  @Prop()
  private readonly zoomYStartValue!: number | null

  @Prop()
  private readonly zoomYEndValue!: number | null

  @Prop()
  private readonly dataRangeMin!: Date

  @Prop()
  private readonly dataRangeMax!: Date

  @Prop()
  private readonly datapoints!: GraphDataPoint[]

  @Prop()
  private readonly seriesInformation!: SeriesInformation[]

  @Prop()
  private readonly visiblePointCount!: number

  @Prop({ default: null })
  private readonly commitToCompare!: AttributedDatapoint | null

  @Prop({ default: null })
  private readonly referenceDatapoint!: AttributedDatapoint | null

  @Prop({ default: 0 })
  private readonly refreshKey!: number
  // <!--</editor-fold>-->

  // <!--<editor-fold desc="FIELDS">-->
  private chartOptions: ECOption = {}
  private seriesGenerator: SeriesGenerationFunction = this.buildLineSeries

  // >>>> Datapoint Dialog >>>>
  private pointDialogOpen: boolean = false
  private pointDialogDatapoint: GraphDataPoint | null = null
  private pointDialogSeries: SeriesInformation | null = null
  // <<<< Datapoint Dialog <<<<
  //  <!--</editor-fold>-->

  // <!--<editor-fold desc="ECHARTS GRAPH OPTIONS">-->
  @Watch('datapoints')
  @Watch('beginYAtZero')
  @Watch('dataRangeMin')
  @Watch('dataRangeMax')
  @Watch('refreshKey')
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
        min: this.dataRangeMin.getTime(),
        max: this.dataRangeMax.getTime(),
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
          startValue: this.zoomXStartValue || undefined,
          endValue: this.zoomXEndValue || undefined
        },
        {
          id: 'y',
          type: 'inside',
          yAxisIndex: [0],
          startValue: this.zoomYStartValue || undefined,
          endValue: this.zoomYEndValue || undefined,
          zoomOnMouseWheel: false
        },
        {
          id: 'xAxis',
          type: 'slider',
          // Start at the correct place when changing the series type
          startValue: this.zoomXStartValue || undefined,
          endValue: this.zoomXEndValue || undefined
        }
      ],
      tooltip: {
        trigger: 'axis',
        axisPointer: {},
        // TODO: Extract in own helper?
        formatter: this.tooltipFormatter,
        confine: false,
        position: this.tooltipPositioner
      },
      series: this.seriesInformation.map(this.seriesGenerator)
    }

    this.updateReferenceDatapoint()
    this.updateMarkPoints()
  }

  @Watch('visibleDataPoints')
  private onVisibleDatapointsChange() {
    if (this.selectAppropriateSeries() === 're-render') {
      this.updateGraph()
    }
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
    const seriesRows = values.map(val => {
      const seriesInformation = this.seriesInformation[val.seriesIndex || 0]
      const color = seriesInformation.color
      const datapoint = val.data as EchartsDataPoint
      const safeDisplayName = escapeHtml(seriesInformation.displayName)
      let value: string
      if (datapoint.benchmarkStatus === 'success') {
        value = this.numberFormat.format(datapoint.dataValue)
      } else if (datapoint.benchmarkStatus === 'unbenchmarked') {
        value = 'Commit was not benchmarked'
      } else if (datapoint.benchmarkStatus === 'no-such-metric') {
        value = 'Metric not measured for commit'
      } else {
        value = 'Failed'
      }
      return `
                <tr>
                  <td>
                    <span class="color-preview" style="background-color: ${color}"></span>
                    ${safeDisplayName}
                  </td>
                  <td>${value}</td>
                </tr>
                `
    })
    const samplePoint = values[0].data as EchartsDataPoint
    const point = this.datapoints.find(it => it.uid === samplePoint.name)

    if (!point) {
      return 'No point found :/'
    }

    const committerDate = formatDate(point.committerTime)
    return `
           <table class="echarts-tooltip-table">
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
             ${seriesRows.join('\n')}
           </table>
            `
  }

  private tooltipPositioner(point: [number, number], _: unknown, el: unknown) {
    if (!(el instanceof HTMLDivElement)) {
      // We use HTML tooltips so this code path is not reached.
      throw new Error(
        'Only HTML tooltips are supported in our custom tooltip position!'
      )
    }

    // The mouse X and Y position in "point" is relative to the chart. We approximate the chart position using
    // the chart-container, so convert the relative mouse X and Y to screen coordinates.
    // This is needed as our later processing steps try to position the tooltip *on the screen* (and not just
    // inside the chart), and therefore need to be able to refer to the actual screen corners
    const containerRect = document
      .getElementById('chart-container')!
      .getBoundingClientRect()

    // Placement is based on the size of the tooltip
    const tooltipRect = el.getBoundingClientRect()
    const tooltipWidth = tooltipRect.width
    const tooltipHeight = tooltipRect.height

    type Point = { x: number; y: number }
    const dist = (a: Point, b: Point) =>
      Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2)

    // We use an adapted version of
    // https://www.fabiofranchino.com/blog/efficient-tooltip-positioning-in-d3js-chart/
    // To do this we need the four corners that frame the area our tooltip can be placed in.
    // We choose to use the following
    //
    //                    Screen
    //          +-------------------------+
    //          |                         |
    //          |         Graph           |
    // Top left X      -------------      X  Top Right
    //          |      -------------      |
    //          |      -------------      |
    //          |                         |
    // Bot left X-------------------------X Bot right

    const mouseScreenPosition = {
      x: point[0] + containerRect.x,
      y: point[1] + containerRect.y
    }
    const topLeft = { x: 0, y: containerRect.y }
    const topRight = { x: window.innerWidth, y: containerRect.y }
    const bottomLeft = { x: 0, y: window.innerHeight }
    const bottomRight = { x: window.innerWidth, y: window.innerHeight }

    const distanceTopLeft = dist(mouseScreenPosition, topLeft)
    const distanceTopRight = dist(mouseScreenPosition, topRight)
    const distanceBottomLeft = dist(mouseScreenPosition, bottomLeft)
    const distanceBottomRight = dist(mouseScreenPosition, bottomRight)

    // The algorithm works by selecting the corner that is *the furthest* away from the current mouse position
    //
    //           Allowed tooltip positions
    //          +--------------------------+  Would Pick this corner, as it is the furthest away
    //          |        -----------------/|
    //          |       /                  |
    //          |  ---x------------------\ |
    //          | /                       \|
    //          +--------------------------+
    //
    // And then it tries to position the tooltip along the line connecting the point to the corner.
    const maxDist = Math.max(
      distanceTopLeft,
      distanceTopRight,
      distanceBottomLeft,
      distanceBottomRight
    )

    let finalX
    let finalY
    const padding = 15

    //
    // +-----------=----------+
    // |         / = XXX-\    |  M is mouse, X is a possible tooltip location, line is a graph
    // |  /--\  /  = XXX M\   |  Only horizontal position is relevant here
    // | -    --   = XXX   \- |
    // +-----------=----------+
    //             ^
    // If we are on the right, we try to place the tooltip on the left, so the user can freely see the data on the
    // right.
    if (maxDist === distanceTopLeft || maxDist === distanceBottomLeft) {
      finalX = point[0] - padding - tooltipWidth
    } else {
      finalX = point[0] + padding
    }

    // If we are in the top half, we just try to place the tooltip below us. This will likely be fine, as there
    // are some controls under the graph and we can more easily overflow at the bottom:
    //   The data is sorted by value in the tooltip, so if the users hover somewhere at the top of the graph, we
    //   do want to show the top of the tooltip - cutting off the bottom is likely okay, as the users mouse is far
    //   away from any relevant data.
    if (!(maxDist === distanceTopLeft || maxDist === distanceTopRight)) {
      finalY = point[1] + padding
    } else {
      // If we are in the bottom half, we try to do the same. Place the tooltip below us. This prevents the
      // tooltip from jumping around, if we move through the graph.
      finalY = point[1] + padding

      // However, if we place the tooltip below and hover at the bottom, it might go offscreen - even when there
      // is plenty space above!
      // If we detect that the tooltip is at least partially hidden, we instead try to place the tooltip
      // *ABOVE* the mouse cursor.

      // Does the tooltip at least partially go off-screen (i.e. is larger than the bottom y)?
      if (finalY + containerRect.y + tooltipHeight > bottomLeft.y) {
        // It did go off-screen, so we try to lift it above instead.

        // Does the tooltip fit above our mouse cursor?
        // finalY + containerRect.y translates our planned top left corner to screen coordinates. We then
        // subtract the tooltipHeight (as we want to place the *bottom* corner) at mouse position.
        // If this is smaller than the top y, we clipped outside our top border and there isn't enough space.
        // The padding is deliberately ignored in the calculation, as there are some controls above the chart
        // (toolbox) and the tooltip has some information at the top we are willing to sacrifice. Ignoring the
        // padding here just makes it a bit more likely that the tooltip fits above, while not harming readability
        // too much.
        if (finalY + containerRect.y - tooltipHeight >= topLeft.y) {
          // It fit above! We now adjust the finalY to place the bottom of the tooltip <padding> above our mouse's
          // y position.
          finalY = point[1] - padding - tooltipHeight
        } else {
          // It didn't fit above. The tooltip is apparently too tall and we can't place it *anywhere* without
          // clipping it. This is sad, but we will at least try to salvage it a bit.
          // One of our priorities was clearing the horizontal line of your mouse cursor, so you can easily click
          // on points and their neighbours. This property is nice, but not essential.
          // Therefore, we give it up and instead place the tooltip below the mouse cursor - but lifted up
          // enough to be fully visible. It might now extend below and above the mouse y position, but as it is
          // also offset in the x-direction, clicking datapoints is still possible.
          finalY -= finalY + containerRect.y + tooltipHeight - bottomLeft.y
        }
      }
    }

    return [finalX, finalY]
  }
  // <!--</editor-fold>-->

  // <!--<editor-fold desc="SERIES GENERATION">-->
  private findFirstSuccessful = (seriesId: SeriesId) => {
    const point = this.datapoints.find(it => it.successful(seriesId))

    if (point) {
      return point.values.get(seriesId) as number
    }
    return 0
  }

  private buildPointsForSeries(seriesInformation: SeriesInformation) {
    const seriesId = seriesInformation.id
    let lastSuccessfulValue: number = this.findFirstSuccessful(seriesId)
    return this.datapoints
      .filter(it => it.values.has(seriesId))
      .map(point => {
        let benchmarkStatus: BenchmarkStatus = 'success'
        let color = seriesInformation.color
        let borderColor = color

        let pointValue = point.values.get(seriesId)
        if (typeof pointValue !== 'number') {
          pointValue = lastSuccessfulValue
        }
        lastSuccessfulValue = pointValue

        if (point.failed(seriesId)) {
          // grey circle
          benchmarkStatus = 'failed'
          color = this.graphFailedOrUnbenchmarkedColor
          borderColor = color
        } else if (point.unbenchmarked(seriesId)) {
          benchmarkStatus = point.commitUnbenchmarked(seriesId)
            ? 'unbenchmarked'
            : 'no-such-metric'
          // empty circle with outline
          color = this.graphBackgroundColor
          borderColor = this.graphFailedOrUnbenchmarkedColor
        }

        return new EchartsDataPoint(
          point.positionTime,
          pointValue,
          benchmarkStatus,
          point.uid,
          color,
          borderColor
        )
      })
  }

  private get echartsDataPoints(): Map<SeriesId, EchartsDataPoint[]> {
    const map: Map<SeriesId, EchartsDataPoint[]> = new Map([])

    this.seriesInformation.forEach(series => {
      map.set(series.id, this.buildPointsForSeries(series))
    })

    return map
  }

  private buildLineSeries(series: SeriesInformation): LineSeriesOption {
    // noinspection JSMismatchedCollectionQueryUpdate
    const echartPoints: EchartsDataPoint[] = this.echartsDataPoints.get(
      series.id
    )!

    return {
      type: 'line',
      showSymbol: false,
      symbol: ((value: EchartsDataPoint) => value.symbol) as any,
      symbolSize: 6,
      lineStyle: {
        color: series.color
      },
      data: echartPoints as any
    }
  }

  private buildGraphSeries(series: SeriesInformation): GraphSeriesOption {
    // noinspection JSMismatchedCollectionQueryUpdate
    const echartPoints: EchartsDataPoint[] = this.echartsDataPoints.get(
      series.id
    )!
    const links = this.datapoints.flatMap(point => {
      return point.parentUids.map(parentUid => ({
        source: point.uid,
        target: parentUid
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
        color: series.color
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
    const visibleDataPoints = this.visiblePointCount
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
  private get showReferenceMarkers() {
    if (!this.referenceDatapoint) {
      return false
    }
    const seriesId = this.referenceDatapoint.seriesId

    return this.seriesInformation.find(it => it.id === seriesId)
  }

  private get showCommitToCompareMarker() {
    if (!this.commitToCompare) {
      return false
    }
    const seriesId = this.commitToCompare.seriesId

    return this.seriesInformation.find(it => it.id === seriesId)
  }

  @Watch('referenceDatapoint')
  @Watch('commitToCompare')
  private updateReferenceDatapoint() {
    const series = this.chartOptions.series! as ValidEchartsSeries[]

    // noinspection JSMismatchedCollectionQueryUpdate
    const markLineData: any[] = []
    if (this.showReferenceMarkers) {
      const reference = this.referenceDatapoint!
      const referenceValue = reference.datapoint.values.get(reference.seriesId)
      if (typeof referenceValue === 'number') {
        markLineData.push({ yAxis: referenceValue, name: 'Reference' })
      }
    }
    if (this.showCommitToCompareMarker) {
      // Day equidistant points might move the point and its author date
      const displayedPoint = this.echartsDataPoints
        .get(this.commitToCompare!.seriesId)!
        .find(it => it.name === this.commitToCompare!.datapoint.uid)

      if (displayedPoint) {
        markLineData.push({
          xAxis: displayedPoint.positionTime,
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
      const point = this.referenceDatapoint!.datapoint
      const displayedPoint = this.echartsDataPoints
        .get(this.referenceDatapoint!.seriesId)!
        .find(it => it.name === point.uid)

      if (displayedPoint) {
        markPointData.push({
          coord: [
            displayedPoint.positionTime,
            point.values.get(this.referenceDatapoint!.seriesId)
          ],
          label: {
            show: true,
            position: 'inside',
            formatter: () => 'R'
          }
        })
      }
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
      this.$emit('update:zoomXStartValue', orNull(dataZooms[0], 'start'))
      this.$emit('update:zoomXEndValue', orNull(dataZooms[0], 'end'))
    } else {
      this.$emit('update:zoomYStartValue', orNull(dataZooms[1], 'start'))
      this.$emit('update:zoomYEndValue', orNull(dataZooms[1], 'end'))
    }
  }

  private echartsOpenContextMenu(e: any) {
    if (!e.data) {
      return
    }

    const echartsPoint = e.data as EchartsDataPoint

    const datapoint = this.datapoints.find(it => it.uid === echartsPoint.name)
    const seriesInformation = this.seriesInformation[e.seriesIndex]

    if (!datapoint || !seriesInformation) {
      return
    }

    // Hide browser right click context menu
    if ((e as any).event && (e as any).event.event) {
      const event = (e as any).event.event as Event
      event.preventDefault()
    }

    this.pointDialogDatapoint = datapoint
    this.pointDialogSeries = seriesInformation
    this.pointDialogOpen = true
  }

  private echartsClicked(e: any) {
    if (e.data === undefined) {
      return
    }

    // Was a *link* (i.e. the arrow between two datapoints) and no datapoint
    if (e.data.name === undefined) {
      return
    }

    const echartsPoint = e.data as EchartsDataPoint
    const datapoint = this.datapoints.find(it => it.uid === echartsPoint.name)

    // Should not happen, but better be safe than sorry
    if (!datapoint) {
      return
    }

    if ((e as any).event && (e as any).event.event) {
      const event = (e as any).event.event as MouseEvent
      if (event.ctrlKey) {
        const routeData = this.$router.resolve({
          name: 'run-detail',
          params: {
            first: datapoint.repoId,
            second: datapoint.hash
          }
        })
        window.open(routeData.href, '_blank')
        return
      }
    }
    this.$router.push({
      name: 'run-detail',
      params: {
        first: datapoint.repoId,
        second: datapoint.hash
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
        color: 'currentColor'
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
          borderColor: 'currentColor'
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
}
.full-height {
  height: 100%;
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
