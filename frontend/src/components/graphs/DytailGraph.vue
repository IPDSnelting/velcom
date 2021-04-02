<template>
  <v-container fluid style="height: 100%" class="mx-0 px-0">
    <v-row
      align="center"
      justify="center"
      style="height: 100%"
      class="mx-0 px-0"
    >
      <v-col style="height: 100%" class="mx-0 px-0">
        <div
          id="chart"
          @wheel="$emit('wheel', $event)"
          style="height: 100%; width: 100%"
        ></div>
        <div id="ranger" :style="{ height: '30px' }"></div>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop, Watch } from 'vue-property-decorator'
import Dygraph from 'dygraphs'
import {
  AttributedDatapoint,
  DetailDataPoint,
  GraphDataPoint,
  SeriesId,
  SeriesInformation
} from '@/store/types'
import { vxm } from '@/store'
import 'dygraphs/css/dygraph.css'
import Crosshair from 'dygraphs/src/extras/crosshair.js'
import { escapeHtml } from '@/util/TextUtils'
import GraphDatapointDialog from '@/components/dialogs/GraphDatapointDialog.vue'
import { formatDate } from '@/util/TimeUtil'

// eslint-disable-next-line no-undef
type RealOptions = dygraphs.Options & {
  rangeSelectorPlotLineWidth?: number
  rangeSelectorAlpha?: number
}

@Component({
  components: {
    'datapoint-dialog': GraphDatapointDialog
  }
})
export default class DytailGraph extends Vue {
  // <!--<editor-fold desc="PROPS">-->
  @Prop({ default: false })
  private beginYAtZero!: boolean

  @Prop()
  private zoomXStartValue!: number | null

  @Prop()
  private zoomXEndValue!: number | null

  @Prop()
  private zoomYStartValue!: number | null

  @Prop()
  private zoomYEndValue!: number | null

  @Prop()
  private dataRangeMin!: Date

  @Prop()
  private dataRangeMax!: Date

  @Prop()
  private datapoints!: GraphDataPoint[]

  @Prop()
  private seriesInformation!: SeriesInformation[]

  @Prop()
  private visiblePointCount!: number

  @Prop({ default: null })
  private commitToCompare!: AttributedDatapoint | null

  @Prop({ default: null })
  private referenceDatapoint!: AttributedDatapoint | null

  @Prop({ default: 0 })
  private refreshKey!: number
  // <!--</editor-fold>-->

  // <!--<editor-fold desc="FIELDS">-->
  private graph!: Dygraph

  // >>>> Datapoint Dialog >>>>
  private pointDialogDatapoint: DetailDataPoint | null = null
  private pointDialogSeries: SeriesInformation | null = null
  private pointDialogOpen: boolean = false
  // <<<< Datapoint Dialog <<<<
  //  <!--</editor-fold>-->

  // <!--<editor-fold desc="DATA POINTS">-->
  /**
   * The only possibility one has to find the (abstract) data point for a given
   * point in the graph is to go by x axis position ( = the committer date).
   * This is not guaranteed to give the correct data point, but it's accurate
   * enough to display a fitting tooltip
   *
   * @param authorDate: date on which to search for a data point
   * @private
   */
  private datapointByPositionDate(
    authorDate: number
  ): DetailDataPoint | undefined {
    return this.datapoints.find(
      point => point.positionTime.getTime() === authorDate
    ) as DetailDataPoint | undefined
  }

  /**
   * Returns the first DataPoint that contains a (successful) benchmark for a given
   * dimension, therefore providing a value by which it can be placed in the graph
   *
   * @param series: the ID of the series of interest
   * @private
   */
  private firstSuccessful(series: SeriesId): number {
    for (const datapoint of this.datapoints) {
      const value = datapoint.values.get(series)
      if (typeof value === 'number') {
        return value
      }
    }
    return 0
  }

  //  <!--</editor-fold>-->

