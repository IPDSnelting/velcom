<template>
  <v-container fluid>
    <v-row align="center" justify="center">
      <v-col>
        <div id="chart" :style="{'height': this.height + 'px'}">
          <svg id="mainSvg" />
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
import { vxm } from '../../store'
import { formatDateUTC } from '../../util/TimeUtil'

type CommitInfo = { commit: Commit; comparison: CommitComparison }

@Component
export default class NewDetailGraph extends Vue {
  @Prop({})
  measurement!: MeasurementID

  @Prop({})
  amount!: number

  @Prop({ default: true })
  beginYAtZero!: boolean

  // anything with and height related

  private width: number = 0
  private height: number = 0

  private margin: {
    left: number
    right: number
    top: number
    bottom: number
  } = {
    left: 100,
    right: 30,
    top: 10,
    bottom: 100
  }

  private get innerWidth() {
    return this.width - this.margin.left - this.margin.right
  }

  private get innerHeight() {
    return this.height - this.margin.top - this.margin.bottom
  }

  private get mainSvg(): d3.Selection<SVGGElement, unknown, HTMLElement, any> {
    return d3
      .select('#mainSvg')
      .attr('width', '100%')
      .attr('height', '100%')
      .attr('align', 'end')
      .attr('justify', 'end')
      .append('g')
      .attr('id', 'dataLayer')
      .attr(
        'transform',
        'translate(' + this.margin.left + ',' + this.margin.top + ')'
      )
  }

  private resizeListener: () => void = () => {}

  // anything related with getting values

  private get datapoints(): CommitInfo[] {
    return vxm.repoDetailModule.repoHistory.slice().reverse()
  }

  // prettier-ignore
  private get wantedMeasurementForDatapoint(): (comparison: CommitComparison) => Measurement | undefined {
    return (comparison: CommitComparison) => {
      if (
        comparison.second &&
        comparison.second.measurements
      ) {
        let wantedMeasurement: Measurement | undefined =
          comparison.second.measurements.find(it => it.id.equals(this.measurement))
        return wantedMeasurement
      }
      return undefined
    }
  }

  private datapointValue(datapoint: {
    commit: Commit
    comparison: CommitComparison
  }): number | undefined {
    let wantedMeasurement = this.wantedMeasurementForDatapoint(
      datapoint.comparison
    )
    if (wantedMeasurement !== undefined && wantedMeasurement.value) {
      return wantedMeasurement.value
    }
    return undefined
  }

  private get minVal(): number | undefined {
    return d3.min(this.datapoints, this.datapointValue)
  }

  private get maxVal(): number | undefined {
    return d3.max(this.datapoints, this.datapointValue)
  }

  private get dataAvailable(): boolean {
    return this.measurement.metric !== '' && this.maxVal !== undefined
  }

  private lastValue: number = 0

  get firstSuccessful(): number {
    for (const datapoint of this.datapoints) {
      let wantedMeasurement = this.wantedMeasurementForDatapoint(
        datapoint.comparison
      )
      if (
        wantedMeasurement !== undefined &&
        wantedMeasurement.successful &&
        wantedMeasurement.value
      ) {
        return wantedMeasurement.value
      }
    }
    return this.height / 2
  }

  private graphDrawn: boolean = false

  // anything axes related

  private get xScale(): d3.ScaleLinear<number, number> {
    return d3
      .scaleLinear()
      .domain([this.amount, 0])
      .range([0, this.innerWidth])
  }

  private get yScale(): d3.ScaleLinear<number, number> {
    let min: number = !this.beginYAtZero && this.minVal ? this.minVal : 0
    let max: number = this.maxVal || 0
    return d3
      .scaleLinear()
      .domain([min, max])
      .range([this.innerHeight, 0])
  }

  private x(comparison: CommitComparison): number {
    return this.xScale(
      this.datapoints.length -
        this.datapoints.findIndex(it => it.comparison === comparison)
    )
  }
  private y(comparison: CommitComparison): number {
    let wantedMeasurement = this.wantedMeasurementForDatapoint(comparison)
    if (wantedMeasurement !== undefined && wantedMeasurement.value) {
      this.lastValue = wantedMeasurement.value
      return this.yScale(wantedMeasurement.value)
    }
    if (this.datapoints.findIndex(it => it.comparison === comparison) === 0) {
      this.lastValue = this.firstSuccessful
    }
    return this.yScale(this.lastValue)
  }

  private valueFormat: any = d3.format('<.4')

  private xAxisFormat(d: any) {
    if (d % 1 === 0) {
      return d3.format('.0f')(d)
    } else {
      return ''
    }
  }

  private get xAxis(): d3.Axis<number | { valueOf(): number }> {
    return d3.axisBottom(this.xScale).tickFormat(this.xAxisFormat)
  }

