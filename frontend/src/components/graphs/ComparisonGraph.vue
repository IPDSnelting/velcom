<template>
  <v-container fluid>
    <v-row align="center" justify="center">
      <v-col>
        <div id="chart" :style="{'height': this.height + 'px'}">
          <svg id="mainSvg" />
        </div>
      </v-col>
    </v-row>
    <v-row>
      <v-col>
        <datapoint-dialog
          :dialogOpen="dialogOpen"
          :selectedCommit="selectedCommit"
          @setReference="setReference"
          @removeReference="removeReference"
          @viewInDetailGraph="showInDetailGraph"
          @close="closeDialog"
        ></datapoint-dialog>
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
import { Datapoint, Commit, Repo, MeasurementID } from '../../store/types'
import ComparisonDatapointDialog from '../dialogs/ComparisonDatapointDialog.vue'
import { crosshairIcon } from '../graphs/crosshairIcon'

@Component({
  components: {
    'datapoint-dialog': ComparisonDatapointDialog
  }
})
export default class ComparisonGraph extends Vue {
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
    top: 20,
    bottom: 70,
    between: 70
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
    return vxm.repoModule.allRepos.map((repo: Repo) => repo.id)
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

  brushedArea(): number[] {
    return this.focus
  }

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
    let inFocusMin: number | undefined = d3.min(
      this.allDatapoints,
      (d: Datapoint) => {
        let date: number = d.commit.authorDate ? d.commit.authorDate * 1000 : 0
        return this.focus[0] <= date && date <= this.focus[1] ? d.value : NaN
      }
    )

