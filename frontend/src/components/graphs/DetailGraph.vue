<template>
  <v-card ref="graph-card" flat outlined>
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

  get innerWidth() {
    return this.width - this.margin.left - this.margin.right
  }

  get innerHeight() {
    return this.height - this.margin.top - this.margin.bottom
  }

  private valueFormat: any = d3.format('<.4')
  private lastValue: number = 0

  get datapoints(): { commit: Commit; comparison: CommitComparison }[] {
    let selectedRepo: string = vxm.repoDetailModule.selectedRepoId
    return vxm.repoDetailModule.historyForRepoId(selectedRepo)
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
    let interpretation: any = 'NEUTRAL'
    // pick the first interpretation that exists... wow, that's ugly
    this.datapoints.forEach(
      (datapoint: { commit: Commit; comparison: CommitComparison }) => {
        let wantedMeasurement = this.wantedMeasurementForDatapoint(datapoint)
        if (wantedMeasurement !== undefined) {
          interpretation = wantedMeasurement.interpretation
        }
      }
    )
    return interpretation
  }

  get unit() {
    let unit: string | null = null
    // pick the first unit that exists... wow, that's just as ugly
    this.datapoints.forEach(
      (datapoint: { commit: Commit; comparison: CommitComparison }) => {
        let wantedMeasurement = this.wantedMeasurementForDatapoint(datapoint)
        if (wantedMeasurement !== undefined && wantedMeasurement.unit) {
          unit = wantedMeasurement.unit
        }
      }
    )
    return unit
  }

  get xScale(): any {
    return d3
      .scaleLinear()
      .domain([0, this.amount])
      .range([0, this.innerWidth])
  }

  get colorById(): (repoID: string) => string {
    return (repoID: string) => {
      let index: number = vxm.repoModule.repoIndex(repoID)
      return vxm.colorModule.colorByIndex(index)
    }
  }

  x(datapoint: any) {
    return this.xScale(this.datapoints.indexOf(datapoint) + 1)
  }

  y(datapoint: any) {
    let wantedMeasurement = this.wantedMeasurementForDatapoint(datapoint)
    if (wantedMeasurement !== undefined && wantedMeasurement.value) {
      this.lastValue = wantedMeasurement.value
      return this.yScale(wantedMeasurement.value)
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

  @Watch('datapoints')
  @Watch('amount')
  drawGraph() {
    this.svg.selectAll('*').remove()

    if (
      this.metric !== '' &&
      this.valueRange.min !== Number.POSITIVE_INFINITY
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
    this.svg
      .append('g')
      .attr('class', 'axis')
      .attr('transform', 'translate(0,' + this.innerHeight + ')')
      .call(d3.axisBottom(this.xScale))
  }

  drawYAxis() {
    this.svg
      .append('g')
      .attr('class', 'axis')
      .call(d3.axisLeft(this.yScale).tickFormat(this.valueFormat))

    this.svg
      .append('text')
      .attr('text-anchor', 'end')
      .attr('transform', 'rotate(-90)')
      .attr('y', -this.margin.left + 20)
      .attr('x', -this.innerHeight + 300)
      .text(this.yLabel)
  }

  drawDatapoints() {
    /* don't draw data without a visualisation within this context
    (would cause d3 to draw data points at the top of the chart...) */
    if (this.valueRange.min === Number.POSITIVE_INFINITY) {
      return
    }
    let graph = this.svg.append('g').attr('id', 'graph')

    // draw the connecting lines
    graph
      .append('path')
      .attr('d', this.line(this.datapoints))
      .attr('stroke', this.colorById(this.selectedRepo))
      .attr('stroke-width', 2)
      .attr('fill', 'none')

    // draw the scatterplot and add tooltips
    graph
      .selectAll('dot')
      .data(this.datapoints)
      .enter()
      .append('circle')
      .attr('class', 'datapoint')
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
      .data(this.datapoints)
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
    this.tooltip.style('opacity', 0.8)
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
      htmlMessage =
        'Commit ' +
        d.commit.hash +
        '<br />author:' +
        d.commit.author +
        '<br> authored on ' +
        formatDateUTC(d.commit.authorDate) +
        ',<br />exact value: ' +
        this.valueFormat(wantedMeasurement.value) +
        ' ' +
        this.unit
    } else if (d.commit.authorDate) {
      htmlMessage =
        'Commit ' +
        d.commit.hash +
        '<br />author:' +
        d.commit.author +
        '<br> authored on ' +
        formatDateUTC(d.commit.authorDate) +
        '<br />This commit was not benchmarked successfully for this metric.'
    } else {
      htmlMessage =
        'Commit ' + d.commit.hash + '<br />author:' + d.commit.author
    }
    this.tooltip
      .html(htmlMessage)
      .style('left', d3.mouse(n[i])[0] + 90 + 'px')
      .style('top', d3.mouse(n[i])[1] + 90 + 'px')
      .style('display', 'inline-block')
  }

  mouseleave(d: any) {
    this.tooltip
      .transition()
      .duration(500)
      .style('opacity', 0)
  }

  updateYourself() {
    d3.select('#svg-container')
      .selectAll('*')
      .remove()

    this.svg = d3
      .select('#svg-container')
      .append('svg')
      .attr('width', this.width)
      .attr('height', this.height)
      .attr('align', 'end')
      .attr('justify', 'end')
      .append('g')
      .attr(
        'transform',
        'translate(' + this.margin.left + ',' + this.margin.top + ')'
      )

    this.tooltip = d3
      .select('#svg-container')
      .append('div')
      .style('opacity', 0)
      .attr('class', 'tooltip')
      .style('position', 'absolute')
      .style('padding', '5px')
      .style('border-radius', '5px')
      .style('background-color', 'black')
      .style('color', 'white')
      .style('text-align', 'center')
      .style('font-family', 'Roboto')
      .style('font-size', '14px')

    this.drawGraph()
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
</style>
