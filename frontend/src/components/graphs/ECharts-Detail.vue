<template>
  <v-container fluid>
    <v-row align="center" justify="center">
      <v-col>
        <datapoint-dialog
          :dialogOpen="datapointDialogOpen"
          :selectedDatapoint="
            selectedDatapoint ? selectedDatapoint.commitInfo : null
          "
          :commitToCompare="commitToCompare ? commitToCompare.commitInfo : null"
          :allowSelectAsReference="allowSelectAsReference"
          @close="datapointDialogOpen = false"
          @removeReference="referenceDatapoint = null"
          @setReference="referenceDatapoint = selectedDatapoint.commitInfo"
          @selectCommitToCompare="commitToCompare = selectedDatapoint"
          @compareCommits="compareCommits"
        ></datapoint-dialog>
        <div id="chart-container">
          <v-chart
            :autoresize="true"
            @click="chartClicked"
            @restore="restored"
            @datazoom="zoomed"
            :options="chartOptions"
          />
        </div>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop, Watch } from 'vue-property-decorator'
import * as d3 from 'd3'
import {
  Commit,
  CommitComparison,
  Datapoint,
  Measurement,
  MeasurementID
} from '../../store/types'
import { crosshairIcon } from '../graphs/crosshairIcon'
import { vxm } from '../../store'
import { formatDateUTC } from '../../util/TimeUtil'
import DatapointDialog from '../dialogs/DatapointDialog.vue'
import EChartsComp from 'vue-echarts'

import { EChartOption } from 'echarts'
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

class ItemInfo {
  name: string
  commitMessage: string
  symbol: string
  itemStyle: {
    color: string
    borderColor: string
    borderWidth: number
  }
  value: [number, number]
  measurementId: MeasurementID

  constructor(
    x: number,
    y: number,
    commitHash: string,
    commitMessage: string,
    symbol: string,
    color: string,
    borderColor: string,
    measurementId: MeasurementID
  ) {
    this.value = [x, y]
    this.commitMessage = commitMessage
    this.name = commitHash
    this.symbol = symbol
    this.measurementId = measurementId
    this.itemStyle = {
      color: color,
      borderColor: borderColor,
      borderWidth: 2
    }
  }
}

type CommitInfo = {
  commit: Commit
  comparison: CommitComparison
  measurementId: MeasurementID
}

@Component({
  components: {
    'datapoint-dialog': DatapointDialog,
    'v-chart': EChartsComp
  }
})
export default class EchartsDetailGraph extends Vue {
  @Prop({})
  measurements!: MeasurementID[]

  @Prop({ default: true })
  beginYAtZero!: boolean

  // dimensions
  private chartOptions: EChartOption = {}
  private showGraph: boolean = false
  private datapointDialogOpen = false
  private selectedDatapoint: {
    commitInfo: CommitInfo
    itemInfo: ItemInfo
  } | null = null
  private commitToCompare: {
    commitInfo: CommitInfo
    itemInfo: ItemInfo
  } | null = null
  private allowSelectAsReference: boolean = true

  private numberFormat: Intl.NumberFormat = new Intl.NumberFormat(
    this.getLocaleString(),
    { maximumFractionDigits: 4 }
  )

  private getLocaleString() {
    return new Intl.NumberFormat().resolvedOptions().locale
  }

  private get referenceDatapoint(): CommitInfo | null {
    return vxm.repoDetailModule.referenceDatapoint
  }

  private set referenceDatapoint(datapoint: CommitInfo | null) {
    vxm.repoDetailModule.referenceDatapoint = datapoint
  }

  private compareCommits() {
    this.$router.push({
      name: 'commit-comparison',
      params: {
        repoID: vxm.repoDetailModule.selectedRepoId,
        hashOne: this.commitToCompare!.itemInfo.name,
        hashTwo: this.selectedDatapoint!.itemInfo.name
      }
    })
  }

  // retrieving and interpreting datapoints
  private get amount(): number {
    return Number.parseInt(vxm.repoDetailModule.selectedFetchAmount)
  }

  private get datapoints(): CommitInfo[] {
    return vxm.repoDetailModule.repoHistory
      .slice()
      .reverse()
      .flatMap(it => {
        return this.measurements.map(measurementId => {
          return {
            commit: it.commit,
            comparison: it.comparison,
            measurementId: measurementId
          }
        })
      })
  }

  // prettier-ignore
  private get wantedMeasurementForDatapoint(): (comparison: CommitComparison, measurementId: MeasurementID) => Measurement | undefined {
    return (comparison: CommitComparison, measurementId: MeasurementID) => {
      if (
        comparison.second &&
        comparison.second.measurements
      ) {
        let wantedMeasurement: Measurement | undefined =
          comparison.second.measurements.find(it => it.id.equals(measurementId))
        return wantedMeasurement
      }
      return undefined
    }
  }

