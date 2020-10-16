<template>
  <v-container fluid>
    <v-row align="center" justify="center">
      <v-col>
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

// eslint-disable-next-line no-undef
type RealOptions = dygraphs.Options & {
  rangeSelectorPlotLineWidth?: number
  rangeSelectorAlpha?: number
}

@Component({})
export default class DytailGraph extends Vue {
  @Prop()
  private dimensions!: Dimension[]

  @Prop({ default: true })
  private beginYAtZero!: boolean

  private graph!: Dygraph

  private height: number = 500

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

  private get selectorAlpha(): number {
    return vxm.userModule.darkThemeSelected ? 0.2 : 0.7
  }

  private get selectorLineWidth(): number {
    return vxm.userModule.darkThemeSelected ? 2 : 1.5
  }

  private get minDateValue(): number {
    const min = Math.min.apply(
      Math,
      this.datapoints.map(it => it.authorDate.getTime())
    )
    return min || 0
  }

  private get maxDateValue(): number {
    const max = Math.max.apply(
      Math,
      this.datapoints.map(it => it.authorDate.getTime())
    )
    return max || 0
  }

  private get numberFormat(): Intl.NumberFormat {
    return new Intl.NumberFormat(
      new Intl.NumberFormat().resolvedOptions().locale,
      { maximumFractionDigits: 3 }
    )
  }

  // eslint-disable-next-line no-undef
  private tooltipFormatter(legendData: dygraphs.LegendData) {
    const datapoint: DetailDataPoint | undefined = this.datapoints.find(
      point => point.authorDate.getTime() === legendData.x
    )
    if (datapoint) {
      const dimensionRows = legendData.series.map(val => {
        const dimension = val.labelHTML
        const color = val.color

        return `
                <tr>
                  <td>
                    <span class="color-preview" style="background-color: ${color}"></span>
                    ${dimension}
                  </td>
                  <td>${this.numberFormat.format(val.y)}</td>
                </tr>
                `
      })

      return `<table class="dygraphs-tooltip-table">
                  <tr>
                    <td>Hash</td>
                    <td>${datapoint ? datapoint.hash : 'xxx'}</td>
                  </tr>
                  </tr>
                    <td>Message</td>
                    <td>${datapoint ? datapoint.summary : 'xxx'}</td>
                  </tr>
                  <tr>
                    <td>Author</td>
                    <td>
                      ${datapoint ? datapoint.author : 'xxx'} at ${
        datapoint ? datapoint.authorDate.toLocaleString() : 'xxx'
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

  @Watch('datapoints')
  @Watch('dimensions')
  @Watch('beginYAtZero')
  @Watch('darkTheme')
  private up() {
    const data: number[][] = []

    // One array entry per datapoint. That array contains all values: [x, dim1, dim2, ...]
    for (let i = 0; i < this.datapoints.length; i++) {
      data[i] = [this.datapoints[i].authorDate.getTime()]
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
      labels: [
        'x',
        ...this.dimensions.map(it => it.benchmark + ' - ' + it.metric)
      ],
      colors: this.dimensionsColors(),
      dateWindow: [
        vxm.detailGraphModule.zoomXStartValue || this.minDateValue,
        vxm.detailGraphModule.zoomXEndValue || this.maxDateValue
      ],
      valueRange: [
        vxm.detailGraphModule.zoomYStartValue,
        vxm.detailGraphModule.zoomYEndValue
      ],
      includeZero: this.beginYAtZero,
      rangeSelectorPlotLineWidth: this.selectorLineWidth,
      rangeSelectorAlpha: this.selectorAlpha
    } as RealOptions)
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
            }
          },
          y: {
            axisLineColor: 'currentColor'
          }
        },
        ylabel: this.yLabel,
        legend: 'follow',
        labelsSeparateLines: true,
        connectSeparatedPoints: true,
        drawPoints: false,
        animatedZooms: true,
        panEdgeFraction: 0.00001,
        zoomCallback: this.dygraphsZoomed,
        legendFormatter: this.tooltipFormatter,
        crosshairColor: 'currentColor',
        plugins: [new Crosshair({ direction: 'vertical' })],
        showRangeSelector: true,
        rangeSelectorHeight: 30,
        rangeSelectorPlotLineWidth: this.selectorLineWidth,
        rangeSelectorAlpha: this.selectorAlpha,
        rangeSelectorForegroundLineWidth: 0.5,
        rangeSelectorPlotFillColor: '',
        rangeSelectorPlotStrokeColor: 'grey',
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

    if (this.graph.isZoomed('y')) {
      const [yZoomStart, yZoomEnd] = yRanges[0]
      vxm.detailGraphModule.zoomYStartValue = yZoomStart
      vxm.detailGraphModule.zoomYEndValue = yZoomEnd
    } else {
      vxm.detailGraphModule.zoomYStartValue = null
      vxm.detailGraphModule.zoomYEndValue = null
    }
  }
}
</script>

<style>
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
  border-width: 0px;
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

.dygraph-rangesel-fgcanvas {
  margin-top: 15px;
}

.dygraph-rangesel-bgcanvas {
  margin-top: 15px;
}

.dygraph-rangesel-zoomhandle {
  margin-top: 15px;
}

.dygraph-axis-label {
  color: currentColor;
}
</style>
