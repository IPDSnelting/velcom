<template>
  <v-card flat outlined ref="graph-card" class>
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
import { Datapoint, Commit } from '@/store/types'

@Component
export default class ComparisonGraph extends Vue {
  @Prop({})
  metric!: string

  private width: number = 900
  private height: number = 500

  private svg: any = null

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

  private innerWidth: number = this.width - this.margin.left - this.margin.right
  private innerHeight: number =
    this.height - this.margin.top - this.margin.bottom

  private timeFormat: any = d3.timeFormat('%Y-%m-%d')
  private valueFormat: any = d3.format('<.4')

  get interpretation() {
    return vxm.repoComparisonModule.interpretation
  }

  get unit() {
    return vxm.repoComparisonModule.unit
  }

  get minTimestamp(): any {
    return vxm.repoComparisonModule.startDate.getTime()
  }

  get maxTimestamp(): any {
    return vxm.repoComparisonModule.stopDate.getTime()
  }

  get xScale(): any {
    return d3
      .scaleTime()
      .domain([this.minTimestamp, this.maxTimestamp])
      .range([0, this.innerWidth])
  }

  get datapoints(): { [key: string]: Datapoint[] } {
    return vxm.repoComparisonModule.allDatapoints
  }

  get repos(): string[] {
    return Array.from(Object.keys(this.datapoints))
  }

  get colorById(): (repoID: string) => string {
    return (repoID: string) => {
      let index: number = vxm.repoModule.repoIndex(repoID)
      return vxm.colorModule.colorByIndex(index)
    }
  }

  get valueRange(): { min: number; max: number } {
    let min: number = Number.POSITIVE_INFINITY
    let max: number = Number.NEGATIVE_INFINITY

    this.repos.forEach((repoID: string) => {
      this.datapoints[repoID].forEach(datapoint => {
        min = Math.min(min, datapoint.value)
        max = Math.max(max, datapoint.value)
      })
    })
    return { min: min, max: max }
  }

  get yScale() {
    if (this.interpretation === 'LESS_IS_BETTER') {
      return d3
        .scaleLinear()
        .domain([this.valueRange.min, this.valueRange.max])
        .range([0, this.innerHeight])
    } else {
      return d3
        .scaleLinear()
        .domain([this.valueRange.min, this.valueRange.max])
        .range([this.innerHeight, 0])
    }
  }

  get yLabel(): string {
    if (this.metric) {
      return this.metric + ' in ' + this.unit
    } else {
      return 'please select benchmark and metric'
    }
  }

  @Watch('datapoints')
  @Watch('minTimestamp')
  @Watch('maxTimestamp')
  drawGraph() {
    this.svg.selectAll('*').remove()
    this.drawXAxis()
    this.drawYAxis()

    this.repos.forEach((repoID: string) => {
      this.drawDatapoints(repoID)
    })
  }

  drawXAxis() {
    this.svg
      .append('g')
      .attr('class', 'axis')
      .attr('transform', 'translate(0,' + this.innerHeight + ')')
      .call(d3.axisBottom(this.xScale).tickFormat(this.timeFormat))
      .selectAll('text')
      .attr('transform', 'translate(-10,10)rotate(-45)')
      .style('text-anchor', 'end')
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

  drawDatapoints(repoID: string) {
    let repoGroup = this.svg.append('g').attr('id', repoID)

    this.datapoints[repoID].forEach(datapoint => {
      let date: any = datapoint.commit.authorDate
      if (date) {
        repoGroup
          .append('circle')
          .attr('class', 'datapoint')
          .attr('fill', this.colorById(repoID))
          .attr('stroke', this.colorById(repoID))
          .attr('r', 5)
          .attr('cx', () => {
            return this.xScale(date * 1000)
          })
          .attr('cy', () => {
            return this.yScale(datapoint.value)
          })
      }
    })
  }

  mounted() {
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

    this.drawXAxis()
    this.drawYAxis()
  }
}
</script>
<style>
.axis text {
  font-family: Roboto;
  font-size: 12px;
}
</style>
