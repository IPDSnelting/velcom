<template>
  <v-card ref="graph-card">
    <v-container>
      <v-row align="center" justify="center">
        <v-col>
          <div id="svg-container"></div>
        </v-col>
      </v-row>
    </v-container>
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop, Watch } from 'vue-property-decorator'
import { vxm } from '@/store/index'
import * as d3 from 'd3'
import {
  Commit,
  CommitComparison,
  MeasurementID,
  Measurement
} from '@/store/types'
import { formatDateUTC } from '@/util/TimeUtil'

@Component
export default class DetailGraph extends Vue {
  @Prop({})
  benchmark!: string

  @Prop({ default: null })
  metric!: string

  @Prop({})
  amount!: number

  @Prop({ default: true })
  beginYAtZero!: boolean

  private resizeListener: () => void = () => {}

  get selectedRepo(): string {
    return vxm.repoDetailModule.selectedRepoId
  }

  get selectedMeasurement(): MeasurementID {
    return new MeasurementID(this.benchmark, this.metric)
  }

  created() {
    this.resizeListener = () => {
      this.resize()
      this.updateYourself()
    }
    window.addEventListener('resize', this.resizeListener)
  }

  beforeDestroy() {
    window.removeEventListener('resize', this.resizeListener)
  }

  resize() {
    if (!this.$refs['graph-card']) {
      return
    }
    let card = (this.$refs['graph-card'] as Vue).$el as HTMLElement
    if (!card) {
      return
    }

    this.width = card.getBoundingClientRect().width - 40
    this.height =
      this.width > 1000 ? this.width * (3 / 7) : this.width * (9 / 16)
  }

  private width: number = 0
  private height: number = 0

  private svg: any = null
  private tooltip: any = null
  private brushArea: any = null
  private zooming: boolean = false

  private get brush() {
    return d3
      .brushX()
      .extent([
        [0, 0],
        [this.innerWidth, this.innerHeight]
      ])
      .on('end', this.brushed)
  }

  brushed() {
    let selection = d3.event.selection
    let newMin: number = Math.floor(this.xScale.invert(selection[0]))
    let newMax = Math.floor(this.xScale.invert(selection[1]))
    if (selection) {
      let newAmount: number = this.amount - newMin - (this.amount - newMax)
      this.zooming = true
      this.$emit('selectionChanged', newAmount, newMin)
    }
  }

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

  private valueFormat: any = d3.format('<.4')
  private lastValue: number = 0

  xAxisFormat(d: any) {
    if (d % 1 === 0) {
      return d3.format('.0f')(d)
    } else {
      return ''
    }
  }

  get datapoints(): { commit: Commit; comparison: CommitComparison }[] {
    let selectedRepo: string = vxm.repoDetailModule.selectedRepoId
    return vxm.repoDetailModule.historyForRepoId(selectedRepo)
  }

  get datapointsBetweenAxes() {
    return this.datapoints.slice(0, this.amount)
  }

  // prettier-ignore
  get wantedMeasurementForDatapoint(): (datapoint: {
    commit: Commit
    comparison: CommitComparison
  }) => Measurement | undefined {
    return (datapoint: { commit: Commit; comparison: CommitComparison }) => {
      if (
        datapoint.comparison.second &&
        datapoint.comparison.second.measurements
      ) {
        let wantedMeasurement:
          | Measurement
          | undefined = datapoint.comparison.second.measurements.find(it =>
            it.id.equals(this.selectedMeasurement)
          )
        return wantedMeasurement
      }
      return undefined
    }
  }

  get interpretation() {
    for (const datapoint of this.datapoints) {
      let wantedMeasurement = this.wantedMeasurementForDatapoint(datapoint)
      if (wantedMeasurement !== undefined) {
        return wantedMeasurement.interpretation
      }
    }
    return 'NEUTRAL'
  }

  get unit() {
    for (const datapoint of this.datapoints) {
      let wantedMeasurement = this.wantedMeasurementForDatapoint(datapoint)
      if (wantedMeasurement !== undefined && wantedMeasurement.unit) {
        return wantedMeasurement.unit
      }
    }
    return null
  }

  get firstSuccessful(): number {
    for (const datapoint of this.datapoints) {
      let wantedMeasurement = this.wantedMeasurementForDatapoint(datapoint)
      if (
        wantedMeasurement !== undefined &&
        wantedMeasurement.successful &&
        wantedMeasurement.value
      ) {
        return wantedMeasurement.value
      }
    }
    // if every commit failed, place them on the center line
    return this.height / 2
  }

