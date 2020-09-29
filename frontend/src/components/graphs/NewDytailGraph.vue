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

@Component({})
export default class DytailGraph extends Vue {
  @Prop()
  dimensions!: Dimension[]

  @Prop({ default: true })
  beginYAtZero!: boolean

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
            axisLabelFormatter: function(x: number | Date) {
              return x instanceof Date || x % 1 === 0 ? x : ''
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
    vxm.detailGraphModule.zoomXStartValue = startX
    vxm.detailGraphModule.zoomXEndValue = endX

    const [yZoomStart, yZoomEnd] = yRanges[0]
    vxm.detailGraphModule.zoomYStartValue = yZoomStart
    vxm.detailGraphModule.zoomYEndValue = yZoomEnd
  }
}
</script>