  private get yAxis(): d3.Axis<number | { valueOf(): number }> {
    return d3.axisLeft(this.yScale)
  }

  get unit(): string | null {
    for (const datapoint of this.datapoints) {
      let wantedMeasurement = this.wantedMeasurementForDatapoint(
        datapoint.comparison
      )
      if (wantedMeasurement !== undefined && wantedMeasurement.unit) {
        return wantedMeasurement.unit
      }
    }
    return null
  }

  private get yLabel(): string {
    if (this.measurement.metric) {
      return this.unit
        ? this.measurement.metric + ' in ' + this.unit
        : this.measurement.metric
    } else {
      return ''
    }
  }

  // drawing the actual graph

  private drawGraph() {
    if (this.dataAvailable) {
      if (!this.graphDrawn) {
        this.mainSvg.selectAll('*').remove()
        this.defineSvgElements()
        this.graphDrawn = true
      }

      let keyFn: d3.ValueFn<any, any, string> = (d: CommitInfo) => {
        return d.commit.hash
      }
      this.drawPath()
      this.drawDatapoints(keyFn)
      this.appendTooltips(keyFn)
    } else {
      if (this.graphDrawn) {
        this.mainSvg.selectAll('*').remove()
        this.graphDrawn = false
      }
      let information: string =
        this.measurement.metric === ''
          ? 'No data available. Please select benchmark and metric.'
          : 'There are no commits within the specified time period that have been benchmarked with this metric.'

      this.mainSvg
        .append('text')
        .attr('y', this.innerHeight / 2)
        .attr('x', this.margin.left)
        .text(information)
        .style('text-align', 'center')
        .style('font-family', 'Roboto')
        .style('font-size', '18px')
        .style('fill', 'grey')
    }
  }

  private drawPath() {
    let path: d3.Selection<
      SVGPathElement,
      CommitInfo[],
      d3.BaseType,
      unknown
    > = d3
      .select('#dataLayer')
      .selectAll<SVGPathElement, unknown>('#line')
      .data([this.datapoints])
    let newPath = path
      .enter()
      .append('path')
      .attr('id', 'line')
      .merge(path)
      .transition()
      .duration(1000)
      .delay(100)
      .attr('d', this.line)
      .attr('stroke', this.colorById(this.selectedRepo))
      .attr('stroke-width', 2)
      .attr('fill', 'none')
      .attr('pointer-events', 'none')
    path
      .exit()
      .transition()
      .attr('opacity', 0)
      .attr('width', 0)
      .remove()
  }

  private drawDatapoints(keyFn: d3.ValueFn<any, any, string>) {
    let datapoints: d3.Selection<
      SVGPathElement,
      CommitInfo,
      d3.BaseType,
      unknown
    > = d3
      .select('#dataLayer')
      .selectAll<SVGPathElement, unknown>('.datapoint')
      .data(this.datapoints, keyFn)

    let newDatapoints = datapoints
      .enter()
      .append('path')
      .attr('class', 'datapoint')
      .attr('id', (d: CommitInfo) => '_' + d.commit.hash)
      .merge(datapoints)
      .transition()
      .duration(1000)
      .delay(100)
      .attr(
        'd',
        d3
          .symbol()
          .type((d: CommitInfo) => this.datapointSymbol(d))
          .size((d: CommitInfo) => this.datapointSize(d))
      )
      .attr(
        'transform',
        (d: CommitInfo) =>
          'translate(' +
          this.x(d.comparison) +
          ', ' +
          this.y(d.comparison) +
          ') rotate(-45)'
      )
      .attr('fill', (d: CommitInfo) => this.datapointColor(d))
      .attr('stroke', (d: CommitInfo) => this.strokeColor(d))
      .attr('stroke-width', (d: CommitInfo) => this.strokeWidth(d))
      .attr('opacity', 1)

    datapoints
      .exit()
      .transition()
      .attr('opacity', 0)
      .attr('width', 0)
      .remove()
  }

  private appendTooltips(keyFn: d3.ValueFn<any, any, string>) {
    let tooltip = d3
      .selectAll('.datapoint')
      .data(this.datapoints, keyFn)
      .on('mouseover', this.mouseover)
      .on('mousemove', this.mousemove)
      .on('mouseleave', this.mouseleave)
      .on('click', (d: CommitInfo) => {
        this.$router.push({
          name: 'commit-detail',
          params: { repoID: this.selectedRepo, hash: d.commit.hash }
        })
      })
      .on('contextmenu', (d: CommitInfo) => {
        d3.event.preventDefault()
        this.openDatapointMenu(d)
      })
  }

