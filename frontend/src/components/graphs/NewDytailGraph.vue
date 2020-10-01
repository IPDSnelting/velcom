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
import 'dygraphs/dist/dygraph.css'
import { formatDate } from '@/util/TimeUtil'

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

    let daysShown: number = Math.floor((end - start) / (1000 * 60 * 60 * 24))
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

  @Watch('datapoints')
  @Watch('dimensions')
  @Watch('beginYAtZero')
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
      includeZero: this.beginYAtZero
    })
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
          }
        },
        ylabel: this.yLabel,
        legend: 'follow',
        labelsSeparateLines: true,
        connectSeparatedPoints: true,
        drawPoints: false,
        animatedZooms: true,
        panEdgeFraction: 0.00001,
        zoomCallback: this.dygraphsZoomed
      }
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