  private datapointValue(datapoint: {
    commit: Commit
    comparison: CommitComparison
    measurementId: MeasurementID
  }): number | undefined {
    let wantedMeasurement = this.wantedMeasurementForDatapoint(
      datapoint.comparison,
      datapoint.measurementId
    )
    if (wantedMeasurement !== undefined && wantedMeasurement.value !== null) {
      return wantedMeasurement.value
    }
    return undefined
  }

  private get dataAvailable(): boolean {
    return this.measurements.length > 0
  }

  private groupBy<K, V>(list: K[], keyGetter: (key: K) => V) {
    const map = new Map()
    list.forEach(item => {
      const key = keyGetter(item)
      const collection = map.get(key)
      if (!collection) {
        map.set(key, [item])
      } else {
        collection.push(item)
      }
    })
    return map
  }

  private get groupedByMeasurement(): Map<string, CommitInfo[]> {
    return this.groupBy(this.datapoints, it => it.measurementId.toString())
  }

  private datapointColor(d: CommitInfo): string {
    let wantedMeasurement = this.wantedMeasurementForDatapoint(
      d.comparison,
      d.measurementId
    )
    if (this.benchmarkFailed(d)) {
      return this.graphFailedOrUnbenchmarkedColor
    } else if (wantedMeasurement) {
      return this.metricColor(d.measurementId)
    }
    // Fill of not benchmarked points is the background color so you do
    // not see the line inside of it
    return this.graphBackgroundColor
  }

  private strokeColor(d: CommitInfo): string {
    let wantedMeasurement = this.wantedMeasurementForDatapoint(
      d.comparison,
      d.measurementId
    )
    if (wantedMeasurement && wantedMeasurement.successful) {
      return this.metricColor(d.measurementId)
    }
    // Failed or unbenchmarked commits have data stroke around them
    return this.graphFailedOrUnbenchmarkedColor
  }

  private metricColor(measurementId: MeasurementID) {
    return vxm.colorModule.colorByIndex(
      this.measurements.findIndex(it => it.equals(measurementId))
    )
  }

  private strokeWidth(d: CommitInfo): number {
    if (this.benchmarkFailed(d)) {
      return 1
    }
    return 2
  }

  private benchmarkFailed(d: CommitInfo): boolean {
    let wantedMeasurement = this.wantedMeasurementForDatapoint(
      d.comparison,
      d.measurementId
    )
    let runFailed: boolean =
      !!d.comparison.second && !!d.comparison.second.errorMessage
    return runFailed || (!!wantedMeasurement && !wantedMeasurement.successful)
  }

  private restored(e: any) {
    this.updateGraphTyp(0, 1)
  }

  private zoomed(e: any) {
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

    if (event.start !== undefined && event.end !== undefined) {
      startPercent = event.start / 100
      endPercent = event.end / 100
    } else if (event.startValue !== undefined && event.endValue !== undefined) {
      startPercent = event.startValue / this.amount
      endPercent = event.endValue / this.amount
    } else {
      return
    }

    this.updateGraphTyp(startPercent, endPercent)
  }

  private updateGraphTyp(startPercent: number, endPercent: number) {
    let visibleCommits = (endPercent - startPercent) * this.amount
    let showSymbols = visibleCommits * this.groupedByMeasurement.size < 200

    if (this.showGraph !== showSymbols) {
      this.showGraph = showSymbols
      this.displaySeries()
    }
  }

  private chartClicked(e: EChartOption.Tooltip.Format) {
    if (e.data === undefined) {
      return
    }

    let referencedCommitInfo = this.datapoints.find(
      it =>
        it.measurementId.equals(e.data.measurementId) &&
        it.commit.hash === e.data.name
    )

    if (!referencedCommitInfo) {
      return
    }

    if ((e as any).event && (e as any).event.event) {
      let event = (e as any).event.event as MouseEvent
      if (event.ctrlKey) {
        let routeData = this.$router.resolve({
          name: 'commit-detail',
          params: {
            repoID: vxm.repoDetailModule.selectedRepoId,
            hash: referencedCommitInfo.commit.hash
          }
        })
        window.open(routeData.href, '_blank')
        return
      }
    }

    this.selectedDatapoint = {
      commitInfo: referencedCommitInfo,
      itemInfo: e.data
    }

    this.allowSelectAsReference =
      this.datapointValue(this.selectedDatapoint.commitInfo) !== undefined
    this.datapointDialogOpen = true
  }

