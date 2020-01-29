<template>
  <v-card flat outlined ref="graph-card">
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
import { formatDateUTC } from '@/util/TimeUtil'

@Component
export default class ComparisonGraph extends Vue {
  @Prop({})
  metric!: string

  private resizeTimeout: number | undefined
  private resizeListener: () => void = () => {}

  created() {
    this.resizeListener = () => {
      this.debounce(this.resize, 1000)()
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

  debounce(func: Function, wait: number) {
    return () => {
      if (this.resizeTimeout) {
        return
      }
      let context = this
      let args = arguments
      clearTimeout(this.resizeTimeout)
      this.resizeTimeout = setTimeout(() => {
        this.resizeTimeout = undefined
        func.apply(context, args)
      }, wait)
    }
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

  get line(): any {
    return d3
      .line()
      .x((datapoint: any) => {
        return this.xScale(datapoint.commit.authorDate * 1000)
      })
      .y((datapoint: any) => {
        return this.yScale(datapoint.value)
      })
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
  @Watch('minTimestamp')
  @Watch('maxTimestamp')
  drawGraph() {
    this.svg.selectAll('*').remove()

    if (
      this.metric !== '' &&
      this.valueRange.min !== Number.POSITIVE_INFINITY
    ) {
      this.drawXAxis()
      this.drawYAxis()
      this.repos.forEach((repoID: string) => {
        this.drawDatapoints(repoID)
      })
    } else {
      let information: string =
        this.metric === ''
          ? 'No data available. Please select benchmark and metric.'
          : 'There are no commits within the specified time period that have been benchmarked with this metric.'

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

    // draw the connecting lines
    repoGroup
      .append('path')
      .attr('d', this.line(this.datapoints[repoID]))
      .attr('stroke', this.colorById(repoID))
      .attr('stroke-width', 2)
      .attr('fill', 'none')

    // draw the scatterplot and add tooltips
    repoGroup
      .selectAll('dot')
      .data(this.datapoints[repoID])
      .enter()
      .append('circle')
      .attr('class', 'datapoint')
      .attr('fill', this.colorById(repoID))
      .attr('stroke', this.colorById(repoID))
      .attr('r', 4)
      .attr('cx', (d: any) => {
        return this.xScale(d.commit.authorDate * 1000)
      })
      .attr('cy', (d: any) => {
        return this.yScale(d.value)
      })
      .style('cursor', 'pointer')
      .data(this.datapoints[repoID])
      .on('mouseover', this.mouseover)
      .on('mousemove', this.mousemove)
      .on('mouseleave', this.mouseleave)
      .on('click', (d: any) => {
        this.$router.push({
          name: 'commit-detail',
          params: { repoID: repoID, hash: d.commit.hash }
        })
      })
  }

  mouseover(d: any) {
    this.tooltip.style('opacity', 0.8)
  }

  mousemove(d: any, i: any, n: any) {
    if (d.commit.authorDate) {
      let truncatedValue = this.valueFormat(d.value)
      this.tooltip
        .html(
          'Commit ' +
            d.commit.hash +
            '<br> authored on ' +
            formatDateUTC(d.commit.authorDate) +
            ',<br />exact value: ' +
            truncatedValue +
            ' ' +
            this.unit
        )
        .style('left', d3.mouse(n[i])[0] + 90 + 'px')
        .style('top', d3.mouse(n[i])[1] + 60 + 'px')
        .style('display', 'inline-block')
    }
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
