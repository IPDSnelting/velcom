<template>
  <v-container fluid>
    <v-row align="center" justify="center">
      <v-col>
        <div id="chart-container">
          <v-chart
            :autoresize="true"
            :options="chartOptions"
            @datazoom="echartsZoomed"
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
import { Prop } from 'vue-property-decorator'
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

type ValidEchartsSeries = EChartOption.SeriesLine | EChartOption.SeriesGraph
type SeriesGenerationFunction = (id: DimensionId) => ValidEchartsSeries

class EchartsDataPoint {
  // convenience methods for accessing the value
  readonly time: Date
  readonly dataValue: number

  // Used to display the symbol
  readonly symbol: string

  // Needed pointh THIS NAME for Echarts.
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

  constructor(time: Date, dataValue: number, symbol: string, name: string) {
    this.time = time
    this.dataValue = dataValue
    this.symbol = symbol
    this.value = [this.time, this.dataValue]
    this.name = name
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
  private seriesGenerator: SeriesGenerationFunction = this.buildLineSeries
  private zoomStartPercent: number = 0
  private zoomEndPercent: number = 1

  private get detailDataPoints(): DetailDataPoint[] {
    return [
      new DetailDataPoint(
        'Commit1',
        [],
        'Peter3',
        new Date(new Date().getTime()),
        'this is an OG point',
        this.randomGarbage()
      ),
      new DetailDataPoint(
        'Commit2',
        ['Commit1'],
        'Peter',
        new Date(new Date().getTime() - 1000 * 60 * 60),
        'this is a point',
        this.randomGarbage()
      ),
      new DetailDataPoint(
        'Commit3',
        ['Commit1', 'Commit2'],
        'Peter2',
        new Date(new Date().getTime() - 1000 * 60 * 120),
        'this is a point!!',
        this.randomGarbage()
      )
    ]
    // return vxm.detailGraphModule.detailGraph
  }

  private get minDateValue(): number {
    let min = Math.min.apply(
      Math,
      this.detailDataPoints.map(it => it.authorDate.getTime())
    )
    return min || 0
  }

  private get maxDateValue(): number {
    let max = Math.max.apply(
      Math,
      this.detailDataPoints.map(it => it.authorDate.getTime())
    )
    return max || 0
  }

  private randomGarbage(): Map<DimensionId, number | null> {
    const map = new Map()
    map.set(this.dimensions[0], Math.random() * 20 - 5)
    return map
  }

  private updateGraph() {
    console.log('UPDATED')

    this.selectAppropriateSeries()

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
          type: 'inside',
          // Start at the correct place when changing the series type
          start: this.zoomStartPercent * 100,
          end: this.zoomEndPercent * 100
        },
        {
          type: 'slider',
          // Start at the correct place when changing the series type
          start: this.zoomStartPercent * 100,
          end: this.zoomEndPercent * 100
        }
      ],
      series: this.dimensions.map(this.seriesGenerator)
    }
  }

  private buildEchartsDataPoints(dimension: DimensionId): EchartsDataPoint[] {
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

    return this.detailDataPoints.map(point => {
      let symbol = 'circle'
      let pointValue = point.values.get(dimension)
      if (pointValue === undefined || pointValue === null) {
        pointValue = lastSuccessfulValue
        symbol = this.crossIcon
      }
      lastSuccessfulValue = pointValue

      return new EchartsDataPoint(
        point.authorDate,
        pointValue,
        symbol,
        point.hash
      )
    })
  }

  private buildLineSeries(dimension: DimensionId): EChartOption.SeriesLine {
    const echartPoints: EchartsDataPoint[] = this.buildEchartsDataPoints(
      dimension
    )

    return {
      type: 'line',
      showSymbol: true,
      symbol: ((value: EchartsDataPoint) => value.symbol) as any,
      symbolSize: 6,
      data: echartPoints as any
    }
  }

  private buildGraphSeries(dimension: DimensionId): EChartOption.SeriesGraph {
    const echartPoints: EchartsDataPoint[] = this.buildEchartsDataPoints(
      dimension
    )
    let links: EChartOption.SeriesGraph.LinkObject[] = this.detailDataPoints.flatMap(
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
    const totalPoints = this.detailDataPoints.length * this.dimensions.length
    // FIXME: This is now broken, as the graph is not equidistant
    const visibleDataPoints =
      totalPoints * (this.zoomEndPercent - this.zoomStartPercent)

    let newGenerator: SeriesGenerationFunction =
      visibleDataPoints > 2 ? this.buildLineSeries : this.buildGraphSeries

    if (newGenerator !== this.seriesGenerator) {
      this.seriesGenerator = newGenerator
      return 're-render'
    }
    return 'unchanged'
  }

  mounted(): void {
    this.dimensions = [
      new Dimension('Random', 'metric', 'cats', 'LESS_IS_BETTER')
    ]
    this.updateGraph()
  }

  // ==== ECHARTS EVENT HANDLER ====
  private echartsZoomed(e: any) {
    let event: {
      start?: number
      end?: number
      startValue?: number
      endValue?: number
    }
    if (!e.batch || e.batch.length === 0) {
      event = e
    } else {
      event = e.batch[0]
    }

    let startPercent: number
    let endPercent: number

    // Batch and unbatched events set either the percent or absolute value
    // we normalize to percentages
    if (event.start !== undefined && event.end !== undefined) {
      startPercent = event.start / 100
      endPercent = event.end / 100
    } else if (event.startValue !== undefined && event.endValue !== undefined) {
      startPercent = event.startValue / (this.maxDateValue - this.minDateValue)
      endPercent = event.endValue / (this.maxDateValue - this.minDateValue)
    } else {
      return
    }

    this.zoomStartPercent = startPercent
    this.zoomEndPercent = endPercent

    if (this.selectAppropriateSeries() === 're-render') {
      this.updateGraph()
    }
  }

  // ==== THEME HELPER ====
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
