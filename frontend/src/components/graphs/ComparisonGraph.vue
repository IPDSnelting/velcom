<template>
  <v-card flat outlined ref="graph-card">
    <div id="comparison-graph"></div>
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop, Watch } from 'vue-property-decorator'
import { vxm } from '@/store/index'
import * as d3 from 'd3'
import { Run, MeasurementID, Measurement } from '@/store/types'

@Component
export default class ComparisonChart extends Vue {
  @Prop({ default: 960 })
  width!: number

  @Prop({ default: 500 })
  height!: number

  @Prop({ default: null })
  measurement!: MeasurementID

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
    bottom: 30
  }

  private innerWidth: number = this.width - this.margin.left - this.margin.right
  private innerHeight: number =
    this.height - this.margin.top - this.margin.bottom

  private timeFormat: any = d3.timeFormat('%Y-%m-%d')

  private line: any = d3
    .line()
    .x((run: any) => {
      return this.xScale((Math.abs(run.startTime) % 1.8934156e9) * 1000)
    })
    .y((run: any) => {
      return this.yScale(Math.abs(run.measurements[0].value))
    })

  get selectedRepos(): string[] {
    return vxm.repoComparisonModule.selectedRepos
  }

  get datapointsByRepoID(): (repoID: string) => Run[] {
    return (repoID: string) => vxm.repoComparisonModule.runsByRepoID(repoID)
  }

  get allRuns(): { [key: string]: Run[] } {
    return vxm.repoComparisonModule.allRuns
  }

  get allDatapoints(): Run[] {
    let datapoints: Run[] = []

    Array.from(Object.keys(this.allRuns)).forEach((repoID: string) => {
      datapoints = datapoints.concat(this.allRuns[repoID])
    })
    return datapoints
  }

  get allValues(): any[] {
    let values: number[] = []
    this.allDatapoints.forEach(run => {
      if (run.measurements) {
        let measurement = run.measurements[0]
        if (measurement.value) {
          values.push(Math.abs(measurement.value))
        }
      }
    })
    return values
  }

  get minVal(): any {
    return d3.min(this.allValues)
  }

  get maxVal(): any {
    return d3.max(this.allValues)
  }

  get minDate(): any {
    let min = d3.min(this.allDatapoints, function(d) {
      return (Math.abs(d.startTime) % 1.8934156e9) * 1000
    })
    return min
  }

  get maxDate(): any {
    let max = d3.max(this.allDatapoints, function(d) {
      return (Math.abs(d.startTime) % 1.8934156e9) * 1000
    })
    return max
  }

  get yScale() {
    return d3
      .scaleLinear()
      .domain([this.minVal, this.maxVal])
      .range([this.innerHeight, 0])
  }

  get xScale() {
    return d3
      .scaleTime()
      .domain([this.minDate, this.maxDate])
      .range([0, this.innerWidth])
  }

  get colorById(): (repoID: string) => string {
    return (repoID: string) => {
      let index: number = vxm.repoModule.repoIndex(repoID)
      return vxm.colorModule.colorByIndex(index)
    }
  }

  @Watch('allDatapoints')
  drawGraph() {
    this.svg.selectAll('*').remove()
    this.drawXAxis()
    this.drawYAxis()
    Array.from(Object.keys(this.allRuns)).forEach((repoID: string) => {
      this.drawDatapoints(repoID)
    })
  }

  drawXAxis() {
    this.svg
      .append('g')
      .attr('transform', 'translate(0,' + this.innerHeight + ')')
      .call(
        d3.axisBottom(this.xScale).ticks(d3.timeYear.every(10))
        // .tickFormat(this.timeFormat)
      )
  }

  drawYAxis() {
    this.svg.append('g').call(d3.axisLeft(this.yScale))
  }

  drawDatapoints(repoID: string) {
    let repoGroup = this.svg.append('g').attr('id', repoID)

    this.allRuns[repoID].forEach(run => {
      if (run.measurements && run.measurements[0].value) {
        let value = Math.abs(run.measurements[0].value)

        repoGroup
          .append('circle')
          .attr('fill', this.colorById(repoID))
          .attr('stroke', this.colorById(repoID))
          .attr('r', 5)
          .attr('cx', () => {
            return this.xScale((Math.abs(run.startTime) % 1.8934156e9) * 1000)
          })
          .attr('cy', () => {
            return this.yScale(value)
          })
      }
    })
  }

  mounted() {
    this.svg = d3
      .select('#comparison-graph')
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
  }
}
</script>
