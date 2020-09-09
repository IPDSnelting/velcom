<template>
  <v-container fluid>
    <v-row align="center" justify="center">
      <v-col>
        <div id="chart-container">
          <v-chart :autoresize="true" :options="chartOptions" />
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
import { Prop } from 'vue-property-decorator'
import EChartsComp from 'vue-echarts'

import 'echarts/lib/chart/line'
import 'echarts/lib/chart/graph'
import 'echarts/lib/component/polar'
import 'echarts/lib/component/tooltip'
import 'echarts/lib/component/legend'
import 'echarts/lib/component/title'
import 'echarts/lib/component/dataZoomSlider'
import 'echarts/lib/component/dataZoom'
import 'echarts/lib/component/dataZoomInside'
import 'echarts/lib/component/toolbox'
import 'echarts/lib/component/brush'
import 'echarts/lib/component/markLine'
import 'echarts/lib/component/markPoint'

class EchartsDataPoint {
  readonly time: Date
  readonly dataValue: number
  readonly symbol: string
  readonly value: [Date, number]

  constructor(time: Date, dataValue: number, symbol: string) {
    this.time = time
    this.dataValue = dataValue
    this.symbol = symbol
    this.value = [this.time, this.dataValue]
  }
}

@Component({
  components: {
    'v-chart': EChartsComp
  }
})
export default class NewEchartsDetail extends Vue {
  @Prop()
  private dimensions!: Dimension[]

  @Prop({ default: true })
  private beginYAtZero!: boolean

  private chartOptions: EChartOption = {}

  private get detailDataPoints(): DetailDataPoint[] {
    return [
      new DetailDataPoint(
        'hash me as well',
        ['hash me too'],
        'Peter3',
        new Date(new Date().getTime()),
        'this is an OG commit',
        this.randomGarbage()
      ),
      new DetailDataPoint(
        'hash me',
        ['hash you'],
        'Peter',
        new Date(new Date().getTime() - 1000 * 60 * 60),
        'this is a commit',
        this.randomGarbage()
      ),
      new DetailDataPoint(
        'hash me too',
        ['hash me'],
        'Peter2',
        new Date(new Date().getTime() - 1000 * 60 * 120),
        'this is a commit!!',
        this.randomGarbage()
      )
    ]
    // return vxm.detailGraphModule.detailGraph
  }

  private randomGarbage(): Map<DimensionId, number | null> {
    const map = new Map()
    map.set(this.dimensions[0], Math.random() * 20 - 5)
    return map
  }

  private updateGraph() {
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
      dataZoom: [
        {
          type: 'inside'
        },
        {
          type: 'slider'
        }
      ],
      series: this.dimensions.map(this.buildSeries)
    }
  }

  private buildSeries(dimension: DimensionId): EChartOption.SeriesLine {
    const findFirstSuccessful = () => {
      for (let i = 0; i < this.detailDataPoints.length; i++) {
        const value = this.detailDataPoints[i].values.get(dimension)
        if (value !== null && value !== undefined) {
          return value
        }
      }
      return 0
    }

    let lastSuccessfulValue: number = findFirstSuccessful()

    const echartPoints: EchartsDataPoint[] = this.detailDataPoints.map(
      point => {
        let symbol = 'circle'
        let pointValue = point.values.get(dimension)
        if (pointValue === undefined || pointValue === null) {
          pointValue = lastSuccessfulValue
          symbol = this.crossIcon
        }
        lastSuccessfulValue = pointValue

        return new EchartsDataPoint(point.authorDate, pointValue, symbol)
      }
    )

    return {
      type: 'line',
      showSymbol: true,
      symbol: ((value: EchartsDataPoint) => value.symbol) as any,
      symbolSize: 6,
      data: echartPoints as any
    }
  }

  mounted(): void {
    this.dimensions = [
      new Dimension('Random', 'metric', 'cats', 'LESS_IS_BETTER')
    ]
    this.updateGraph()
  }

  // THEME HELPER
  private get graphBackgroundColor() {
    return this.$vuetify.theme.currentTheme.graphBackground as string
  }
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
.echarts {
  width: 100%;
  height: 100%;
}
</style>
