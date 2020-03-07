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
import { vxm } from '../../store'
import { formatDateUTC } from '../../util/TimeUtil'
import { Datapoint } from '../../store/types'

@Component({})
export default class NewComparisonGraph extends Vue {
  @Prop({ default: true })
  beginYAtZero!: boolean

  // dimensions

  private width: number = 0
  private height: number = 0
  private datapointWidth: number = 50

  private margin: {
    left: number
    right: number
    top: number
    bottom: number
    between: number
  } = {
    left: 100,
    right: 50,
    top: 10,
    bottom: 100,
    between: 50
  }

  private get innerWidth(): number {
    return this.width - this.margin.left - this.margin.right
  }

  private get innerHeight(): number {
    return this.height - this.margin.top - this.margin.bottom
  }

  private get focusHeight(): number {
    return 0.8 * (this.innerHeight - this.margin.between)
  }

  private get contextHeight(): number {
    return this.innerHeight - this.margin.between - this.focusHeight
  }

  // retrieving and interpreting datapoints

  private get datapoints(): { [key: string]: Datapoint[] } {
    return vxm.repoComparisonModule.allDatapoints
  }

  private get repos(): string[] {
    return Array.from(Object.keys(this.datapoints))
  }

  private get metric(): string {
    return vxm.repoComparisonModule.selectedMetric
  }

  private get unit(): string {
    return vxm.repoComparisonModule.unit
  }

  private get minTimestamp(): number {
    return vxm.repoComparisonModule.startDate.getTime()
  }

  private get maxTimestamp(): number {
    return vxm.repoComparisonModule.stopDate.getTime() + 1000 * 60 * 60 * 24
  }

  private context: number[] = [this.minTimestamp, this.maxTimestamp]
  private focus: number[] = this.context

  get allDatapoints(): Datapoint[] {
    return Array.from(Object.values(this.datapoints)).reduce(
      (acc, next) => acc.concat(next),
      []
    )
  }

  private get minContextVal(): number | undefined {
    return d3.min(this.allDatapoints, (d: Datapoint) => {
      return d.value
    })
  }

  private get maxContextVal(): number | undefined {
    return d3.max(this.allDatapoints, (d: Datapoint) => {
      return d.value
    })
  }

  private get minFocusVal(): number | undefined {
    return d3.min(this.allDatapoints, (d: Datapoint) => {
      let date: number = d.commit.authorDate ? d.commit.authorDate * 1000 : 0
      return this.focus[0] <= date && date <= this.focus[1] ? d.value : NaN
    })
  }

  private get maxFocusVal(): number | undefined {
    return d3.max(this.allDatapoints, (d: Datapoint) => {
      let date: number = d.commit.authorDate ? d.commit.authorDate * 1000 : 0
      return this.focus[0] <= date && date <= this.focus[1] ? d.value : NaN
    })
  }

  private get dataAvailable(): boolean {
    return this.metric !== '' && this.maxContextVal !== undefined
  }

  // scales and axes
  private xScale(domain: number[]): d3.ScaleTime<number, number> {
    return d3
      .scaleTime()
      .domain(domain)
      .range([0, this.innerWidth])
  }

  private yScale(
    domain: number[],
    height: number
  ): d3.ScaleLinear<number, number> {
    let min: number = !this.beginYAtZero && domain[0] ? domain[0] : 0
    let max: number = domain[1] || 0
    return d3
      .scaleLinear()
      .domain([min, max])
      .range([height, 0])
  }

  private x(domain: number[], datapoint: Datapoint): number {
    return datapoint.commit.authorDate
      ? this.xScale(domain)(datapoint.commit.authorDate * 1000)
      : 0
  }
  private y(domain: number[], datapoint: Datapoint, height: number): number {
    return this.yScale(domain, height)(datapoint.value)
  }

  private timeFormat: any = d3.timeFormat('%Y-%m-%d')
  private valueFormat: any = d3.format('<.4')

  private get focusXAxis(): any {
    return d3.axisBottom(this.xScale(this.focus)).tickFormat(this.timeFormat)
  }

  private get contextXAxis(): any {
    return d3.axisBottom(this.xScale(this.context)).tickFormat(this.timeFormat)
  }

  private get yAxis(): any {
    let domain: number[] =
      this.minFocusVal !== undefined && this.maxFocusVal !== undefined
        ? [this.minFocusVal, this.maxFocusVal]
        : [0, 0]
    return d3
      .axisLeft(this.yScale(domain, this.focusHeight))
      .tickFormat(this.valueFormat)
  }

  get yLabel(): string {
    if (this.metric) {
      return this.unit ? this.metric + ' in ' + this.unit : this.metric
    } else {
      return '-'
    }
  }

  // interacting with the context graph via brushing, updating the foczs graph accordingly
  private get brush() {
    return d3
      .brushX()
      .extent([
        [0, 0],
        [this.innerWidth, this.contextHeight]
      ])
      .on('brush', this.brushed)
      .on('end', this.brushended)
  }
  private brushed() {
    let selection = d3.event.selection

    if (selection) {
      let newMinDate: Date = this.xScale(this.context).invert(selection[0])
      let newMaxDate: Date = this.xScale(this.context).invert(selection[1])

      let newFocusMin: number = newMinDate.getTime()
      let newFocusMax: number = newMaxDate.getTime()

      this.focus = [newFocusMin, newFocusMax]
    }
  }