  get xScale(): any {
    return d3
      .scaleLinear()
      .domain([0.5, this.amount + 0.99])
      .range([0, this.innerWidth])
  }

  get colorById(): (repoID: string) => string {
    return (repoID: string) => {
      let index: number = vxm.repoModule.repoIndex(repoID)
      return vxm.colorModule.colorByIndex(index)
    }
  }

  x(datapoint: any) {
    return this.xScale(
      this.datapoints.length - this.datapoints.indexOf(datapoint)
    )
  }

  y(datapoint: any) {
    let wantedMeasurement = this.wantedMeasurementForDatapoint(datapoint)
    if (wantedMeasurement !== undefined && wantedMeasurement.value) {
      this.lastValue = wantedMeasurement.value
      return this.yScale(wantedMeasurement.value)
    }
    if (this.datapoints.indexOf(datapoint) === 0) {
      this.lastValue = this.firstSuccessful
    }
    return this.yScale(this.lastValue)
  }

  get line(): any {
    return d3
      .line()
      .x((datapoint: any) => {
        return this.x(datapoint)
      })
      .y((datapoint: any) => {
        return this.y(datapoint)
      })
  }

  get valueRange(): { min: number; max: number } {
    let min: number = Number.POSITIVE_INFINITY
    let max: number = Number.NEGATIVE_INFINITY

    this.datapoints.forEach(
      (datapoint: { commit: Commit; comparison: CommitComparison }) => {
        let wantedMeasurement = this.wantedMeasurementForDatapoint(datapoint)
        if (wantedMeasurement !== undefined && wantedMeasurement.value) {
          min = Math.min(min, wantedMeasurement.value)
          max = Math.max(max, wantedMeasurement.value)
        }
      }
    )
    min = this.beginYAtZero ? 0 : min
    return { min: min, max: max }
  }
  get yScale() {
    if (this.interpretation === 'LESS_IS_BETTER') {
      return d3
        .scaleLinear()
        .domain([this.valueRange.min, this.valueRange.max])
        .range([this.innerHeight, 0])
    } else {
      return d3
        .scaleLinear()
        .domain([this.valueRange.min, this.valueRange.max])
        .range([0, this.innerHeight])
    }
  }

  get yLabel(): string {
    if (this.metric) {
      return this.unit ? this.metric + ' in ' + this.unit : this.metric
    } else {
      return '-'
    }
  }

