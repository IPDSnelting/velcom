<template>
  <v-container fluid class="full-height mt-0 pt-0">
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
  Dimension,
  DimensionId,
  dimensionIdEquals,
  dimensionIdToString,
  MeasurementError,
  MeasurementSuccess,
  RepoId,
  Run,
  RunResultScriptError,
  RunResultSuccess,
  RunResultVelcomError,
  StatusComparisonPoint
} from '@/store/types'
import { ComposeOption, use } from 'echarts/core'
import { BarChart, BarSeriesOption } from 'echarts/charts'
import {
  AriaComponent,
  AriaComponentOption,
  BrushComponent,
  BrushComponentOption,
  DataZoomComponent,
  DataZoomComponentOption,
  DataZoomInsideComponent,
  DataZoomSliderComponent,
  GridComponent,
  GridComponentOption,
  LegendComponent,
  LegendComponentOption,
  ToolboxComponent,
  ToolboxComponentOption,
  TooltipComponent,
  TooltipComponentOption
} from 'echarts/components'
import EChartsComp from 'vue-echarts'
import { Prop, Watch } from 'vue-property-decorator'
import { vxm } from '@/store'
import { escapeHtml } from '@/util/Texts'
import { mdiDotsHexagon } from '@mdi/js'

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

type DatapointErrorType =
  | 'NO_RUN'
  | 'MEASUREMENT_FAILED'
  | 'RUN_FAILED'
  | 'NO_MEASUREMENT'

class DatapointValue {
  readonly dimension: DimensionId
  readonly repoId: RepoId
  /**
   * Used by Echarts, matched based on the name!
   */
  readonly value: [string, number]

  constructor(dimension: DimensionId, repoId: RepoId, value: number) {
    this.dimension = dimension
    this.repoId = repoId
    this.value = [dimensionIdToString(dimension), value]
  }
}
class DatapointDimensionError {
  readonly dimension: DimensionId
  readonly repoId: RepoId
  readonly error: string

  /**
   * Used by Echarts, matched based on the name!
   */
  readonly value: [string, number]

  readonly itemStyle: any
  readonly label: any

  constructor(
    dimension: DimensionId,
    repoId: RepoId,
    error: string,
    errorType: DatapointErrorType,
    dummyValue: number,
    color: string,
    themeColor: (name: string) => string
  ) {
    this.dimension = dimension
    this.repoId = repoId
    this.error = error
    this.value = [dimensionIdToString(dimension), dummyValue]

    let labelText
    if (errorType === 'NO_RUN') {
      labelText = 'Unbenchmarked'
    } else if (errorType === 'RUN_FAILED') {
      labelText = 'Run failed'
    } else if (errorType === 'NO_MEASUREMENT') {
      labelText = 'Not measured'
    } else {
      labelText = 'Dimension failed'
    }
    this.label = {
      show: true,
      formatter: labelText,
      rotate: 90,
      fontWeight: 'bold',
      overflow: 'truncate',
      lineOverflow: 'truncate'
    }
    this.itemStyle = {
      borderType: 'dashed',
      borderColor: themeColor('warning'),
      borderWidth: 2
    }
  }
}
class DatapointRepoError {
  readonly error: string
  readonly repoId: RepoId
  readonly type: DatapointErrorType

  constructor(error: string, repoId: RepoId, type: DatapointErrorType) {
    this.error = error
    this.repoId = repoId
    this.type = type
  }
}

type Datapoint = DatapointValue | DatapointDimensionError

@Component({
  components: {
    'v-chart': EChartsComp
  }
})
export default class StatusComparisonGraph extends Vue {
  private chartOptions: ECOption = {}
  private showDecals = false

  @Prop()
  private readonly datapoints!: StatusComparisonPoint[]

  @Prop({ default: null })
  private readonly baselinePoint!: StatusComparisonPoint | null

  @Prop()
  private readonly selectedDimensions!: Dimension[]

  private get maxDatapointValue() {
    const values = this.datapoints
      .filter(it => it.run)
      .map(it => it.run!.result)
      .filter(it => it instanceof RunResultSuccess)
      .flatMap(it => (it as RunResultSuccess).measurements)
      .filter(it => it instanceof MeasurementSuccess)
      .filter(it => this.isSelected(it.dimension))
      .map(it => (it as MeasurementSuccess).value)

    if (values.length === 0) {
      // Arbitrary placeholder so something is displayed
      return 1
    }

    return Math.max(...values)
  }

  private isSelected(dimension: Dimension) {
    return this.selectedDimensions.find(it => dimensionIdEquals(it, dimension))
  }

  private get processedDataPoints(): Map<
    string,
    Datapoint[] | DatapointRepoError
  > {
    const map: Map<string, Datapoint[] | DatapointRepoError> = new Map()
    for (const point of this.datapoints) {
      const id = point.repoId
      if (!point.run) {
        map.set(id, new DatapointRepoError('Unbenchmarked', id, 'NO_RUN'))
        continue
      }
      const points = this.pointsForRun(point.run, id)
      map.set(id, points)
    }
    return map
  }