  private brushended() {
    if (!d3.event.selection) {
      d3.select('#brush').call(
        this.brush.move as any,
        this.context.map(this.xScale(this.context))
      )
    }
  }

  // drawing the graph
  private graphDrawn: boolean = false

  private drawGraph() {
    if (this.dataAvailable) {
      if (!this.graphDrawn) {
        d3.select('#dataLayer').remove()
        this.defineSvgElements()
        this.graphDrawn = true
      }

      let keyFn: d3.ValueFn<any, any, string> = (d: Datapoint) => {
        return d.commit.repoID + '#' + d.commit.hash
      }

      this.repos.forEach(repoID => {
        this.drawPaths(repoID)
        this.drawDatapoints(repoID, keyFn)
        // this.appendTooltips(keyFn)
      })
      // this.setReference() */
    } else {
      if (this.graphDrawn) {
        this.graphDrawn = false
      }
      d3.select('#dataLayer').remove()

      let information: string =
        this.metric === ''
          ? '<tspan x="0" dy="1.2em">No data available.</tspan><tspan x="0" dy="1.2em">Please select benchmark and metric.</tspan>'
          : '<tspan x="0" dy="1.2em">There are no commits within the specified time period</tspan><tspan x="0" dy="1.2em"> that have been benchmarked with this metric.</tspan>'

      d3.select('#mainSvg')
        .append('g')
        .attr('id', 'dataLayer')
        .attr(
          'transform',
          'translate(' + this.margin.left + ',' + this.margin.top + ')'
        )
        .append('text')
        .attr('y', this.innerHeight / 2)
        .attr('x', -this.margin.left)
        .html(information)
        .attr('class', 'information')
    }
  }

  private drawPaths(repoID: string) {
    let focusDomain: number[] =
      this.minFocusVal !== undefined && this.maxFocusVal !== undefined
        ? [this.minFocusVal, this.maxFocusVal]
        : [0, 0]

    let contextDomain: number[] =
      this.minContextVal !== undefined && this.maxContextVal !== undefined
        ? [this.minContextVal, this.maxContextVal]
        : [0, 0]

    this.drawPath(repoID, 'focus', this.focus, focusDomain, this.focusHeight)

    this.drawPath(
      repoID,
      'context',
      this.context,
      contextDomain,
      this.contextHeight
    )
  }