  sleep(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms))
  }

  @Watch('datapoints')
  @Watch('amount')
  @Watch('beginYAtZero')
  async updateYourself() {
    if (this.zooming) {
      await this.sleep(500)
      this.zooming = false
    }
    d3.select('#svg-container')
      .selectAll('*')
      .remove()

    this.svg = d3
      .select('#svg-container')
      .append('svg')
      .attr('id', 'mainSVG')
      .attr('width', this.width)
      .attr('height', this.height)
      .attr('align', 'end')
      .attr('justify', 'end')
      .append('g')
      .attr(
        'transform',
        'translate(' + this.margin.left + ',' + this.margin.top + ')'
      )

    this.brushArea = d3
      .select('#mainSVG')
      .append('g')
      .attr('id', 'brushArea')
      .attr(
        'transform',
        'translate(' + this.margin.left + ',' + this.margin.top + ')'
      )
      .call(this.brush)

    this.drawGraph()

    this.tooltip = d3
      .select('#svg-container')
      .append('div')
      .style('opacity', 0)
      .attr('id', 'tooltip')
      .attr('class', 'tooltip')
  }

  drawGraph() {
    this.svg.selectAll('*').remove()
    console.log(this.valueRange.max)

    if (
      this.metric !== '' &&
      this.valueRange.max !== Number.NEGATIVE_INFINITY
    ) {
      this.drawXAxis()
      this.drawYAxis()
      this.drawDatapoints()
    } else {
      let information: string =
        this.metric === ''
          ? 'No data available. Please select benchmark and metric.'
          : 'The requested commits have not been benchmarked with this metric.'

      this.svg
        .append('text')
        .attr('y', this.height / 2)
        .attr('x', this.margin.left)
        .text(information)
        .style('text-align', 'center')
        .style('font-family', 'Roboto')
        .style('font-size', '18px')
        .style('fill', 'grey')
    }
  }

  drawXAxis() {
    return this.svg
      .append('g')
      .attr('class', 'axis')
      .attr('transform', 'translate(0,' + this.innerHeight + ')')
      .call(d3.axisBottom(this.xScale).tickFormat(this.xAxisFormat))
  }

  drawYAxis() {
    this.svg
      .append('g')
      .attr('class', 'axis')
      .call(d3.axisLeft(this.yScale).tickFormat(this.valueFormat))

    this.svg
      .append('text')
      .attr('text-anchor', 'middle')
      .attr('transform', 'rotate(-90)')
      .attr('y', -this.margin.left + 20)
      .attr('x', -this.innerHeight / 2)
      .text(this.yLabel)
  }

  drawDatapoints() {
    // draw the connecting line
    this.brushArea
      .append('path')
      .attr('d', this.line(this.datapointsBetweenAxes))
      .attr('stroke', this.colorById(this.selectedRepo))
      .attr('stroke-width', 2)
      .attr('fill', 'none')
      .attr('pointer-events', 'none')

    // draw the scatterplot and add tooltips
    this.brushArea
      .selectAll('dot')
      .data(this.datapointsBetweenAxes)
      .enter()
      .append('circle')
      .attr('class', 'datapoint')
      .attr('z-index', 20) // lift it to top to properly capture mouse events
      .attr('fill', (d: any) => this.datapointColor(d))
      .attr('stroke', (d: any) => this.strokeColor(d))
      .attr('stroke-width', 2)
      .attr('r', 4)
      .attr('cx', (d: any) => {
        return this.x(d)
      })
      .attr('cy', (d: any) => {
        return this.y(d)
      })
      .style('cursor', 'pointer')
      .data(this.datapointsBetweenAxes)
      .on('mouseover', this.mouseover)
      .on('mousemove', this.mousemove)
      .on('mouseleave', this.mouseleave)
      .on('click', (d: any) => {
        this.$router.push({
          name: 'commit-detail',
          params: { repoID: this.selectedRepo, hash: d.commit.hash }
        })
      })
  }

  datapointColor(d: any): string {
    let wantedMeasurement = this.wantedMeasurementForDatapoint(d)
    if (wantedMeasurement && wantedMeasurement.successful) {
      return this.colorById(this.selectedRepo)
    }
    return 'white'
  }

  strokeColor(d: any): string {
    let wantedMeasurement = this.wantedMeasurementForDatapoint(d)
    if (wantedMeasurement && wantedMeasurement.successful) {
      return this.colorById(this.selectedRepo)
    }
    return 'grey'
  }

  mouseover(d: any) {
    // We need a transition here to overwrite possible exit transition
    // happening in parallel
    // If we don't do that, this update will be lost and no tooltip
    // displayed
    this.tooltip
      .transition()
      .duration(300)
      .style('opacity', 0.8)
      .style('visibility', 'visible')
  }
  mousemove(
    d: { commit: Commit; comparison: CommitComparison },
    i: any,
    n: any
  ) {
    let wantedMeasurement = this.wantedMeasurementForDatapoint(d)
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
            <td>Exact value</td>
            <td>This commit was not benchmarked successfully for this metric.</td>
          </tr>
       </table>
      `
    } else {
      htmlMessage =
        'Commit ' + d.commit.hash + '<br />author:' + d.commit.author
    }
    if (this.datapoints.indexOf(d) < this.datapoints.length / 2) {
      d3.select('#tooltip')
        .html(htmlMessage)
        .style('left', d3.mouse(n[i])[0] + 90 + 'px')
        .style('top', d3.mouse(n[i])[1] + 90 + 'px')
        .style('display', 'inline-block')
    } else {
      d3.select('#tooltip')
        .html(htmlMessage)
        .style('left', d3.mouse(n[i])[0] - 90 + 'px')
        .style('top', d3.mouse(n[i])[1] + 90 + 'px')
        .style('display', 'inline-block')
    }
  }

  mouseleave(d: any) {
    this.tooltip
      .transition()
      .duration(500)
      .style('opacity', 0)
      .style('visibility', 'hidden')
  }

  mounted() {
    this.resize()
    this.updateYourself()
  }
}
</script>
<style>
.axis text {
  font-family: Roboto;
  font-size: 12px;
}

.tooltip {
  font-size: 10pt;
  position: absolute;
  padding: 5px;
  border-radius: 5px;
  background-color: black;
  color: white;
  text-align: center;
  font-family: 'Roboto';
}

.tooltip-table tr td {
  text-align: left;
}

.tooltip-table tr td:first-child {
  padding-right: 2em;
}
</style>