  datapointSymbol(d: CommitInfo): d3.SymbolType {
    if (this.benchmarkFailed(d)) {
      return d3.symbolCross
    }
    return d3.symbolCircle
  }

  datapointSize(d: CommitInfo): number {
    if (this.benchmarkFailed(d)) {
      return 100
    }
    return 50
  }

  datapointColor(d: CommitInfo): string {
    let wantedMeasurement = this.wantedMeasurementForDatapoint(d.comparison)
    if (this.benchmarkFailed(d)) {
      return 'grey'
    } else if (wantedMeasurement) {
      return this.colorById(this.selectedRepo)
    }
    return 'white'
  }

  strokeColor(d: CommitInfo): string {
    let wantedMeasurement = this.wantedMeasurementForDatapoint(d.comparison)
    if (wantedMeasurement && wantedMeasurement.successful) {
      return this.colorById(this.selectedRepo)
    }
    return 'grey'
  }

  private strokeWidth(d: CommitInfo): number {
    if (this.benchmarkFailed(d)) {
      return 0
    }
    return 2
  }

  private benchmarkFailed(d: CommitInfo): boolean {
    let wantedMeasurement = this.wantedMeasurementForDatapoint(d.comparison)
    let runFailed: boolean =
      !!d.comparison.second && !!d.comparison.second.errorMessage
    return runFailed || (!!wantedMeasurement && !wantedMeasurement.successful)
  }

  private get colorById(): (repoID: string) => string {
    return (repoID: string) => {
      let index: number = vxm.repoModule.repoIndex(repoID)
      return vxm.colorModule.colorByIndex(index)
    }
  }

  get line() {
    return d3
      .line<CommitInfo>()
      .x((datapoint: CommitInfo) => {
        return this.x(datapoint.comparison)
      })
      .y((datapoint: CommitInfo) => {
        return this.y(datapoint.comparison)
      })
  }

  mouseover(d: any) {
    d3.select('#tooltip')
      .transition()
      .duration(300)
      .style('opacity', 1)
      .style('visibility', 'visible')
  }
  private mousemove(d: { commit: Commit; comparison: CommitComparison }) {
    let tooltip: d3.Selection<
      d3.BaseType,
      unknown,
      HTMLElement,
      any
    > = d3.select('#tooltip')
    let tipWidth = (tooltip.node() as HTMLElement).getBoundingClientRect().width
    let tipHeight = (tooltip.node() as HTMLElement).getBoundingClientRect()
      .height

    let wantedMeasurement = this.wantedMeasurementForDatapoint(d.comparison)
    let htmlMessage: string = ''
    if (
      d.commit.authorDate &&
      wantedMeasurement &&
      wantedMeasurement.successful
    ) {
      htmlMessage = `
        <table class="tooltip-table">
          <tr>
            <td>Commit</td>
            <td>${d.commit.hash}</td>
          </tr>
          <tr>
            <td>Author</td>
            <td>${d.commit.author}</td>
          </tr>
          <tr>
            <td>Date</td>
            <td>${formatDateUTC(d.commit.authorDate)}</td>
          </tr>
          <tr>
            <td>Exact value</td>
            <td>${this.valueFormat(wantedMeasurement.value)} ${this.unit}</td>
          </tr>
          <tr>
            <td>Commit summary</td>
            <td>${d.commit.summary!.trim()}</td>
          </tr>
        </table>
      `
    } else if (d.commit.authorDate && this.benchmarkFailed(d)) {
      htmlMessage = `
        <table class="tooltip-table">
          <tr>
            <td>Commit</td>
            <td>${d.commit.hash}</td>
          </tr>
          <tr>
            <td>Author</td>
            <td>${d.commit.author}</td>
          </tr>
          <tr>
            <td>Date</td>
            <td>${formatDateUTC(d.commit.authorDate)}</td>
          </tr>
          <tr>
            <td>Commit summary</td>
            <td>${d.commit.summary!.trim()}</td>
          </tr>
          <tr>
            <td colspan=2>This commit has not been benchmarked successfully.</td>
          </tr>
       </table>
      `
    } else if (d.commit.authorDate) {
      htmlMessage = `
        <table class="tooltip-table">
          <tr>
            <td>Commit</td>
            <td>${d.commit.hash}</td>
          </tr>
          <tr>
            <td>Author</td>
            <td>${d.commit.author}</td>
          </tr>
          <tr>
            <td>Date</td>
            <td>${formatDateUTC(d.commit.authorDate)}</td>
          </tr>
          <tr>
            <td>Commit summary</td>
            <td>${d.commit.summary!.trim()}</td>
          </tr>
          <tr>
            <td colspan=2>This commit has not been benchmarked with this metric.</td>
          </tr>
       </table>
      `
    } else {
      htmlMessage =
        'Commit ' + d.commit.hash + '<br />author:' + d.commit.author
    }
    tooltip.html(htmlMessage)

    let horizontalMousePos = d3.mouse(
      d3.select('#mainSvg').node() as SVGSVGElement
    )[0]
    let verticalMousePos = d3.mouse(
      d3.select('#mainSvg').node() as SVGSVGElement
    )[1]

    if (horizontalMousePos < this.width / 2) {
      tooltip.style('left', horizontalMousePos - 20 + 'px')
      ;(tooltip.node() as HTMLElement).style.setProperty('--tail-left', '15px')
    } else {
      tooltip.style('left', horizontalMousePos - tipWidth + 20 + 'px')
      ;(tooltip.node() as HTMLElement).style.setProperty(
        '--tail-left',
        tipWidth - 25 + 'px'
      )
    }
    if (verticalMousePos < this.height / 2) {
      tooltip.style('top', verticalMousePos + 10 + 'px')
      ;(tooltip.node() as HTMLElement).style.setProperty('--tail-top', '-10px')
      ;(tooltip.node() as HTMLElement).style.setProperty(
        '--tail-rotation',
        'rotate(90deg)'
      )
    } else {
      tooltip.style('top', verticalMousePos - tipHeight - 10 + 'px')
      ;(tooltip.node() as HTMLElement).style.setProperty(
        '--tail-top',
        tipHeight - 5 + 'px'
      )
      ;(tooltip.node() as HTMLElement).style.setProperty(
        '--tail-rotation',
        'rotate(270deg)'
      )
    }
  }