  private pointsForRun(
    run: Run,
    repoId: RepoId
  ): DatapointRepoError | Datapoint[] {
    if (run.result instanceof RunResultScriptError) {
      return new DatapointRepoError(run.result.error, repoId, 'RUN_FAILED')
    }
    if (run.result instanceof RunResultVelcomError) {
      return new DatapointRepoError(run.result.error, repoId, 'RUN_FAILED')
    }
    return run.result.measurements
      .filter(measurement => this.isSelected(measurement.dimension))
      .map(measurement => {
        if (measurement instanceof MeasurementError) {
          return new DatapointDimensionError(
            measurement.dimension,
            repoId,
            measurement.error,
            'MEASUREMENT_FAILED',
            this.maxDatapointValue,
            this.repoColor(repoId),
            this.themeColor
          )
        }
        return new DatapointValue(
          measurement.dimension,
          repoId,
          measurement.value
        )
      })
  }

  @Watch('datapoints')
  @Watch('baselinePoint')
  @Watch('chartTheme')
  @Watch('selectedDimensions')
  private init() {
    this.chartOptions = {
      darkMode: vxm.userModule.darkThemeSelected,
      legend: {
        type: 'scroll'
      },
      grid: {
        left: 20,
        right: 20,
        bottom: 0,
        containLabel: true
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        },
        formatter: this.tooltipFormatter
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
          },
          myDecal: {
            show: true,
            title: this.showDecals ? 'Hide decals' : 'Show decals',
            icon: mdiDotsHexagon,
            onclick: () => {
              this.showDecals = !this.showDecals
              const ariaOption = this.chartOptions.aria as AriaComponentOption
              ariaOption.decal!.show = this.showDecals
            }
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
      series: this.datapoints.map(point => this.generateSeries(point.repoId)),
      aria: {
        enabled: true,
        decal: {
          show: this.showDecals
        }
      }
    }
  }

  // The correct type is not exposed sadly
  private tooltipFormatter(params: any) {
    const values = Array.isArray(params) ? params.slice() : [params]
    // Sort them so the order corresponds to the order of the lines
    const seriesRows = values.map(val => {
      const color = val.color
      const datapoint = val.data as Datapoint
      const safeDisplayName = escapeHtml(this.repoName(datapoint.repoId))
      let value: string
      if (!Object.hasOwnProperty.call(datapoint, 'error')) {
        value = this.numberFormat.format(datapoint.value[1])
      } else {
        const error = (datapoint as any).error
        value = escapeHtml(error.substring(0, 40))
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
    const samplePoint = values[0].data as Datapoint

    if (!samplePoint) {
      return 'No point found :/'
    }

    const dimension = dimensionIdToString(samplePoint.dimension)
    return `
            ${escapeHtml(dimension)}
           <table class="echarts-tooltip-table">
             ${seriesRows.join('\n')}
           </table>
            `
  }

  private generateSeries(repoId: RepoId): BarSeriesOption {
    const data = this.processedDataPoints.get(repoId)!
    let datapoints: Datapoint[]

    if (data instanceof DatapointRepoError) {
      datapoints = this.selectedDimensions.map(dimension => {
        return new DatapointDimensionError(
          dimension,
          repoId,
          data.error,
          data.type,
          this.maxDatapointValue,
          this.repoColor(repoId),
          this.themeColor
        )
      })
    } else {
      datapoints = data
    }

    const seriesData = this.selectedDimensions.map(dim => {
      const displayedPoint = datapoints.find(point =>
        dimensionIdEquals(dim, point.dimension)
      )
      if (displayedPoint) return displayedPoint
      return new DatapointDimensionError(
        dim,
        repoId,
        'Not measured',
        'NO_MEASUREMENT',
        this.maxDatapointValue,
        this.repoColor(repoId),
        this.themeColor
      )
    })

    return {
      type: 'bar',
      name: this.repoName(repoId),
      data: seriesData,
      color: this.repoColor(repoId),
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

  private repoColor(id: RepoId) {
    return vxm.colorModule.colorByIndex(
      vxm.repoModule.allRepos.map(it => it.id).indexOf(id)
    )
  }

  private mounted() {
    this.init()
  }

  private get numberFormat(): Intl.NumberFormat {
    return new Intl.NumberFormat(
      new Intl.NumberFormat().resolvedOptions().locale,
      { maximumFractionDigits: 3 }
    )
  }

  private get chartTheme() {
    const axisSettings = () => ({
      axisLine: {
        lineStyle: {
          color: this.themeColor('graphTextColor')
        }
      },
      axisTick: {
        lineStyle: {
          color: this.themeColor('graphTextColor')
        }
      },
      axisLabel: {
        color: this.themeColor('graphTextColor')
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
      legend: {
        textStyle: {
          color: this.themeColor('graphTextColor')
        }
      },
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
