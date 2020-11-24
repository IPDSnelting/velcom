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
        <div id="chart" :style="{ height: '500px' }"></div>
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
import DetailDatapointDialog from '@/components/dialogs/DetailDatapointDialog.vue'

// eslint-disable-next-line no-undef
type RealOptions = dygraphs.Options & {
  rangeSelectorPlotLineWidth?: number
  rangeSelectorAlpha?: number
}

const doubleClickZoomOutPlugin = {
  activate: function (g: Dygraph) {
    const initialDateWindow = g.getOption('dateWindow')
    return {
      dblclick: (e: Event) => {
        ;(e as any).dygraph.updateOptions({
          dateWindow: initialDateWindow // don't show buffer
        })
        e.preventDefault()
      }
    }
  }
}

@Component({
  components: {
    'datapoint-dialog': DetailDatapointDialog
  }
})
export default class DytailGraph extends Vue {
  @Prop()
  private dimensions!: Dimension[]

  @Prop({ default: true })
  private beginYAtZero!: boolean

  @Prop({ default: true })
  private dayEquidistant!: boolean

  private graph!: Dygraph

  private height: number = 500
  private pointDialogDatapoint: DetailDataPoint | null = null
  private pointDialogDimension: Dimension | null = null
  private pointDialogOpen: boolean = false

  private get datapoints(): DetailDataPoint[] {
    return vxm.detailGraphModule.detailGraph
  }

  private firstSuccessful(dimension: DimensionId): number {
    for (const datapoint of this.datapoints) {
      const value = datapoint.values.get(dimension)
      if (typeof value === 'number') {
        return value
      }
    }
    return this.height / 2
  }

  private get yLabel(): string {
    if (this.dimensions.length === 1) {
      return this.dimensions[0].metric + ' in ' + this.dimensions[0].unit
    }
    return ''
  }

  private xAxisFormatter(d: Date, [start, end]: [number, number]): string {
    const dateString: string = d.getDate() + '-' + (d.getMonth() + 1)
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

  private get minDateValue(): number {
    const min = Math.min.apply(
      Math,
      this.datapoints.map(it => it.committerDate.getTime())
    )
    return min || 0
  }

  private get maxDateValue(): number {
    const max = Math.max.apply(
      Math,
      this.datapoints.map(it => it.committerDate.getTime())
    )
    return max || 0
  }

  private get initialLowerBound(): number {
    return Math.max(
      vxm.detailGraphModule.startTime.getTime(),
      this.minDateValue
    )
  }

  private get initialUpperBound(): number {
    return Math.min(vxm.detailGraphModule.endTime.getTime(), this.maxDateValue)
  }

  private get numberFormat(): Intl.NumberFormat {
    return new Intl.NumberFormat(
      new Intl.NumberFormat().resolvedOptions().locale,
      { maximumFractionDigits: 3 }
    )
  }

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

        return `
                <tr>
                  <td>
                    <span class="color-preview" style="background-color: ${color}"></span>
                    ${safeDimension}
                  </td>
                  <td>${this.numberFormat.format(val.y)}</td>
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

  // Used in the watcher for up()
  // noinspection JSUnusedLocalSymbols
  private get darkTheme() {
    return vxm.userModule.darkThemeSelected
  }

  private datapointByPositionDate(
    authorDate: number
  ): DetailDataPoint | undefined {
    return this.datapoints.find(
      point => point.positionDate.getTime() === authorDate
    )
  }

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

  @Watch('datapoints')
  @Watch('dimensions')
  @Watch('beginYAtZero')
  @Watch('darkTheme')
  @Watch('dayEquidistant')
  private up() {
    const data: number[][] = []

    // One array entry = #dimensions data points per commit
    // one array entry has the form [x-val, dim1, dim2, ...]
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
        vxm.detailGraphModule.zoomXStartValue || this.initialLowerBound,
        vxm.detailGraphModule.zoomXEndValue || this.initialUpperBound
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
            },
            ticker: function (min, max, pixels, opts, dygraph: Dygraph) {
              // now shut up, eslint
              return (Dygraph as any).getDateAxis(
                min,
                max,
                (Dygraph as any).Granularity.DAILY, // please, just shut up
                opts,
                dygraph
              )
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
        dateWindow: [this.initialLowerBound, this.initialUpperBound],
        legendFormatter: this.tooltipFormatter,
        crosshairColor: 'currentColor',
        plugins: [
          new Crosshair({ direction: 'vertical' }),
          doubleClickZoomOutPlugin
        ],
        showRangeSelector: true,
        rangeSelectorHeight: 30,
        rangeSelectorPlotLineWidth: this.selectorLineWidth,
        rangeSelectorAlpha: this.selectorAlpha,
        rangeSelectorForegroundLineWidth: 0.5,
        rangeSelectorPlotFillColor: '',
        rangeSelectorPlotStrokeColor: 'grey',
        drawHighlightPointCallback: this.drawHighlightPointCallback as any,
        highlightCircleSize: 5,
        rangeSelectorBackgroundStrokeColor: 'currentColor',
        // and, to keep the ability to brush and zoom:
        interactionModel: Dygraph.defaultInteractionModel
      } as RealOptions
    )

    this.up()
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
      vxm.detailGraphModule.zoomXStartValue = null
      vxm.detailGraphModule.zoomXEndValue = null
    }

    this.graph.xAxisExtremes()

    if (this.graph.isZoomed('y')) {
      const [yZoomStart, yZoomEnd] = yRanges[0]
      vxm.detailGraphModule.zoomYStartValue = yZoomStart
      vxm.detailGraphModule.zoomYEndValue = yZoomEnd
    } else {
      vxm.detailGraphModule.zoomYStartValue = null
      vxm.detailGraphModule.zoomYEndValue = null
    }
  }

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
}
</script>

<style>
/*noinspection CssUnusedSymbol*/
.dygraph-legend {
  position: absolute;
  width: auto;
  border-style: solid;
  white-space: nowrap;
  z-index: 9999999;
  transition: left 0.4s cubic-bezier(0.23, 1, 0.32, 1) 0s,
    top 0.4s cubic-bezier(0.23, 1, 0.32, 1) 0s;
  transition-duration: 1s;
  background-color: rgba(50, 50, 50, 0.7);
  border-width: 0;
  border-color: rgb(51, 51, 51);
  border-radius: 4px;
  color: rgb(255, 255, 255);
  font: 14px / 21px sans-serif;
  padding: 5px;
  left: 378px;
  top: -49px;
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