  @Watch('datapoints')
  private updateDatapoints() {
    this.updateData()
  }

  @Watch('graphBackgroundColor')
  @Watch('graphFailedOrUnbenchmarkedColor')
  private updateColors() {
    this.updateData()
  }

  @Watch('beginYAtZero')
  private beginAtZero() {
    if (this.chartOptions.yAxis === undefined) {
      return
    }

    if (Array.isArray(this.chartOptions.yAxis)) {
      console.warn('Could not set y axis begins at 0, I have multiple :(')
      return
    }
    this.chartOptions.yAxis!.scale = this.beginYAtZero
  }

  @Watch('amount')
  private updateData() {
    this.drawGraph()
  }

  private drawGraph() {
    this.chartOptions = {
      backgroundColor: this.graphBackgroundColor,
      grid: {
        left: 20,
        right: 20,
        containLabel: true
      },
      toolbox: {
        show: true,
        showTitle: false, // hide the default text so they don't overlap each other
        feature: {
          restore: { show: true, title: 'Reset' },
          saveAsImage: { show: true, title: 'Save as Image' },
          dataZoom: {
            title: {
              zoom: 'Zoom (brush)',
              back: 'Reset zoom'
            }
          }
        },
        tooltip: {
          show: true
        }
      },
      title: {
        text: 'A graph',
        left: '50%',
        textAlign: 'center',
        top: '20px'
      },
      xAxis: {
        type: 'value',
        min: 1,
        max: this.amount
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
      tooltip: {
        trigger: 'axis',
        axisPointer: {},
        formatter: (val, params) => {
          let formats = Array.isArray(val) ? val : [val]

          if (formats.length === 0) {
            return 'No data'
          }

          let sampleInfo = formats[0].data as ItemInfo

          let parts = formats
            .map(it => {
              let info = it.data as ItemInfo
              // prettier-ignore
              return `
                <tr>
                  <td>Value</td>
                  <td>${this.numberFormat.format(info.value[1])}</td>
                </tr>
                `
            })
            .join('\n')

          return `
            <table>
              <tr>
                <td>Hash</td>
                <td>${sampleInfo.name}</td>
              </tr>
                <td>Message</td>
                <td>${sampleInfo.commitMessage}</td>
              </tr>
              ${parts}
            </table>
          `
        }
      },
      series: []
    }

    this.displaySeries()
  }

  private displaySeries() {
    this.chartOptions.series = []

    let chartSeries = []

    for (let [key, value] of this.groupedByMeasurement.entries()) {
      let newSeries = this.showGraph
        ? this.createGraphSeries(value)
        : this.createLineSeries(value)
      chartSeries.push(newSeries)
    }

    this.chartOptions.series = chartSeries
    this.displayMarkers()
  }

  private createGraphSeries(
    commitInfos: CommitInfo[]
  ): EChartOption.SeriesGraph {
    let lastSuccessfulValue: number = this.firstSuccessful(
      commitInfos[0].measurementId
    )
    let data: ItemInfo[] = []
    let links: EChartOption.SeriesGraph.LinkObject[] = []

    commitInfos.forEach((point, index) => {
      point.commit.parents.forEach(parent => {
        links.push({ source: point.commit.hash, target: parent })
      })
      let value = this.datapointValue(point)
      if (value !== undefined) {
        lastSuccessfulValue = value
        data.push(
          new ItemInfo(
            index + 1,
            value,
            point.commit.hash,
            (point.commit.summary || '').trim(),
            this.datapointSymbol(point),
            this.datapointColor(point),
            this.strokeColor(point),
            point.measurementId
          )
        )
        return
      }
      data.push(
        new ItemInfo(
          index + 1,
          lastSuccessfulValue,
          point.commit.hash,
          (point.commit.summary || '').trim(),
          this.datapointSymbol(point),
          this.datapointColor(point),
          this.strokeColor(point),
          point.measurementId
        )
      )
    })

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
      symbol: ((value: ItemInfo) => {
        return value.symbol
      }) as any,
      lineStyle: {
        color: this.metricColor(commitInfos[0].measurementId)
      },
      symbolSize: 6,
      links: links,
      data: data as any
    }
  }