  // <!--<editor-fold desc="LIFECYCLE HOOKS">-->
  /**
   * initialize graph with all options, but without data
   */
  mounted(): void {
    window.addEventListener('resize', this.onResize)

    // empty string because div is not expected to be NULL
    const chartDiv = document.getElementById('chart') || ''

    this.graph = new Dygraph(
      // containing div
      chartDiv,

      [[1, 1]],

      {
        axes: {
          x: {
            axisLineColor: 'currentColor',
            axisLabelFormatter: (
              x: number | Date,
              granularity: number,
              opts: (name: string) => any,
              dygraph: Dygraph
            ) => {
              const [start, end] = dygraph.xAxisRange()
              if (x instanceof Date) {
                return this.xAxisFormatter(x, [start, end])
              }
              return x % 1 === 0
                ? this.xAxisFormatter(new Date(x), [start, end])
                : ''
            }
          },
          y: {
            axisLineColor: 'currentColor'
          }
        },
        ylabel: '',
        pointClickCallback: this.pointClickCallback,
        labels: ['', ''], // will be replaced in update
        legend: 'follow',
        labelsSeparateLines: true,
        connectSeparatedPoints: true,
        drawPoints: false,
        animatedZooms: false, // does not work with slider
        pointSize: 3,
        panEdgeFraction: 0.5,
        zoomCallback: this.dygraphsZoomed,
        legendFormatter: this.tooltipFormatter,
        crosshairColor: 'currentColor',
        showRangeSelector: true,
        rangeSelectorHeight: 30,
        rangeSelectorPlotLineWidth: this.selectorLineWidth,
        rangeSelectorAlpha: this.selectorAlpha,
        rangeSelectorForegroundLineWidth: 0.5,
        rangeSelectorPlotFillColor: '',
        rangeSelectorPlotStrokeColor: 'grey',
        rangeSelectorBackgroundStrokeColor: 'currentColor',
        plugins: [new Crosshair({ direction: 'vertical' })],
        drawHighlightPointCallback: this.drawHighlightPointCallback as any,
        highlightCircleSize: 5,
        // and, to keep the ability to brush and zoom:
        interactionModel: this.fancyPanningInteractions
      } as RealOptions
    )

    this.update()
  }

  private beforeDestroy() {
    window.removeEventListener('resize', this.onResize)
  }

  private onResize() {
    window.requestAnimationFrame(() => {
      this.graph.resize()
    })
  }

  @Watch('datapoints')
  @Watch('dimensions')
  @Watch('beginYAtZero')
  @Watch('darkTheme')
  @Watch('refreshKey')
  private update() {
    const data: number[][] = []

    // One array entry = #dimensions data points per commit
    // each array entry has the form [x-val, dim1, dim2, ...]
    for (let i = 0; i < this.datapoints.length; i++) {
      data[i] = [this.datapoints[i].positionTime.getTime()]
    }

    for (const series of this.seriesInformation) {
      let lastValue = this.firstSuccessful(series.id)
      this.datapoints.forEach((point, index) => {
        let pointValue = point.values.get(series.id)
        if (typeof pointValue !== 'number') {
          pointValue = lastValue
        }
        lastValue = pointValue

        data[index].push(pointValue)
      })
    }

    this.graph.updateOptions({
      file: data,
      labels: ['x', ...this.seriesInformation.map(it => escapeHtml(it.id))],
      colors: this.seriesInformation.map(it => it.color),
      dateWindow: [
        this.zoomXStartValue || this.dataRangeMin.getTime(),
        this.zoomXEndValue || this.dataRangeMax.getTime()
      ],
      valueRange: [this.zoomYStartValue, this.zoomYEndValue],
      includeZero: this.beginYAtZero,
      rangeSelectorPlotLineWidth: this.selectorLineWidth,
      rangeSelectorAlpha: this.selectorAlpha
    } as RealOptions)

    this.drawMarkers()
  }
  //  <!--</editor-fold>-->

  // <!--<editor-fold desc="FORMATTERS">-->

  /**
   * corrects numbers to three decimal places
   */
  private get numberFormat(): Intl.NumberFormat {
    return new Intl.NumberFormat(
      new Intl.NumberFormat().resolvedOptions().locale,
      { maximumFractionDigits: 3 }
    )
  }