  private drawPath(
    repoID: string,
    layer: string,
    xDomain: number[],
    yDomain: number[],
    height: number
  ) {
    let path: any = d3
      .select('#' + layer + 'Layer')
      .selectAll<SVGPathElement, unknown>('#' + layer + 'line_' + repoID)
      .data([this.datapoints[repoID]])
    let newPath: any = path
      .enter()
      .append('path')
      .attr('id', layer + 'line_' + repoID)
      .merge(path)
      .attr('d', this.line(xDomain, yDomain, height))
      .attr('stroke', this.colorById(repoID))
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

  get line(): (xDomain: number[], yDomain: number[], height: number) => any {
    return (xDomain: number[], yDomain: number[], height: number) =>
      d3
        .line<Datapoint>()
        .x((d: Datapoint) => {
          return this.x(xDomain, d)
        })
        .y((d: Datapoint) => {
          return this.y(yDomain, d, height)
        })
  }

  private drawDatapoints(repoID: string, keyFn: d3.ValueFn<any, any, string>) {
    let yDomain: number[] =
      this.minFocusVal !== undefined && this.maxFocusVal !== undefined
        ? [this.minFocusVal, this.maxFocusVal]
        : [0, 0]

    let datapoints: d3.Selection<
      SVGPathElement,
      Datapoint,
      d3.BaseType,
      unknown
    > = d3
      .select('#focusLayer')
      .selectAll<SVGPathElement, unknown>('.datapoint')
      .data(this.datapoints[repoID], keyFn)

    let newDatapoints = datapoints
      .enter()
      .append('path')
      .attr('class', 'datapoint')
      .attr('id', (d: Datapoint) => repoID + '_' + d.commit.hash)
      .merge(datapoints)
      .attr(
        'd',
        d3
          .symbol()
          .type(d3.symbolCircle)
          .size(this.datapointWidth)
      )
      .attr(
        'transform',
        (d: Datapoint) =>
          'translate(' +
          this.x(this.focus, d) +
          ', ' +
          this.y(yDomain, d, this.focusHeight) +
          ')'
      )
      .attr('fill', (d: Datapoint) => this.colorById(repoID))
      .attr('stroke', (d: Datapoint) => this.colorById(repoID))
      .attr('stroke-width', 2)
      .attr('opacity', 1)
      .style('cursor', 'pointer')

    datapoints
      .exit()
      .transition()
      .attr('opacity', 0)
      .attr('width', 0)
      .remove()
  }

  get colorById(): (repoID: string) => string {
    return (repoID: string) => {
      let index: number = vxm.repoModule.repoIndex(repoID)
      return vxm.colorModule.colorByIndex(index)
    }
  }

  // updating
  private resizeListener: () => void = () => {}

  private resize() {
    let chart = d3.select('#chart').node() as HTMLElement
    this.width = chart ? chart.getBoundingClientRect().width : 900
    this.height = this.width * 0.6

    d3.select('#contextLayer').attr(
      'transform',
      'translate(0,' + (this.focusHeight + this.margin.between) + ')'
    )

    d3.select('#contextLayer')
      .select('#brush')
      .remove()
    d3.select('#contextLayer')
      .append('g')
      .attr('id', 'brush')
      .call(this.brush)
      .call(this.brush.move, this.focus.map(this.xScale(this.context)))

    d3.select('#mainSvg')
      .select('#focusClipRect')
      .attr('width', this.innerWidth)
      .attr('height', this.focusHeight + 2 * this.datapointWidth)
    d3.select('#mainSvg')
      .select('#contextClipRect')
      .attr('width', this.innerWidth)
      .attr('height', this.contextHeight + 2 * this.datapointWidth)

    this.updateData()
  }

  @Watch('datapoints')
  @Watch('minTimestamp')
  @Watch('maxTimestamp')
  @Watch('beginYAtZero')
  @Watch('focus')
  private updateData() {
    console.log('updating data')
    d3.select('#yLabel').text(this.yLabel)
    this.updateAxes()
    this.drawGraph()
  }

  private updateAxes() {
    ;(d3.select('#focusXAxis') as d3.Selection<
      SVGGElement,
      unknown,
      HTMLElement,
      any
    >)
      .attr('transform', 'translate(0,' + this.focusHeight + ')')
      .call(this.focusXAxis)
    ;(d3.select('#contextXAxis') as d3.Selection<
      SVGGElement,
      unknown,
      HTMLElement,
      any
    >)
      .attr('transform', 'translate(0,' + this.innerHeight + ')')
      .call(this.contextXAxis)
    ;(d3.select('#yAxis') as d3.Selection<
      SVGGElement,
      unknown,
      HTMLElement,
      any
    >).call(this.yAxis)

    d3.select('#yLabel')
      .attr('y', -this.margin.left + 30)
      .attr('x', -this.focusHeight / 2)
  }

  private defineSvgElements() {
    d3.select('#mainSvg')
      .append('g')
      .attr('id', 'dataLayer')
      .attr(
        'transform',
        'translate(' + this.margin.left + ',' + this.margin.top + ')'
      )

    d3.select('#mainSvg')
      .append('clipPath')
      .attr('id', 'focusClip')
      .append('rect')
      .attr('id', 'focusClipRect')
      .attr('y', -this.datapointWidth)
      .attr('width', this.innerWidth)
      .attr('height', this.focusHeight + 2 * this.datapointWidth)

    d3.select('#mainSvg')
      .append('clipPath')
      .attr('id', 'contextClip')
      .append('rect')
      .attr('id', 'contextClipRect')
      .attr('y', -this.datapointWidth)
      .attr('width', this.innerWidth)
      .attr('height', this.contextHeight + 2 * this.datapointWidth)

    d3.select('#dataLayer')
      .append('g')
      .attr('id', 'focusLayer')
      .attr('clip-path', 'url(#focusClip)')

    d3.select('#dataLayer')
      .append('g')
      .attr('id', 'contextLayer')
      .attr(
        'transform',
        'translate(0,' + (this.focusHeight + this.margin.between) + ')'
      )
      .attr('clip-path', 'url(#contextClip)')

    d3.select('#contextLayer')
      .append('g')
      .attr('id', 'brush')
      .call(this.brush)
      .call(this.brush.move, this.context.map(this.xScale(this.context)))

    d3.select('#dataLayer')
      .append('g')
      .attr('class', 'axis')
      .attr('id', 'focusXAxis')
      .attr('transform', 'translate(0,' + this.focusHeight + ')')
      .call(this.focusXAxis)

    d3.select('#dataLayer')
      .append('g')
      .attr('class', 'axis')
      .attr('id', 'yAxis')
      .call(this.yAxis)

    d3.select('#dataLayer')
      .append('g')
      .attr('class', 'axis')
      .attr('id', 'contextXAxis')
      .attr('transform', 'translate(0,' + this.innerHeight + ')')
      .call(this.contextXAxis)

    d3.select('#dataLayer')
      .append('text')
      .attr('id', 'yLabel')
      .attr('text-anchor', 'middle')
      .attr('transform', 'rotate(-90)')
      .attr('y', -this.margin.left + 30)
      .attr('x', -this.focusHeight / 2)
      .text(this.yLabel)
  }

  // initializing
  created() {
    this.resizeListener = () => {
      this.resize()
    }
    window.addEventListener('resize', this.resizeListener)
  }

  mounted() {
    d3.select('#mainSvg')
      .attr('width', '100%')
      .attr('height', '100%')
      .attr('align', 'end')
      .attr('justify', 'end')
    this.resize()
  }

  beforeDestroy() {
    window.removeEventListener('resize', this.resizeListener)
  }
}
</script>