  private createLineSeries(commitInfos: CommitInfo[]): EChartOption.SeriesLine {
    let lastSuccessfulValue: number = this.firstSuccessful(
      commitInfos[0].measurementId
    )
    let data: ItemInfo[] = commitInfos.map((point, index) => {
      let value = this.datapointValue(point)
      if (value !== undefined) {
        lastSuccessfulValue = value
        return new ItemInfo(
          index + 1,
          value,
          point.commit.hash,
          (point.commit.summary || '').trim(),
          this.datapointSymbol(point),
          this.datapointColor(point),
          this.strokeColor(point),
          point.measurementId
        )
      }
      return new ItemInfo(
        index + 1,
        lastSuccessfulValue,
        point.commit.hash,
        (point.commit.summary || '').trim(),
        this.datapointSymbol(point),
        this.datapointColor(point),
        this.strokeColor(point),
        point.measurementId
      )
    })
    return {
      type: 'line',
      showSymbol: false,
      symbol: ((value: ItemInfo) => {
        return value.symbol
      }) as any,
      lineStyle: {
        color: this.metricColor(commitInfos[0].measurementId)
      },
      connectNulls: true,
      symbolSize: 6,
      data: data as any
    }
  }

  @Watch('referenceDatapoint')
  @Watch('commitToCompare')
  private displayMarkers() {
    let marklineData: any[] = []
    if (this.referenceDatapoint) {
      marklineData = [{ yAxis: this.datapointValue(this.referenceDatapoint) }]
    }
    let markPointData: any[] = []

    if (this.commitToCompare) {
      markPointData.push({
        coord: this.commitToCompare.itemInfo.value,
        label: {
          show: true,
          position: 'inside',
          formatter: () => 'A'
        }
      })
    }

    for (let series of this.chartOptions.series!) {
      // FIXME: Proper types?
      let asLine = series as EChartOption.SeriesLine | EChartOption.SeriesGraph

      let markPointJson = {
        silent: true,
        data: markPointData as any // ts types are garbage :/
      }
      Vue.set(asLine, 'markPoint', markPointJson)

      let markLineJson = {
        symbol: 'none',
        lineStyle: {
          type: 'dotted',
          width: 2
        },
        label: {
          show: true,
          formatter: () => {
            return 'Reference'
          }
        },
        silent: true,
        data: marklineData as any
      }
      Vue.set(asLine, 'markLine', markLineJson)
    }
  }

  private firstSuccessful(measurementId: MeasurementID): number {
    let datapoints = this.groupedByMeasurement.get(measurementId.toString())!
    for (const datapoint of datapoints) {
      let wantedMeasurement = this.wantedMeasurementForDatapoint(
        datapoint.comparison,
        measurementId
      )
      if (
        wantedMeasurement !== undefined &&
        wantedMeasurement.successful &&
        wantedMeasurement.value
      ) {
        return wantedMeasurement.value || 0
      }
    }
    return 0
  }

  datapointSymbol(d: CommitInfo): string {
    if (this.benchmarkFailed(d)) {
      return 'path://M24 20.188l-8.315-8.209 8.2-8.282-3.697-3.697-8.212 8.318-8.31-8.203-3.666 3.666 8.321 8.24-8.206 8.313 3.666 3.666 8.237-8.318 8.285 8.203z'
    }
    return 'circle'
  }

  mounted() {
    this.updateData()
  }

  private get graphBackgroundColor() {
    return this.$vuetify.theme.currentTheme.graphBackground as string
  }

  private get graphFailedOrUnbenchmarkedColor() {
    return this.$vuetify.theme.currentTheme.graphFailedOrUnbenchmarked as string
  }
}
</script>
<style>
.echarts {
  width: 100%;
  height: 100%;
}

.axis text {
  font-family: Roboto;
  font-size: 13px;
}

.tooltip-table tr td {
  padding: 2px;
}

.tooltip-table tr td:nth-child(2) {
  font-family: monospace;
}
.tooltip-table tr td:first-child {
  padding-right: 10px;
}
.tooltip-table tr td:only-child {
  font-weight: bold;
  padding-top: 1em;
  font-size: 1.1em;
}

.tooltip {
  font-size: 10pt;
  position: absolute;
  padding: 5px;
  border-radius: 5px;
  background-color: rgba(0, 0, 0, 0.8);
  color: rgba(255, 255, 255, 0.9);
  text-align: center;
  margin: 0;
}

.tooltip:after {
  content: '';
  display: block;
  width: 0;
  height: 0;
  position: absolute;
  border-top: 8px solid transparent;
  border-bottom: 8px solid transparent;
  border-right: 8px solid black;
  transform: var(--tail-rotation);
  left: var(--tail-left);
  top: var(--tail-top);
}

.information {
  text-align: center;
  font-size: 18px;
  fill: currentColor;
  opacity: 0.8;
}

#referenceLine {
  fill: none;
  stroke-width: 1px;
  stroke-dasharray: 5 5;
}

#chart-container {
  position: relative;
  height: 80vh;
}

.datapointDialog .v-input .v-label {
  height: unset !important;
}
</style>