  /**
   * formats the date that determines the x axis position of a data point, depending on the length
   * of the timespan currently displayed on the x axis:
   * no more than two days -> hh:mm \n mm-dd
   * otherwise             -> mm-dd \n yyyy
   * @param d: date to format
   * @param start: start value of x axis (required by dygraph)
   * @param end: end value of x axis (required by dygraph)
   * @private
   */
  private xAxisFormatter(d: Date, [start, end]: [number, number]): string {
    const dateString: string = d.getMonth() + 1 + '-' + d.getDate()
    const hourString: string | number =
      d.getHours().toString().length === 1 ? '0' + d.getHours() : d.getHours()
    const minuteString: string | number =
      d.getMinutes().toString().length === 1
        ? '0' + d.getMinutes()
        : d.getMinutes()

    const daysShown: number = Math.floor((end - start) / (1000 * 60 * 60 * 24))
    if (daysShown < 2) {
      return hourString + ':' + minuteString + '\n' + dateString
    }
    return dateString + '\n' + d.getFullYear()
  }

  /**
   * dygraphs doesn't provide tooltips, but it does provide a legend which can be used as
   * tooltips. For each data point, the legend shows additional info like the hash
   * of the corresponding commit, the author and (if there is a successful benchmark) the
   * benchmarked values
   *
   * @param legendData: the legend to format
   * @private
   */
  // eslint-disable-next-line no-undef
  private tooltipFormatter(legendData: dygraphs.LegendData) {
    const datapoint = this.datapointByPositionDate(legendData.x)
    if (datapoint) {
      const data = legendData.series.slice()
      // Sort them so the order corresponds to the order of the lines
      data.sort((a, b) => b.y - a.y)

      const seriesRows = data.map(val => {
        const color = val.color
        const series = this.seriesInformation.find(
          it => escapeHtml(it.id.toString()) === val.labelHTML
        )
        let safeSeriesId = 'N/A'
        if (series !== undefined) {
          safeSeriesId = escapeHtml(series.displayName)
        }

        let value: string = this.numberFormat.format(val.y)
        if (series && datapoint.commitUnbenchmarked(series.id)) {
          value = 'Commit was not benchmarked'
        } else if (series && datapoint.metricNotBenchmarked(series.id)) {
          value = 'Metric not measured for commit'
        } else if (series && !datapoint.successful(series.id)) {
          value = 'Failed'
        }

        return `
                <tr>
                  <td>
                    <span class="color-preview" style="background-color: ${color}"></span>
                    ${safeSeriesId}
                  </td>
                  <td>${value}</td>
                </tr>
                `
      })

      return `<table class="dygraphs-tooltip-table">
                  <tr>
                    <td>Hash</td>
                    <td>${escapeHtml(datapoint.hash)}</td>
                  </tr>
                  </tr>
                    <td>Message</td>
                    <td>${escapeHtml(datapoint.summary)}</td>
                  </tr>
                  <tr>
                    <td>Author</td>
                    <td>
                      ${escapeHtml(datapoint.author)} at
                      ${formatDate(datapoint.committerTime)}
                    </td>
                  </tr>
                 ${seriesRows.join('\n')}
              </table>
            `
    }
    return "something went wrong :(\n couldn't find commit"
  }

  //  <!--</editor-fold>-->

  // <!--<editor-fold desc="COLORS AND THEMES">-->

  // Used in the watcher for update()
  // noinspection JSUnusedLocalSymbols
  private get darkTheme() {
    return vxm.userModule.darkThemeSelected
  }

  private get graphFailedOrUnbenchmarkedColor() {
    return this.$vuetify.theme.currentTheme.graphFailedOrUnbenchmarked as string
  }

  private get selectorAlpha(): number {
    return vxm.userModule.darkThemeSelected ? 0.2 : 0.7
  }

  private get selectorLineWidth(): number {
    return vxm.userModule.darkThemeSelected ? 2 : 1.5
  }

