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
        ></datapoint-dialog>
        <div
          id="chart"
          :style="{ height: '500px' }"
          @wheel="$emit('wheel', $event)"
        ></div>
        <div id="ranger" :style="{ height: '500px', height: '30px' }"></div>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop, Watch } from 'vue-property-decorator'
import Dygraph from 'dygraphs'
import { DetailDataPoint, Dimension, DimensionId } from '@/store/types'
import { vxm } from '@/store'
import 'dygraphs/css/dygraph.css'
import Crosshair from 'dygraphs/src/extras/crosshair.js'
import { escapeHtml } from '@/util/TextUtils'
import { formatDate } from '@/util/TimeUtil'
import { debounce, defaultWaitTime } from '@/util/Debouncer.ts'
import DetailDatapointDialog from '@/components/dialogs/DetailDatapointDialog.vue'

// eslint-disable-next-line no-undef
type RealOptions = dygraphs.Options & {
  rangeSelectorPlotLineWidth?: number
  rangeSelectorAlpha?: number
}

@Component({
  components: {
    'datapoint-dialog': DetailDatapointDialog
  }
})
export default class DytailGraph extends Vue {
  // <!--<editor-fold desc="PROPS">-->
  @Prop()
  private dimensions!: Dimension[]

  @Prop({ default: true })
  private beginYAtZero!: boolean

  @Prop({ default: true })
  private dayEquidistant!: boolean
  // <!--</editor-fold>-->

  // <!--<editor-fold desc="FIELDS">-->
  private graph!: Dygraph
  private height: number = 500

  // >>>> Datapoint Dialog >>>>
  private pointDialogDatapoint: DetailDataPoint | null = null
  private pointDialogDimension: Dimension | null = null
  private pointDialogOpen: boolean = false
  // <<<< Datapoint Dialog <<<<
  //  <!--</editor-fold>-->