  mouseleave(d: any) {
    d3.select('#tooltip')
      .transition()
      .duration(500)
      .style('opacity', 0)
      .style('visibility', 'hidden')
  }
  get selectedRepo(): string {
    return vxm.repoDetailModule.selectedRepoId
  }

  openDatapointMenu(datapoint: CommitInfo) {
    d3.select('#_' + datapoint.commit.hash).attr('stroke', 'red')
    d3.select('#reference-line')
      .attr('x1', this.xScale(0))
      .attr('y1', this.y(datapoint.comparison))
      .attr('x2', this.xScale(this.amount))
      .attr('y2', this.y(datapoint.comparison))
  }

  // updating

  private resize() {
    let chart = d3.select('#chart').node() as HTMLElement
    this.width = chart ? chart.getBoundingClientRect().width : 900
    this.height = this.width / 2
    this.updateData()
  }

  @Watch('datapoints')
  private updateDatapoints() {
    d3.select('#yLabel').text(this.yLabel)
    this.drawGraph()
  }

  @Watch('beginYAtZero')
  @Watch('amount')
  private updateData() {
    this.updateAxes()
    this.drawGraph()
  }

  private updateAxes() {
    ;(d3.select('#xAxis') as d3.Selection<
      SVGGElement,
      unknown,
      HTMLElement,
      any
    >)
      .attr('transform', 'translate(0,' + this.innerHeight + ')')
      .call(this.xAxis)
    ;(d3.select('#yAxis') as d3.Selection<
      SVGGElement,
      unknown,
      HTMLElement,
      any
    >).call(this.yAxis)
    d3.select('#yLabel')
      .attr('y', -this.margin.left + 30)
      .attr('x', -this.innerHeight / 2)
  }

  private defineSvgElements() {
    this.mainSvg
      .append('g')
      .attr('class', 'axis')
      .attr('id', 'xAxis')
      .attr('transform', 'translate(0,' + this.innerHeight + ')')
      .call(this.xAxis)

    this.mainSvg
      .append('g')
      .attr('class', 'axis')
      .attr('id', 'yAxis')
      .call(this.yAxis)

    this.mainSvg
      .append('text')
      .attr('id', 'yLabel')
      .attr('text-anchor', 'middle')
      .attr('transform', 'rotate(-90)')
      .attr('y', -this.margin.left + 30)
      .attr('x', -this.innerHeight / 2)
      .text(this.yLabel)

    this.mainSvg
      .append('g')
      .append('line')
      .attr('id', 'reference-line')

    let tip = d3
      .select('#chart')
      .append('div')
      .attr('class', 'tooltip')
      .attr('id', 'tooltip')
      .style('opacity', 0)
  }

  created() {
    this.resizeListener = () => {
      this.resize()
    }
    window.addEventListener('resize', this.resizeListener)
  }

  mounted() {
    this.resize()
    this.drawGraph()
  }

  beforeDestroy() {
    window.removeEventListener('resize', this.resizeListener)
  }
}
</script>
<style>
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

#reference-line {
  fill: none;
  stroke: black;
  stroke-width: 0.5px;
  stroke-dasharray: 5 5;
}

#chart {
  position: relative;
}
</style>