    if (inFocusMin) {
      return vxm.repoComparisonModule.referenceDatapoint !== undefined &&
        vxm.repoComparisonModule.referenceDatapoint.value < inFocusMin
        ? vxm.repoComparisonModule.referenceDatapoint.value
        : inFocusMin
    }
    return vxm.repoComparisonModule.referenceDatapoint !== undefined
      ? vxm.repoComparisonModule.referenceDatapoint.value
      : 0
  }

  private get maxFocusVal(): number | undefined {
    let inFocusMax: number | undefined = d3.max(
      this.allDatapoints,
      (d: Datapoint) => {
        let date: number = d.commit.authorDate ? d.commit.authorDate * 1000 : 0
        return this.focus[0] <= date && date <= this.focus[1] ? d.value : NaN
      }
    )

    if (inFocusMax) {
      return vxm.repoComparisonModule.referenceDatapoint !== undefined &&
        vxm.repoComparisonModule.referenceDatapoint.value > inFocusMax
        ? vxm.repoComparisonModule.referenceDatapoint.value
        : inFocusMax
    }
    return vxm.repoComparisonModule.referenceDatapoint !== undefined
      ? vxm.repoComparisonModule.referenceDatapoint.value
      : 0
  }

  private get yFocusDomain(): number[] {
    return this.minFocusVal !== undefined && this.maxFocusVal !== undefined
      ? [this.minFocusVal, this.maxFocusVal]
      : [0, 0]
  }

  private get dataAvailable(): boolean {
    return this.metric !== '' && this.maxContextVal !== undefined
  }

  // scales and axes
  private xScale(domain: number[]): d3.ScaleTime<number, number> {
    return d3
      .scaleUtc()
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
      .nice()
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

  private valueFormat: any = d3.format('<.4')

  private focusXAxis(): d3.Axis<number | Date | { valueOf(): number }> {
    return d3.axisBottom(this.xScale(this.focus)).ticks(this.innerWidth / 80)
  }

  private contextXAxis(): d3.Axis<number | Date | { valueOf(): number }> {
    return d3
      .axisBottom(this.xScale(this.context))
      .ticks(this.innerWidth / 80)
      .tickSizeOuter(2)
  }

  private yAxis(): d3.Axis<number | { valueOf(): number }> {
    return d3
      .axisLeft(this.yScale(this.yFocusDomain, this.focusHeight))
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
        [0, -2],
        [this.innerWidth, this.contextHeight + 2]
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

  // interacting with data points via DatapointDialog
  private dialogOpen: boolean = false
  private selectedDatapoint: Datapoint | null = null

  private get selectedCommit(): Commit | null {
    return this.selectedDatapoint ? this.selectedDatapoint.commit : null
  }

  openDatapointMenu(datapoint: Datapoint) {
    this.selectedDatapoint = datapoint
    this.dialogOpen = true
  }

  setReference() {
    if (vxm.repoComparisonModule.referenceDatapoint !== undefined) {
      this.removeCrosshair(vxm.repoComparisonModule.referenceDatapoint)
    }
    if (this.selectedDatapoint) {
      vxm.repoComparisonModule.referenceCommit = this.selectedDatapoint.commit
    }
    if (vxm.repoComparisonModule.referenceDatapoint !== undefined) {
      this.drawReferenceLine(vxm.repoComparisonModule.referenceDatapoint)
      this.drawCrosshair(vxm.repoComparisonModule.referenceDatapoint)
    }
    this.closeDialog()
  }

  private drawReferenceLine(datapoint: Datapoint) {
    let referenceLine = d3
      .select('#focusLayer')
      .selectAll<SVGPathElement, unknown>('#referenceLine')
      .data([datapoint])

    let newReferenceLine = referenceLine
      .enter()
      .append('line')
      .attr('id', 'referenceLine')
      .merge(referenceLine as any)
      .transition()
      .duration(50)
      .delay(0)
      .attr(
        'stroke',
        vxm.userModule.darkThemeSelected ? 'lightgrey' : 'dimgrey'
      )
      .attr('x1', this.innerWidth)
      .attr('y1', this.y(this.yFocusDomain, datapoint, this.focusHeight))
      .attr('x2', 0)
      .attr('y2', this.y(this.yFocusDomain, datapoint, this.focusHeight))

    referenceLine
      .exit()
      .transition()
      .attr('opacity', 0)
      .remove()
  }

  private removeReference() {
    d3.select('#referenceLine')
      .transition()
      .attr('opacity', 0)
      .remove()
    this.removeCrosshair(vxm.repoComparisonModule.referenceDatapoint!)
    vxm.repoComparisonModule.referenceCommit = null
    this.closeDialog()
  }

  private crosshairIcon = crosshairIcon

  private drawCrosshair(datapoint: Datapoint) {
    let crosshair = d3.select(
      '#' + '_' + datapoint.commit.repoID + '_' + datapoint.commit.hash
    )

    if (crosshair.node()) {
      let crosshairRect = (crosshair.node() as SVGElement).getBoundingClientRect()
      let crosshairWidth: number = crosshairRect.width
      let crosshairHeight: number = crosshairRect.height

      d3.select(
        '#' + '_' + datapoint.commit.repoID + '_' + datapoint.commit.hash
      )
        .transition()
        .attr(
          'd',
          d3
            .symbol()
            .type(this.crosshairIcon)
            .size(this.datapointWidth)
        )
        .attr(
          'transform',
          'translate(' +
            (this.x(this.focus, datapoint) - 12) +
            ', ' +
            (this.y(this.yFocusDomain, datapoint, this.focusHeight) - 12) +
            ')'
        )
        .attr('opacity', 1)
        .attr('fill', this.colorById(datapoint.commit.repoID))
        .attr('stroke', this.colorById(datapoint.commit.repoID))
        .attr('stroke-width', 2)
    }
  }

  private removeCrosshair(datapoint: Datapoint) {
    d3.select('#' + '_' + datapoint.commit.repoID + '_' + datapoint.commit.hash)
      .attr(
        'd',
        d3
          .symbol()
          .type(d3.symbolCircle)
          .size(this.datapointWidth)
      )
      .attr(
        'transform',
        'translate(' +
          this.x(this.focus, datapoint) +
          ', ' +
          this.y(this.yFocusDomain, datapoint, this.focusHeight) +
          ')'
      )
      .attr('fill', this.colorById(datapoint.commit.repoID))
      .attr('stroke', this.colorById(datapoint.commit.repoID))
      .attr('stroke-width', 2)
  }

  private closeDialog() {
    this.dialogOpen = false
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
        return '_' + d.commit.repoID + '_' + d.commit.hash
      }

      this.repos.forEach(repoID => {
        this.drawPaths(repoID)
      })
      this.drawDatapoints(keyFn)
      this.appendTooltips(keyFn)
      this.setReference()
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
    let contextDomain: number[] =
      this.minContextVal !== undefined && this.maxContextVal !== undefined
        ? [this.minContextVal, this.maxContextVal]
        : [0, 0]

    this.drawPath(
      repoID,
      'focus',
      this.focus,
      this.yFocusDomain,
      this.focusHeight
    )

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
    if (this.datapoints[repoID]) {
      let path: any = d3
        .select('#' + layer + 'Layer')
        .selectAll<SVGPathElement, unknown>('#' + layer + 'line_' + repoID)
        .data([this.datapoints[repoID]])
      let newPath: any = path
        .enter()
        .append('path')
        .attr('id', layer + 'line_' + repoID)
        .merge(path)
        .transition()
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
    } else {
      d3.select('#' + layer + 'Layer')
        .select('#' + layer + 'line_' + repoID)
        .remove()
    }
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

  private drawDatapoints(keyFn: d3.ValueFn<any, any, string>) {
    let datapoints: d3.Selection<
      SVGPathElement,
      Datapoint,
      d3.BaseType,
      unknown
    > = d3
      .select('#focusLayer')
      .selectAll<SVGPathElement, unknown>('.datapoint')
      .data(this.allDatapoints, keyFn)

    let newDatapoints = datapoints
      .enter()
      .append('path')
      .attr('class', 'datapoint')
      .attr('id', (d: Datapoint) => '_' + d.commit.repoID + '_' + d.commit.hash)
      .merge(datapoints)
      .transition()
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
          this.y(this.yFocusDomain, d, this.focusHeight) +
          ')'
      )
      .attr('fill', (d: Datapoint) => this.colorById(d.commit.repoID))
      .attr('stroke', (d: Datapoint) => this.colorById(d.commit.repoID))
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

  private appendTooltips(keyFn: d3.ValueFn<any, any, string>) {
    let tooltip = d3
      .selectAll('.datapoint')
      .data(this.allDatapoints, keyFn)
      .on('mouseover', this.mouseover)
      .on('mousemove', this.mousemove)
      .on('mouseleave', this.mouseleave)
      .on('click', (d: Datapoint) => {
        this.$router.push({
          name: 'commit-detail',
          params: { repoID: d.commit.repoID, hash: d.commit.hash }
        })
      })
      .on('contextmenu', (d: Datapoint) => {
        d3.event.preventDefault()
        this.openDatapointMenu(d)
      })
      .on('mousedown', (d: Datapoint) => {
        if (d3.event.which === 2) {
          d3.event.preventDefault()
          let routeData = this.$router.resolve({
            name: 'commit-detail',
            params: { repoID: d.commit.repoID, hash: d.commit.hash }
          })
          window.open(routeData.href, '_blank')
        }
      })
  }

  private mouseover(d: Datapoint) {
    d3.select('#tooltip')
      .transition()
      .duration(300)
      .style('opacity', 1)
      .style('visibility', 'visible')
  }

  private mousemove(d: Datapoint) {
    let tooltip: d3.Selection<
      d3.BaseType,
      unknown,
      HTMLElement,
      any
    > = d3.select('#tooltip')
    let tipWidth = (tooltip.node() as HTMLElement).getBoundingClientRect().width
    let tipHeight = (tooltip.node() as HTMLElement).getBoundingClientRect()
      .height

    let date = d.commit.authorDate || 0

    let htmlMessage = `
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
            <td>${formatDateUTC(date)}</td>
          </tr>
          <tr>
            <td>Exact value</td>
            <td>${this.valueFormat(d.value)} ${this.unit}</td>
          </tr>
          <tr>
            <td>Commit summary</td>
            <td>${d.commit.summary!.trim()}</td>
          </tr>
        </table>
      `
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

    this.resetBrush()

    d3.select('#mainSvg')
      .select('#focusClipRect')
      .attr('width', this.innerWidth)
      .attr('height', this.focusHeight + 12)

    d3.select('#mainSvg')
      .select('#contextClipRect')
      .attr('width', this.innerWidth)
      .attr('height', this.contextHeight + 2)

    this.updateFocus()
  }

  @Watch('datapoints')
  @Watch('minTimestamp')
  @Watch('maxTimestamp')
  private updateData() {
    this.context = [this.minTimestamp, this.maxTimestamp]
    this.resetBrush()
    d3.select('#yLabel').text(this.yLabel)
    this.updateFocus()
  }

  @Watch('focus')
  @Watch('beginYAtZero')
  private updateFocus() {
    if (this.focus[0] < this.context[0] || this.focus[1] > this.context[1]) {
      this.focus = this.context
    }
    this.updateAxes()
    this.drawGraph()
  }

  @Watch('dialogOpen')
  private dialogClosed() {
    if (!this.dialogOpen) {
      this.selectedDatapoint = null
    }
  }

  private resetBrush() {
    d3.select('#contextLayer')
      .select('#brush')
      .remove()
    d3.select('#contextLayer')
      .append('g')
      .attr('id', 'brush')
      .call(this.brush)
      .call(this.brush.move, this.focus.map(this.xScale(this.context)))
  }

  private updateAxes() {
    ;(d3.select('#focusXAxis') as d3.Selection<
      SVGGElement,
      unknown,
      HTMLElement,
      any
    >)
      .attr('transform', 'translate(0,' + this.focusHeight + ')')
      .call(this.focusXAxis())
      .selectAll('text')
      .attr('transform', 'translate(-10, 10) rotate(-35)')
      .style('text-anchor', 'end')
    ;(d3.select('#contextXAxis') as d3.Selection<
      SVGGElement,
      unknown,
      HTMLElement,
      any
    >)
      .attr('transform', 'translate(0,' + this.innerHeight + ')')
      .call(this.contextXAxis())
      .selectAll('text')
      .attr('transform', 'translate(-10, 10) rotate(-35)')
      .style('text-anchor', 'end')
    ;(d3.select('#yAxis') as d3.Selection<
      SVGGElement,
      unknown,
      HTMLElement,
      any
    >).call(this.yAxis())

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
      .attr('y', -6)
      .attr('width', this.innerWidth)
      .attr('height', this.focusHeight + 12)

    d3.select('#mainSvg')
      .append('clipPath')
      .attr('id', 'contextClip')
      .append('rect')
      .attr('id', 'contextClipRect')
      .attr('y', -2)
      .attr('width', this.innerWidth + 1 /* 1 px stroke width? */)
      .attr('height', this.contextHeight + 2)

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
      .call(this.focusXAxis())

    d3.select('#dataLayer')
      .append('g')
      .attr('class', 'axis')
      .attr('id', 'yAxis')
      .call(this.yAxis())

    d3.select('#dataLayer')
      .append('g')
      .attr('class', 'axis')
      .attr('id', 'contextXAxis')
      .attr('transform', 'translate(0,' + this.innerHeight + ')')
      .call(this.contextXAxis())

    d3.select('#dataLayer')
      .append('text')
      .attr('id', 'yLabel')
      .attr('fill', 'currentcolor')
      .attr('text-anchor', 'middle')
      .attr('transform', 'rotate(-90)')
      .attr('y', -this.margin.left + 30)
      .attr('x', -this.focusHeight / 2)
      .text(this.yLabel)

    d3.select('#chart')
      .append('div')
      .attr('class', 'tooltip')
      .attr('id', 'tooltip')
      .style('opacity', 0)
  }

  private showInDetailGraph() {
    let selectedCommit = this.selectedDatapoint!.commit
    let repoId = selectedCommit.repoID
    vxm.repoDetailModule
      .fetchIndexOfCommit({
        commitHash: selectedCommit.hash,
        repoId: repoId
      })
      .then(({ index, comparison }) => {
        this.closeDialog()

        vxm.repoDetailModule.referenceDatapoint = {
          commit: selectedCommit,
          comparison: comparison,
          measurementId: new MeasurementID(
            vxm.repoComparisonModule.selectedBenchmark,
            vxm.repoComparisonModule.selectedMetric
          )
        }

        this.$router.push({
          name: 'repo-detail',
          params: { id: repoId },
          query: {
            selectedMeasurements: JSON.stringify([
              {
                metric: vxm.repoComparisonModule.selectedMetric,
                benchmark: vxm.repoComparisonModule.selectedBenchmark
              }
            ]),
            skip: '' + Math.max(0, index - 20),
            fetchAmount: '40',
            relativeToCommit: selectedCommit.hash,
            lockedToRelativeCommit: 'false'
          }
        })
      })
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
    // setTimeout(this.resize, 500)
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

.information {
  text-align: center;
  font-family: Roboto;
  font-size: 18px;
  fill: dimgray;
}

#referenceLine {
  fill: none;
  stroke-width: 1px;
  stroke-dasharray: 5 5;
}

#chart {
  position: relative;
}

#brush .handle {
  width: 16px;
}

.datapointDialog .v-input .v-label {
  height: unset !important;
}
</style>
