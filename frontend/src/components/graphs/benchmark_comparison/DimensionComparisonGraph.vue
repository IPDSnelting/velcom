<template>
  <v-container fluid class="full-height">
    <v-row align="center" justify="center" class="full-height" no-gutters>
      <v-col class="full-height">
        <div id="chart-container" class="full-height">
          <v-chart
            ref="chart"
            :autoresize="true"
            :options="chartOptions"
            :theme="chartTheme"
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
  DimensionComparisonValue,
  DimensionComparisonPoint,
  RepoId
} from '@/store/types'
import { ComposeOption, use } from 'echarts/core'
import { BarChart, BarSeriesOption } from 'echarts/charts'
import {
  GridComponent,
  GridComponentOption,
  LegendComponent,
  LegendComponentOption,
  TooltipComponent,
  TooltipComponentOption,
  AriaComponent,
  AriaComponentOption,
  DataZoomComponent,
  DataZoomComponentOption,
  DataZoomInsideComponent,
  DataZoomSliderComponent,
  BrushComponent,
  BrushComponentOption,
  ToolboxComponent,
  ToolboxComponentOption
} from 'echarts/components'
import EChartsComp from 'vue-echarts'
import { Prop, Watch } from 'vue-property-decorator'
import { vxm } from '@/store'

use([
  BarChart,
  GridComponent,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  AriaComponent,
  DataZoomComponent,
  DataZoomInsideComponent,
  DataZoomSliderComponent,
  BrushComponent,
  ToolboxComponent
])

// A minimal types for option is useful for checking if any components are missing.
type ECOption = ComposeOption<
  | BarSeriesOption
  | GridComponentOption
  | LegendComponentOption
  | TooltipComponentOption
  | AriaComponentOption
  | DataZoomComponentOption
  | BrushComponentOption
  | ToolboxComponentOption
>

type SeriesEntry = [string, DimensionComparisonValue]
type RepoValue = SeriesEntry[]

@Component({
  components: {
    'v-chart': EChartsComp
  }
})
export default class DimensionComparisonGraph extends Vue {
  private chartOptions: ECOption = {}

  @Prop()
  private readonly datapoints!: DimensionComparisonPoint[]

  @Prop({ default: null })
  private readonly baselinePoint!: DimensionComparisonPoint | null

  private normalizeToBaseline(
    dimensionName: string,
    value: DimensionComparisonValue
  ) {
    if (typeof value !== 'number' || !this.baselinePoint) {
      return value
    }
    const baselineValue = this.baselinePoint.data.get(dimensionName)
    if (typeof baselineValue !== 'number') {
      return value
    }
    return value / baselineValue
  }

  private baselineHasDimension(dimension: string) {
    if (!this.baselinePoint) {
      return true
    }
    return this.baselinePoint.data.has(dimension)
  }

  private get seriesData(): Map<string, RepoValue> {
    const map = new Map<string, RepoValue>()

    this.datapoints.forEach(repoPoint => {
      const repoValue: RepoValue = Array.from(repoPoint.data.entries())
      const normalized: RepoValue = repoValue
        .filter(([name]) => this.baselineHasDimension(name))
        .map(([name, value]) => [name, this.normalizeToBaseline(name, value)])
      map.set(repoPoint.repoId, normalized)
    })

    return map
  }

  @Watch('datapoints')
  @Watch('baselinePoint')
  private init() {
    this.chartOptions = {
      darkMode: true,
      legend: {},
      grid: {
        left: 20,
        right: 20,
        containLabel: true
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      toolbox: {
        left: 'center',
        top: '20px',
        feature: {
          dataZoom: {
            xAxisIndex: 0,
            yAxisIndex: -1,
            brushStyle: {
              opacity: 1
            }
          },
          restore: { show: true },
          dataView: { show: true },
          saveAsImage: {
            show: true,
            pixelRatio: 2,
            type: 'jpg',
            backgroundColor: this.graphBackgroundColor
          }
        }
      },
      dataZoom: [
        {
          type: 'slider',
          yAxisIndex: 0,
          filterMode: 'none'
        },
        {
          type: 'inside',
          xAxisIndex: 0
        }
      ],
      xAxis: {
        type: 'category'
      },
      yAxis: {
        type: 'value'
      },
      series: this.datapoints.map(this.generateSeries),
      aria: {
        enabled: true,
        decal: {
          show: true
        }
      }
    }
  }

  private generateSeries(point: DimensionComparisonPoint): BarSeriesOption {
    return {
      type: 'bar',
      name: this.repoName(point.repoId),
      data: this.seriesData.get(point.repoId),
      emphasis: {
        focus: 'series',
        itemStyle: {
          shadowBlur: 2
        }
      }
    }
  }

  private repoName(id: RepoId) {
    const repo = vxm.repoModule.repoById(id)
    if (repo) {
      return repo.name
    }
    return id
  }

  private mounted() {
    this.init()
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
      legend: {},
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

  private get graphBackgroundColor() {
    return this.$vuetify.theme.currentTheme.graphBackground as string
  }

  private get themeColor(): (key: string) => string {
    return key => this.$vuetify.theme.currentTheme[key] as string
  }
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
.echarts {
  width: 100%;
  height: 100%;
}
</style>