  //  <!--</editor-fold>-->

  // <!--<editor-fold desc="DATA POINT DISPLAY">-->

  /**
   * style the data points in the graph depending on wether they were successfully
   * benchmarked or not:
   * failed benchmark for given dimension     -> gray cross icon
   * no benchmark for given dimension         -> grey empty circle
   * successful benchmark for given dimension -> filled in circle in dimension color
   *
   * @param g: the graph
   * @param seriesName: the name of the given dimension
   * @param canvasContext
   * @param cx: x axis position
   * @param cy: y axis position
   * @param color: dimension color
   * @param pointSize
   * @param idx: index in the data point series
   * @private
   */
  private drawHighlightPointCallback(
    g: Dygraph,
    seriesName: string,
    canvasContext: CanvasRenderingContext2D,
    cx: number,
    cy: number,
    color: string,
    pointSize: number,
    idx: number
  ) {
    const datapoint: DetailDataPoint = this.datapoints[idx] as DetailDataPoint
    const series = this.seriesInformation.find(
      it => it.id.toString() === seriesName
    )
    if (!series) {
      return
    }

    canvasContext.strokeStyle = color

    if (datapoint.failed(series.id)) {
      // gray cross icon
      canvasContext.beginPath()
      canvasContext.strokeStyle = this.graphFailedOrUnbenchmarkedColor
      canvasContext.lineWidth = 4
      canvasContext.moveTo(cx - pointSize, cy - pointSize)
      canvasContext.lineTo(cx + pointSize, cy + pointSize)
      canvasContext.stroke()

      canvasContext.moveTo(cx + pointSize, cy - pointSize)
      canvasContext.lineTo(cx - pointSize, cy + pointSize)
      canvasContext.stroke()
      canvasContext.closePath()
    } else if (datapoint.unbenchmarked(series.id)) {
      // grey empty circle
      canvasContext.beginPath()
      canvasContext.lineWidth = 2
      canvasContext.strokeStyle = this.graphFailedOrUnbenchmarkedColor
      canvasContext.arc(cx, cy, pointSize, 0, 360)
      canvasContext.stroke()
      canvasContext.closePath()
    } else {
      // filled in circle in dimension color
      canvasContext.beginPath()
      canvasContext.lineWidth = 2
      canvasContext.arc(cx, cy, pointSize, 0, 360)
      canvasContext.stroke()
      canvasContext.fill()
      canvasContext.closePath()
    }
  }

  /**
   * draw a little flag to mark the reference data point, if specified and in the
   * current timespan
   */
  private drawMarkers() {
    const annotations = []
    if (vxm.detailGraphModule.referenceDatapoint) {
      const { seriesId, datapoint } = vxm.detailGraphModule.referenceDatapoint
      annotations.push({
        series: seriesId,
        x: datapoint.positionTime.getTime(),
        shortText: 'R',
        text: 'Reference datapoint',
        width: 20,
        height: 20,
        tickHeight: 20
      })
    }

    this.graph.setAnnotations(annotations)
  }

  //  <!--</editor-fold>-->

  // <!--<editor-fold desc="CUSTOM GRAPH INTERACTIONS">-->

  /**
   * what to do when interacting with a point in the graph:
   * left click: navigate to corresponding commit detail page
   * right click: open data point dialog
   *
   * @param e: mouse event
   * @param graphPoint: the point that was clicked
   * @private
   */
  private pointClickCallback(e: MouseEvent, graphPoint: any) {
    const datapoint: DetailDataPoint | undefined = graphPoint.xval
      ? this.datapointByPositionDate(graphPoint.xval)
      : undefined

    if (!datapoint) {
      return
    }

    // Datapoint dialog on right click
    if (e.button === 2) {
      this.pointDialogDatapoint = datapoint
      this.pointDialogSeries = this.seriesInformation.find(
        it => it.id.toString() === graphPoint.name
      )!
      this.pointDialogOpen = true

      return
    }

    this.$emit('click', {
      datapoint: datapoint,
      ctrl: e.ctrlKey
    })
  }