  // <!--<editor-fold desc="DATA POINTS">-->
  private get datapoints(): DetailDataPoint[] {
    return vxm.detailGraphModule.detailGraph
  }

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
      point => point.positionDate.getTime() === authorDate
    )
  }

  /**
   * Returns the first DataPoint that contains a (successful) benchmark for a given
   * dimension, therefore providing a value by which it can be placed in the graph
   *
   * @param dimension: the ID of the dimension of interest
   * @private
   */
  private firstSuccessful(dimension: DimensionId): number {
    for (const datapoint of this.datapoints) {
      const value = datapoint.values.get(dimension)
      if (typeof value === 'number') {
        return value
      }
    }
    return this.height / 2
  }

  //  <!--</editor-fold>-->

  // <!--<editor-fold desc="LIFECYCLE HOOKS">-->
  /**
   * initialize graph with all options, but without data
   */
  mounted(): void {
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
        ylabel: this.yLabel,
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

  @Watch('datapoints')
  @Watch('dimensions')
  @Watch('beginYAtZero')
  @Watch('darkTheme')
  @Watch('dayEquidistant')
  private update() {
    const data: number[][] = []

    // One array entry = #dimensions data points per commit
    // each array entry has the form [x-val, dim1, dim2, ...]
    for (let i = 0; i < this.datapoints.length; i++) {
      data[i] = [this.datapoints[i].positionDate.getTime()]
    }

    for (const dimension of this.dimensions) {
      let lastValue = this.firstSuccessful(dimension)
      this.datapoints.forEach((point, index) => {
        let pointValue = point.values.get(dimension)
        if (typeof pointValue !== 'number') {
          pointValue = lastValue
        }
        lastValue = pointValue

        data[index].push(pointValue)
      })
    }

    this.graph.updateOptions({
      file: data,
      labels: ['x', ...this.dimensions.map(it => escapeHtml(it.toString()))],
      colors: this.dimensionsColors(),
      dateWindow: [
        vxm.detailGraphModule.zoomXStartValue ||
          vxm.detailGraphModule.startTime.getTime(),
        vxm.detailGraphModule.zoomXEndValue ||
          vxm.detailGraphModule.endTime.getTime()
      ],
      valueRange: [
        vxm.detailGraphModule.zoomYStartValue,
        vxm.detailGraphModule.zoomYEndValue
      ],
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
   * if only the values for one metric are displayed in the graph, the
   * y axis is labeled
   */
  private get yLabel(): string {
    if (this.dimensions.length === 1) {
      return this.dimensions[0].metric + ' in ' + this.dimensions[0].unit
    }
    return ''
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

      const dimensionRows = data.map(val => {
        const safeDimension = escapeHtml(val.labelHTML)
        const color = val.color
        const dimension = this.dimensions.find(
          it => escapeHtml(it.toString()) === val.labelHTML
        )

        let value: string = this.numberFormat.format(val.y)
        if (dimension && datapoint.unbenchmarked(dimension)) {
          value = 'Unbenchmarked'
        } else if (dimension && !datapoint.successful(dimension)) {
          value = 'Failed'
        }

        return `
                <tr>
                  <td>
                    <span class="color-preview" style="background-color: ${color}"></span>
                    ${safeDimension}
                  </td>
                  <td>${value}</td>
                </tr>
                `
      })

      return `<table class="dygraphs-tooltip-table">
                  <tr>
                    <td>Hash</td>
                    <td>${datapoint ? escapeHtml(datapoint.hash) : 'xxx'}</td>
                  </tr>
                  </tr>
                    <td>Message</td>
                    <td>${
                      datapoint ? escapeHtml(datapoint.summary) : 'xxx'
                    }</td>
                  </tr>
                  <tr>
                    <td>Author</td>
                    <td>
                      ${datapoint ? escapeHtml(datapoint.author) : 'xxx'} at ${
        datapoint ? formatDate(datapoint.committerDate) : 'xxx'
      }
                    </td>
                  </tr>
                 ${dimensionRows.join('\n')}
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

  private dimensionColor(dimension: DimensionId) {
    return vxm.colorModule.colorByIndex(
      vxm.detailGraphModule.colorIndex(dimension)!
    )
  }

  private dimensionsColors(): string[] {
    return this.dimensions.map(this.dimensionColor)
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
    const datapoint: DetailDataPoint = this.datapoints[idx]
    const dimension: DimensionId | undefined = this.dimensions.find(
      it => it.benchmark + ' - ' + it.metric === seriesName
    )
    if (!dimension) {
      return
    }

    canvasContext.strokeStyle = color

    if (datapoint.failed(dimension)) {
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
    } else if (datapoint.unbenchmarked(dimension)) {
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
      const { dimension, dataPoint } = vxm.detailGraphModule.referenceDatapoint
      annotations.push({
        series: dimension.toString(),
        x: dataPoint.committerDate.getTime(),
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
      this.pointDialogDimension = this.dimensions.find(
        it => it.toString() === graphPoint.name
      )!
      this.pointDialogOpen = true

      return
    }

    // New tab on control
    if (e.ctrlKey) {
      const routeData = this.$router.resolve({
        name: 'run-detail',
        params: {
          first: vxm.detailGraphModule.selectedRepoId,
          second: datapoint.hash
        }
      })
      window.open(routeData.href, '_blank')
    } else {
      // open it in place on a normal left click
      this.$router.push({
        name: 'run-detail',
        params: {
          first: vxm.detailGraphModule.selectedRepoId,
          second: datapoint.hash
        }
      })
    }
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
    if (vxm.detailGraphModule.endTime.getTime() < this.graph.xAxisRange()[1]) {
      debounce(() => {
        vxm.detailGraphModule.fetchDetailGraph()
        vxm.detailGraphModule.endTime = new Date(this.graph.xAxisRange()[1])
        vxm.detailGraphModule.zoomXEndValue = this.graph.xAxisRange()[1]
      }, defaultWaitTime)()
    } else if (
      vxm.detailGraphModule.startTime.getTime() > this.graph.xAxisRange()[0]
    ) {
      debounce(() => {
        vxm.detailGraphModule.fetchDetailGraph()
        vxm.detailGraphModule.startTime = new Date(this.graph.xAxisRange()[0])
        vxm.detailGraphModule.zoomXStartValue = this.graph.xAxisRange()[0]
      }, defaultWaitTime)()
    }
  }

  private dygraphsZoomed(
    startX: number,
    endX: number,
    yRanges: [number, number][]
  ) {
    if (this.graph.isZoomed('x')) {
      vxm.detailGraphModule.zoomXStartValue = startX
      vxm.detailGraphModule.zoomXEndValue = endX
    } else {
      vxm.detailGraphModule.zoomXStartValue = vxm.detailGraphModule.startTime.getTime()
      vxm.detailGraphModule.zoomXEndValue = vxm.detailGraphModule.endTime.getTime()
    }

    if (this.graph.isZoomed('y')) {
      const [yZoomStart, yZoomEnd] = yRanges[0]
      vxm.detailGraphModule.zoomYStartValue = yZoomStart
      vxm.detailGraphModule.zoomYEndValue = yZoomEnd
    } else {
      vxm.detailGraphModule.zoomYStartValue = null
      vxm.detailGraphModule.zoomYEndValue = null
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
