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

type CommitInfo = {
  commit: Commit
  comparison: CommitComparison
  measurementId: MeasurementID
}

@Component({
  components: {
    'datapoint-dialog': DatapointDialog
  }
})
export default class DetailGraph extends Vue {
  @Prop({})
  measurements!: MeasurementID[]

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
    right: 50,
    top: 20,
    bottom: 30
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

  private get minVal(): number | undefined {
    return d3.min(this.datapoints, this.datapointValue)
  }

  private get maxVal(): number | undefined {
    return d3.max(this.datapoints, this.datapointValue)
  }

  private get dataAvailable(): boolean {
    return this.measurements.length > 0 && this.maxVal !== undefined
  }

  private lastValue: Map<MeasurementID, number> = new Map()

  private firstSuccessful(measurementId: MeasurementID): number {
    for (const datapoint of this.datapoints) {
      let wantedMeasurement = this.wantedMeasurementForDatapoint(
        datapoint.comparison,
        measurementId
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

  private hasDataForPoint(datapoint: CommitInfo) {
    return (
      datapoint !== null &&
      this.groupedByMeasurement.has(datapoint.measurementId.toString())
    )
  }

  private x(datapoint: CommitInfo): number {
    let datapoints = this.groupedByMeasurement.get(
      datapoint.measurementId.toString()
    )!
    return this.currentXScale(
      datapoints.length -
        datapoints.findIndex(
          it =>
            it.comparison.secondCommit.hash ===
            datapoint.comparison.secondCommit.hash
        )
    )
  }

  private y(
    comparison: CommitComparison,
    measurementId: MeasurementID
  ): number {
    let wantedMeasurement = this.wantedMeasurementForDatapoint(
      comparison,
      measurementId
    )
    if (wantedMeasurement !== undefined && wantedMeasurement.value) {
      this.lastValue.set(measurementId, wantedMeasurement.value)
      return this.yScale(wantedMeasurement.value)
    }
    if (this.datapoints.findIndex(it => it.comparison === comparison) === 0) {
      this.lastValue.set(measurementId, this.firstSuccessful(measurementId))
    }
    return this.yScale(this.lastValue.get(measurementId) || 0)
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

  private unit(measurementId: MeasurementID): string | null {
    for (const datapoint of this.datapoints) {
      let wantedMeasurement = this.wantedMeasurementForDatapoint(
        datapoint.comparison,
        measurementId
      )
      if (wantedMeasurement !== undefined && wantedMeasurement.unit) {
        return wantedMeasurement.unit
      }
    }
    return null
  }

  private get yLabel(): string {
    if (this.measurements.length === 1) {
      return this.unit(this.measurements[0])
        ? this.measurements[0].metric + ' in ' + this.unit(this.measurements[0])
        : this.measurements[0].metric
    }
    return ''
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
      .selectAll<SVGPathElement, CommitInfo>('.datapoint')
      .attr(
        'transform',
        d =>
          'translate(' +
          this.x(d) +
          ', ' +
          this.y(d.comparison, d.measurementId) +
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

    this.drawPath(false)

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
    if (
      vxm.repoDetailModule.referenceDatapoint &&
      this.hasDataForPoint(vxm.repoDetailModule.referenceDatapoint)
    ) {
      this.drawReferenceLine(vxm.repoDetailModule.referenceDatapoint)
      this.drawCrosshair(vxm.repoDetailModule.referenceDatapoint, 'gray')
    } else {
      this.removeReference(false)
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
      .attr('y1', this.y(datapoint.comparison, datapoint.measurementId))
      .attr('x2', 0)
      .attr('y2', this.y(datapoint.comparison, datapoint.measurementId))

    referenceLine
      .exit()
      .transition()
      .attr('opacity', 0)
      .remove()
  }

  private removeReference(clearReferenceDataPoint: boolean = true) {
    d3.select('#referenceLine')
      .transition()
      .attr('opacity', 0)
      .remove()
    this.removeCrosshair(vxm.repoDetailModule.referenceDatapoint!)
    if (clearReferenceDataPoint) {
      vxm.repoDetailModule.referenceDatapoint = null
    }
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
    let crosshair = d3.select('#_' + this.keyFn(datapoint))

    if (crosshair.node()) {
      let crosshairRect = (crosshair.node() as SVGElement).getBoundingClientRect()
      let crosshairWidth: number = crosshairRect.width
      let crosshairHeight: number = crosshairRect.height

      d3.select('#_' + this.keyFn(datapoint))
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
            (this.x(datapoint) - 12) +
            ', ' +
            (this.y(datapoint.comparison, datapoint.measurementId) - 12) +
            ')'
        )
        .attr('opacity', 1)
        .attr('fill', color)
        .attr('stroke', color)
        .attr('stroke-width', this.strokeWidth(datapoint))
    }
  }

  private removeCrosshair(datapoint: CommitInfo) {
    if (!this.hasDataForPoint(datapoint)) {
      return
    }
    d3.select('#_' + this.keyFn(datapoint))
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
          this.x(datapoint) +
          ', ' +
          this.y(datapoint.comparison, datapoint.measurementId) +
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

  private keyFn(d: CommitInfo): string {
    return btoa(
      d.commit.hash +
        '_' +
        d.measurementId.benchmark +
        '_' +
        d.measurementId.metric
    )
      .replace('=', 'eq')
      .replace('+', 'plus')
      .replace('/', 'slash')
  }

  private drawGraph() {
    if (this.dataAvailable) {
      if (!this.graphDrawn) {
        d3.select('#dataLayer').remove()
        this.defineSvgElements()
        this.graphDrawn = true
      }
      this.drawPath()
      this.drawDatapoints(this.keyFn)
      this.appendTooltips(this.keyFn)
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
        this.measurements.length === 0
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

  // https://stackoverflow.com/questions/14446511/most-efficient-method-to-groupby-on-an-array-of-objects
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

  private drawPath(animated: boolean = true) {
    let path: d3.Selection<
      SVGPathElement,
      CommitInfo[],
      d3.BaseType,
      unknown
    > = d3
      .select('#graphArea')
      .selectAll<SVGPathElement, unknown>('.line')
      .data(Array.from(this.groupedByMeasurement.values()))
    let newPath = path
      .enter()
      .append('path')
      .attr('class', 'line')
      .merge(path)
      .transition()
      .duration(animated ? 1000 : 0)
      .delay(animated ? 100 : 0)
      .attr('d', this.line(this.currentXScale))
      .attr('stroke', commitInfos =>
        this.metricColor(commitInfos[0].measurementId)
      )
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
      .attr('id', (d: CommitInfo) => '_' + this.keyFn(d))
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
          this.x(d) +
          ', ' +
          this.y(d.comparison, d.measurementId) +
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
    let wantedMeasurement = this.wantedMeasurementForDatapoint(
      d.comparison,
      d.measurementId
    )
    if (this.benchmarkFailed(d)) {
      return 'grey'
    } else if (wantedMeasurement) {
      return this.metricColor(d.measurementId)
    }
    return 'white'
  }

  strokeColor(d: CommitInfo): string {
    let wantedMeasurement = this.wantedMeasurementForDatapoint(
      d.comparison,
      d.measurementId
    )
    if (wantedMeasurement && wantedMeasurement.successful) {
      return this.metricColor(d.measurementId)
    }
    return 'grey'
  }

  private metricColor(measurementId: MeasurementID) {
    return vxm.colorModule.colorByIndex(
      this.measurements.findIndex(it => it.equals(measurementId))
    )
  }

  private strokeWidth(d: CommitInfo): number {
    if (this.benchmarkFailed(d)) {
      return 0
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

  get line(): (xScale: d3.ScaleLinear<number, number>) => any {
    return (xScale: d3.ScaleLinear<number, number>) =>
      d3
        .line<CommitInfo>()
        .x((datapoint: CommitInfo) => {
          return this.x(datapoint)
        })
        .y((datapoint: CommitInfo) => {
          return this.y(datapoint.comparison, datapoint.measurementId)
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

    let measurementId =
      d.measurementId.benchmark + ' - ' + d.measurementId.metric
    let wantedMeasurement = this.wantedMeasurementForDatapoint(
      d.comparison,
      d.measurementId
    )
    let htmlMessage: string = ''
    if (
      d.commit.authorDate &&
      wantedMeasurement &&
      wantedMeasurement.successful
    ) {
      let unit = this.unit(wantedMeasurement.id)
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
            <td>${this.valueFormat(wantedMeasurement.value)} ${unit}</td>
          </tr>
          <tr>
            <td>Metric</td>
            <td>${measurementId}</td>
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
            <td>Metric</td>
            <td>${measurementId}</td>
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
            <td>Metric</td>
            <td>${measurementId}</td>
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
        'Commit ' +
        d.commit.hash +
        '<br />author:' +
        d.commit.author +
        '<br>metric: ' +
        measurementId
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