  /**
   * the default dygraph interactions, but altered to load new data if panning
   * exceeded the graph boundaries
   *
   * @private
   */
  private fancyPanningInteractions = Object.assign(
    {},
    Dygraph.defaultInteractionModel,
    {
      mouseup: (event: MouseEvent, g: any, context: any) => {
        if (context.isPanning) {
          ;(Dygraph as any).endPan(event, g, context)
          this.dygraphsPanned()
        }
      }
    }
  )

  /**
   * determine if panning exceeded the graph boundaries and fetch new data if needed
   *
   * @private
   */
  private dygraphsPanned() {
    if (this.dataRangeMax.getTime() < this.graph.xAxisRange()[1]) {
      this.$emit('update:dataRangeMax', new Date(this.graph.xAxisRange()[1]))
      this.$emit('update:zoomXEndValue', this.graph.xAxisRange()[1])
    } else if (this.dataRangeMin.getTime() > this.graph.xAxisRange()[0]) {
      this.$emit('update:dataRangeMin', new Date(this.graph.xAxisRange()[0]))
      this.$emit('update:zoomXStartValue', this.graph.xAxisRange()[0])
    }
  }

  private dygraphsZoomed(
    startX: number,
    endX: number,
    yRanges: [number, number][]
  ) {
    if (this.graph.isZoomed('x')) {
      this.$emit('update:zoomXStartValue', startX)
      this.$emit('update:zoomXEndValue', endX)
    } else {
      this.$emit('update:zoomXStartValue', this.dataRangeMin.getTime())
      this.$emit('update:zoomXEndValue', this.dataRangeMax.getTime())
    }

    if (this.graph.isZoomed('y')) {
      const [yZoomStart, yZoomEnd] = yRanges[0]
      this.$emit('update:zoomYStartValue', yZoomStart)
      this.$emit('update:zoomYEndValue', yZoomEnd)
    } else {
      this.$emit('update:zoomYStartValue', null)
      this.$emit('update:zoomYEndValue', null)
    }
  }

  // <!--</editor-fold>-->
}
</script>

<style>
/*noinspection CssUnusedSymbol*/
.dygraph-legend {
  position: absolute;
  display: block;
  width: auto;
  border-style: solid;
  white-space: nowrap;
  z-index: 9999999;
  box-shadow: rgba(0, 0, 0, 0.2) 1px 2px 10px;
  transition: opacity 0.2s cubic-bezier(0.23, 1, 0.32, 1) 0s,
    visibility 0.2s cubic-bezier(0.23, 1, 0.32, 1) 0s;
  background-color: rgb(255, 255, 255);
  border-width: 1px;
  border-radius: 4px;
  color: rgb(102, 102, 102);
  font: 14px / 21px sans-serif;
  padding: 10px;
  border-color: rgb(255, 255, 255);
  pointer-events: none;
}

.dygraphs-tooltip-table tr td {
  padding: 2px;
}

.dygraphs-tooltip-table tr td:nth-child(2) {
  font-family: monospace;
}
.dygraphs-tooltip-table tr td:first-child {
  padding-right: 10px;
}
.dygraphs-tooltip-table tr td:only-child {
  font-weight: bold;
  padding-top: 1em;
  font-size: 1.1em;
}

/*noinspection CssUnusedSymbol*/
.dygraphs-tooltip-table .color-preview {
  width: 10px;
  height: 10px;
  border-radius: 25%;
  display: inline-block;
}

/*noinspection CssUnusedSymbol*/
.dygraph-rangesel-fgcanvas {
  margin-top: 15px;
}

/*noinspection CssUnusedSymbol*/
.dygraph-rangesel-bgcanvas {
  margin-top: 15px;
}

/*noinspection CssUnusedSymbol*/
.dygraph-rangesel-zoomhandle {
  margin-top: 15px;
}

/*noinspection CssUnusedSymbol*/
.dygraph-axis-label {
  color: currentColor;
}
</style>
