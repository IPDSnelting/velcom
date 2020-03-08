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
          :selectedDatapoint="selectedDatapoint"
          :commitToCompare="commitToCompare"
          :allowSelectAsReference="isSelectedAllowedAsReference"
          @setReference="setReference"
          @selectCommitToCompare="selectCommitToCompare"
          @compareCommits="compareCommits"
          @removeReference="removeReference"
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

type CommitInfo = { commit: Commit; comparison: CommitComparison }

@Component({
  components: {
    'datapoint-dialog': DatapointDialog
  }
})
export default class DetailGraph extends Vue {
  @Prop({})
  measurement!: MeasurementID

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

  // retrieving and interpreting datapoints
  private get amount(): number {
    return Number.parseInt(vxm.repoDetailModule.selectedFetchAmount)
  }

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
    if (wantedMeasurement !== undefined && wantedMeasurement.value !== null) {
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

  // scales and axes
  private get baseXScale(): d3.ScaleLinear<number, number> {
    return d3
      .scaleLinear()
      .domain([this.amount + 0.5, 0.5])
      .range([0, this.innerWidth])
  }

  private currentXScale: d3.ScaleLinear<number, number> = this.baseXScale

  private get yScale(): d3.ScaleLinear<number, number> {
    let min: number = !this.beginYAtZero && this.minVal ? this.minVal : 0
    let max: number = this.maxVal || 0
    return d3
      .scaleLinear()
      .domain([min, max])
      .nice()
      .range([this.innerHeight, 0])
  }

  private x(
    comparison: CommitComparison,
    xScale: d3.ScaleLinear<number, number>
  ): number {
    return xScale(
      this.datapoints.length -
        this.datapoints.findIndex(
          it => it.comparison.secondCommit.hash === comparison.secondCommit.hash
        )
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
    return d3.axisBottom(this.currentXScale).tickFormat(this.xAxisFormat)
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

  // interacting with the graph via zooming and brushing
  private get zoom() {
    return d3
      .zoom()
      .scaleExtent([1, 50])
      .extent([
        [0, 0],
        [this.innerWidth, this.innerHeight]
      ])
      .translateExtent([
        [0, -Infinity],
        [this.innerWidth, Infinity]
      ])
      .filter(() => !d3.event.ctrlKey)
      .on('zoom', this.zoomed)
  }

  private zoomed() {
    let transform: d3.ZoomTransform = d3.event.transform
    this.currentXScale = transform.rescaleX(this.baseXScale)

    d3.select('#dataLayer')
      .selectAll<SVGPathElement, unknown>('.datapoint')
      .attr(
        'transform',
        (d: any) =>
          'translate(' +
          this.x(d.comparison, this.currentXScale) +
          ', ' +
          this.y(d.comparison) +
          ') rotate(-45)'
      )
    if (vxm.repoDetailModule.referenceDatapoint) {
      this.drawCrosshair(vxm.repoDetailModule.referenceDatapoint, 'gray')
    }
    if (this.commitToCompare) {
      this.drawCrosshair(
        this.commitToCompare,
        this.datapointColor(this.commitToCompare)
      )
    }

    d3.select('#dataLayer')
      .selectAll<SVGPathElement, unknown>('#line')
      .attr('d', this.line(this.currentXScale))

    this.xAxis.scale(this.currentXScale)
    d3.select('#xAxis')
      .attr('transform', 'translate(0,' + this.innerHeight + ')')
      .call(this.xAxis as any)
  }

  private get brush() {
    return d3
      .brushX()
      .extent([
        [0, 0],
        [this.innerWidth, this.innerHeight]
      ])
      .filter(() => d3.event.ctrlKey)
      .on('end', this.brushed)
  }

  private brushed() {
    let selection = d3.event.selection

    if (selection) {
      let newMin: number = Math.floor(this.currentXScale.invert(selection[1]))
      let newMax = Math.floor(this.currentXScale.invert(selection[0]))
      let newAmount: number = newMax - newMin
      let additionalSkip: number = newMin
      this.$emit('selectionChanged', newAmount, additionalSkip)
      d3.select('#brush').call(this.brush.move as any, null)
    }
  }

  // listening for special key events that trigger resize or change cursor apperance

  private resizeListener: () => void = () => {}
  private keyupListener: (e: KeyboardEvent) => void = () => {}
  private keydownListener: (e: KeyboardEvent) => void = () => {}

  // interacting with data points via DatapointDialog
  private dialogOpen: boolean = false
  private selectedDatapoint: CommitInfo | null = null
  private commitToCompare: CommitInfo | null = null

  private get isSelectedAllowedAsReference() {
    return (
      this.selectedDatapoint &&
      this.datapointValue(this.selectedDatapoint) !== undefined
    )
  }

  private get selectedCommitToCompare(): boolean {
    return this.commitToCompare !== null
  }

  openDatapointMenu(datapoint: CommitInfo) {
    this.selectedDatapoint = datapoint
    this.dialogOpen = true
  }

  setReference() {
    if (vxm.repoDetailModule.referenceDatapoint) {
      this.removeCrosshair(vxm.repoDetailModule.referenceDatapoint)
    }
    if (this.selectedDatapoint) {
      vxm.repoDetailModule.referenceDatapoint = this.selectedDatapoint
    }
    if (vxm.repoDetailModule.referenceDatapoint) {
      this.drawReferenceLine(vxm.repoDetailModule.referenceDatapoint)
      this.drawCrosshair(vxm.repoDetailModule.referenceDatapoint, 'gray')
    }
    this.closeDialog()
  }

  private drawReferenceLine(datapoint: CommitInfo) {
    let referenceLine = d3
      .select('#graphArea')
      .selectAll<SVGPathElement, unknown>('#referenceLine')
      .data([datapoint])

    let newReferenceLine = referenceLine
      .enter()
      .append('line')
      .attr('id', 'referenceLine')
      .merge(referenceLine as any)
      .transition()
      .duration(1000)
      .delay(100)
      .attr('x1', this.innerWidth)
      .attr('y1', this.y(datapoint.comparison))
      .attr('x2', 0)
      .attr('y2', this.y(datapoint.comparison))

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
    this.removeCrosshair(vxm.repoDetailModule.referenceDatapoint!)
    vxm.repoDetailModule.referenceDatapoint = null
    this.closeDialog()
  }

  private selectCommitToCompare() {
    if (this.commitToCompare) {
      this.removeCrosshair(this.commitToCompare)
    }
    if (this.selectedDatapoint) {
      this.commitToCompare = this.selectedDatapoint || null
      this.drawCrosshair(
        this.selectedDatapoint,
        this.datapointColor(this.selectedDatapoint)
      )
    }
    this.closeDialog()
  }

  private compareCommits() {
    if (this.commitToCompare && this.selectedDatapoint) {
      this.$router.push({
        name: 'commit-comparison',
        params: {
          repoID: this.selectedRepo,
          hashOne: this.commitToCompare.commit.hash,
          hashTwo: this.selectedDatapoint.commit.hash
        }
      })
    }
    this.closeDialog()
  }

  private crosshairIcon = crosshairIcon

  private drawCrosshair(datapoint: CommitInfo, color: string) {
    let crosshair = d3.select('#_' + datapoint.commit.hash)

    if (crosshair) {
      let crosshairRect = (crosshair.node() as SVGElement).getBoundingClientRect()
      let crosshairWidth: number = crosshairRect.width
      let crosshairHeight: number = crosshairRect.height

      d3.select('#_' + datapoint.commit.hash)
        .transition()
        .duration(1000)
        .delay(100)
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
            (this.x(datapoint.comparison, this.currentXScale) - 12) +
            ', ' +
            (this.y(datapoint.comparison) - 12) +
            ')'
        )
        .attr('opacity', 1)
        .attr('fill', color)
        .attr('stroke', color)
        .attr('stroke-width', this.strokeWidth(datapoint))
    }
  }

  private removeCrosshair(datapoint: CommitInfo) {
    d3.select('#_' + datapoint.commit.hash)
      .attr(
        'd',
        d3
          .symbol()
          .type(this.datapointSymbol(datapoint))
          .size(this.datapointSize(datapoint))
      )
      .attr(
        'transform',
        'translate(' +
          this.x(datapoint.comparison, this.currentXScale) +
          ', ' +
          this.y(datapoint.comparison) +
          ') rotate(-45)'
      )
      .attr('fill', this.datapointColor(datapoint))
      .attr('stroke', this.strokeColor(datapoint))
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

      let keyFn: d3.ValueFn<any, any, string> = (d: CommitInfo) => {
        return d.commit.hash
      }
      this.drawPath()
      this.drawDatapoints(keyFn)
      this.appendTooltips(keyFn)
      if (this.commitToCompare) {
        this.drawCrosshair(
          this.commitToCompare,
          this.datapointColor(this.commitToCompare)
        )
      }
      this.setReference()
    } else {
      if (this.graphDrawn) {
        this.graphDrawn = false
      }
      d3.select('#dataLayer').remove()

      let information: string =
        this.measurement.metric === ''
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

  private drawPath() {
    let path: d3.Selection<
      SVGPathElement,
      CommitInfo[],
      d3.BaseType,
      unknown
    > = d3
      .select('#graphArea')
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
      .attr('d', this.line(this.currentXScale))
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
      .select('#graphArea')
      .attr('clip-path', 'url(#clip)')
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
          this.x(d.comparison, this.currentXScale) +
          ', ' +
          this.y(d.comparison) +
          ') rotate(-45)'
      )
      .attr('fill', (d: CommitInfo) => this.datapointColor(d))
      .attr('stroke', (d: CommitInfo) => this.strokeColor(d))
      .attr('stroke-width', (d: CommitInfo) => this.strokeWidth(d))
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
      .on('mousedown', (d: CommitInfo) => {
        if (d3.event.which === 2) {
          d3.event.preventDefault()
          let routeData = this.$router.resolve({
            name: 'commit-detail',
            params: { repoID: this.selectedRepo, hash: d.commit.hash }
          })
          window.open(routeData.href, '_blank')
        }
      })
  }

  datapointSymbol(d: CommitInfo): d3.SymbolType {
    if (this.benchmarkFailed(d)) {
      return d3.symbolCross
    }
    return d3.symbolCircle
  }

  datapointSize(d: CommitInfo): number {
    return this.benchmarkFailed(d)
      ? 2 * this.datapointWidth
      : this.datapointWidth
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

  get line(): (xScale: d3.ScaleLinear<number, number>) => any {
    return (xScale: d3.ScaleLinear<number, number>) =>
      d3
        .line<CommitInfo>()
        .x((datapoint: CommitInfo) => {
          return this.x(datapoint.comparison, xScale)
        })
        .y((datapoint: CommitInfo) => {
          return this.y(datapoint.comparison)
        })
  }

  private mouseover(d: CommitInfo) {
    d3.select('#tooltip')
      .transition()
      .duration(300)
      .style('opacity', 1)
      .style('visibility', 'visible')
  }

  private mousemove(d: CommitInfo) {
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

  // updating ths graph
  private resize() {
    let chart = d3.select('#chart').node() as HTMLElement
    this.width = chart ? chart.getBoundingClientRect().width : 900
    this.height = this.width / 2
    this.updateCurrentAxisAfterResize()

    d3.select('#dataLayer')
      .select('#brush')
      .remove()
    d3.select('#dataLayer')
      .append('g')
      .attr('id', 'brush')
      .call(this.brush)
      .call(this.zoom as any)
      .lower()
    d3.select('#mainSvg')
      .select('#clipRect')
      .attr('width', this.innerWidth)
      .attr('height', this.innerHeight + 12)

    this.updateData()
  }

  private updateCurrentAxisAfterResize() {
    // FIXME: Keep zoom on resize
    this.currentXScale = this.baseXScale
  }

  @Watch('datapoints')
  private updateDatapoints() {
    d3.select('#yLabel').text(this.yLabel)
    this.updateData()
  }

  @Watch('beginYAtZero')
  @Watch('amount')
  private updateData() {
    this.updateCurrentAxisAfterResize()
    this.updateAxes()
    this.drawGraph()
  }

  @Watch('dialogOpen')
  private dialogClosed() {
    if (!this.dialogOpen) {
      this.selectedDatapoint = null
    }
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
    d3.select('#mainSvg')
      .append('g')
      .attr('id', 'dataLayer')
      .attr(
        'transform',
        'translate(' + this.margin.left + ',' + this.margin.top + ')'
      )

    d3.select('#dataLayer')
      .append('g')
      .attr('id', 'brush')
      .call(this.brush)
      .call(this.zoom as any)
      .lower()

    d3.select('#dataLayer')
      .append('g')
      .attr('id', 'graphArea')

    d3.select('#mainSvg')
      .append('clipPath')
      .attr('id', 'clip')
      .append('rect')
      .attr('id', 'clipRect')
      .attr('y', -6)
      .attr('width', this.innerWidth)
      .attr('height', this.innerHeight + 12)

    d3.select('#dataLayer')
      .append('g')
      .attr('class', 'axis')
      .attr('id', 'xAxis')
      .attr('transform', 'translate(0,' + this.innerHeight + ')')
      .call(this.xAxis)

    d3.select('#dataLayer')
      .append('g')
      .attr('class', 'axis')
      .attr('id', 'yAxis')
      .call(this.yAxis)

    d3.select('#dataLayer')
      .append('text')
      .attr('id', 'yLabel')
      .attr('text-anchor', 'middle')
      .attr('transform', 'rotate(-90)')
      .attr('y', -this.margin.left + 30)
      .attr('x', -this.innerHeight / 2)
      .text(this.yLabel)

    d3.select('#dataLayer')
      .append('line')
      .attr('id', 'referenceLine')

    let tip = d3
      .select('#chart')
      .append('div')
      .attr('class', 'tooltip')
      .attr('id', 'tooltip')
      .style('opacity', 0)
  }

  // initializing
  created() {
    this.resizeListener = () => {
      this.resize()
    }
    this.keydownListener = (e: KeyboardEvent) => {
      d3.select('#brush .overlay').attr(
        'cursor',
        e.ctrlKey ? 'crosshair' : 'cursor'
      )
    }
    this.keyupListener = (e: KeyboardEvent) => {
      d3.select('#brush .overlay').attr('cursor', 'cursor')
    }
    window.addEventListener('resize', this.resizeListener)
    document.addEventListener('keydown', this.keydownListener)
    document.addEventListener('keyup', this.keyupListener)
  }

  mounted() {
    d3.select('#mainSvg')
      .attr('width', '100%')
      .attr('height', '100%')
      .attr('align', 'end')
      .attr('justify', 'end')
      .on('mouseenter', () => this.keydownListener(d3.event))
    this.resize()
    d3.select('#brush .overlay').attr('cursor', 'cursor')
  }

  beforeDestroy() {
    window.removeEventListener('resize', this.resizeListener)
    document.removeEventListener('keydown', this.keydownListener)
    document.removeEventListener('keyup', this.keyupListener)
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
  stroke: dimgray;
  stroke-width: 1px;
  stroke-dasharray: 5 5;
}

#chart {
  position: relative;
}

.datapointDialog .v-input .v-label {
  height: unset !important;
}
</style>
