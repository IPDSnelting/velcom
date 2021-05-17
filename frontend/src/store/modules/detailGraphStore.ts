import { action, createModule, mutation } from 'vuex-class-component'
import {
  AttributedDatapoint,
  DetailDataPoint,
  Dimension,
  DimensionId,
  dimensionIdToString,
  RepoId
} from '@/store/types'
import axios from 'axios'
import { detailDataPointFromJson } from '@/util/GraphJsonHelper'
import { dimensionFromJson } from '@/util/RepoJsonHelper'
import { vxm } from '@/store'
import router from '@/router'
import { Route } from 'vue-router'
import { spaceDayEquidistant } from '@/util/DayEquidistantUtil'
import {
  orElse,
  orUndefined,
  parseAndSetZoomAndDateRange,
  respectOptions
} from '@/util/LinkUtils'
import { roundDateDown, roundDateUp } from '@/util/TimeUtil'

const VxModule = createModule({
  namespaced: 'detailGraphModule',
  strict: false
})

export type PermanentLinkOptions = Partial<{
  includeXZoom: boolean
  includeYZoom: boolean
  includeDataRestrictions: boolean
}>

export class DetailGraphStore extends VxModule {
  // <!--<editor-fold desc="STATE">-->
  private _detailGraph: DetailDataPoint[] = []
  private _selectedRepoId: RepoId = ''
  private _selectedDimensions: Dimension[] = []

  private colorIndexMap: Map<string, number> = new Map()

  private firstFreeColorIndex: number = 0

  referenceDatapoint: AttributedDatapoint | null = null
  commitToCompare: AttributedDatapoint | null = null

  // One week in the past, as elegant as ever
  private _startTime: Date = new Date(
    new Date(new Date().setDate(new Date().getDate() - 7)).setHours(0, 0, 0, 0)
  )

  private _endTime: Date = new Date(new Date().setHours(24, 0, 0, 0))

  zoomXStartValue: number | null = null
  zoomXEndValue: number | null = null
  zoomYStartValue: number | null = null
  zoomYEndValue: number | null = null

  // different timespan buffer sizes
  private minToBuffer: number = 14
  private maxToBuffer: number = 365
  private minBuffer: number = 8 // ~ a week, 4 days on each side
  private maxBuffer: number = 182 // ~ half a year
  private bufferRatio: number = 0.5

  beginYScaleAtZero: boolean = false
  dayEquidistantGraph: boolean = true

  selectedTab: number = 1
  selectedDimensionSelector: 'tree' | 'matrix' = 'matrix'
  //  <!--</editor-fold>-->

  // <!--<editor-fold desc="ACTIONS">-->

  /**
   * Fetches the data necessary to display the data points
   * in a detail graph of a given time frame.
   *
   * @returns {Promise<DetailDataPoint[]>}
   * @memberof detailGraphStore
   */
  @action
  async fetchDetailGraph(): Promise<DetailDataPoint[]> {
    const dayInMillis: number = 60 * 60 * 24
    const effectiveStartTime: number = Math.floor(
      this.bufferedTimespan.startTime.getTime() / 1000
    )
    const effectiveEndTime: number = Math.floor(
      this.bufferedTimespan.endTime.getTime() / 1000 + dayInMillis
    )

    // If we have selected no dimensions, we don't event need to make a request
    if (this.selectedDimensions.length === 0) {
      this.setDetailGraph([])
      return []
    }

    const response = await axios.get(`/graph/detail/${this._selectedRepoId}`, {
      snackbarTag: 'commit-history',
      params: {
        start_time: effectiveStartTime,
        end_time: effectiveEndTime,
        duration: undefined,
        dimensions: this.dimensionString
      }
    })

    const seriesIds = response.data.dimensions
      .map(dimensionFromJson)
      // This map is not needed as hopefully the CustomKeyEqualsMap should be used
      // but it increases performance [O(n) => O(1)]
      .map((dim: Dimension) =>
        this._selectedDimensions.find(savedDimension =>
          savedDimension.equals(dim)
        )
      )
      .map((it: Dimension) => it.toString())

    const dataPoints: DetailDataPoint[] = response.data.commits.map(
      (json: any) =>
        detailDataPointFromJson(json, seriesIds, this.selectedRepoId)
    )

    this.setDetailGraph(dataPoints)

    return dataPoints
  }

  /**
   * Adjusts this store to the values defined in the permanent link.
   *
   * @param link the link to adjust to
   */
  @action
  async adjustToPermanentLink(link: Route): Promise<void> {
    if (!link.params.id) {
      return
    }
    const repoId: RepoId = link.params.id
    const repo =
      vxm.repoModule.repoById(repoId) ||
      (await vxm.repoModule.fetchRepoById(repoId))

    if (link.query.dayEquidistant === 'true') {
      this.dayEquidistantGraph = true
    }

    if (link.query.dimensions && typeof link.query.dimensions === 'string') {
      const dimensionString = link.query.dimensions
      const parts = dimensionString.split('::')
      vxm.detailGraphModule.selectedDimensions = parts.flatMap(it => {
        const [benchmark, ...metrics] = it.split(':')

        return repo.dimensions.filter(
          it => it.benchmark === benchmark && metrics.includes(it.metric)
        )
      })
    }

    parseAndSetZoomAndDateRange(link, vxm.detailGraphModule)
  }
  //  <!--</editor-fold>-->

  // <!--<editor-fold desc="MUTATIONS">-->
  @mutation
  setDetailGraph(graph: DetailDataPoint[]): void {
    this._detailGraph = graph
  }
  //  <!--</editor-fold>-->

  // <!--<editor-fold desc="GET / SET TIME">-->

  get startTime(): Date {
    return this._startTime
  }

  set startTime(time: Date) {
    this._startTime = roundDateDown(time)
  }

  get endTime(): Date {
    return this._endTime
  }

  set endTime(time: Date) {
    this._endTime = roundDateUp(time)
  }

  get duration(): number {
    const timeDiff = this.endTime.getTime() - this.startTime.getTime()

    return Math.ceil(timeDiff / (1000 * 3600 * 24))
  }

  /**
   * Returns the boundaries of the buffered timespan that is used when fetchin a
   * new detail graph. The buffered timespan extends the timespan implied
   * by start and end date to allow for panning a little outside the graph
   * boundaries without having to wait for a new graph to be fetched
   *
   * @type {startTime: Date, endTime: Date}
   */
  get bufferedTimespan(): { startTime: Date; endTime: Date } {
    let buffer: number
    if (this.duration <= this.minToBuffer) {
      buffer = this.minBuffer
    } else if (this.duration >= this.maxToBuffer) {
      buffer = this.maxBuffer
    } else {
      buffer = this.duration * this.bufferRatio
    }
    const bufferMillis = buffer * 1000 * 60 * 60 * 24 // ms * minutes * hours * days

    const bufferedStartTime = new Date(
      this.startTime.getTime() - bufferMillis / 2
    )
    const bufferedEndTime = new Date(this.endTime.getTime() + bufferMillis / 2)

    return { startTime: bufferedStartTime, endTime: bufferedEndTime }
  }
  //  <!--</editor-fold>-->

  // <!--<editor-fold desc="GET / SET DIMENSIONS">-->

  /**
   * Returns all selected dimensions.
   *
   * @readonly
   * @type {Dimension[]}
   * @memberof detailGraphStore
   */
  get selectedDimensions(): Dimension[] {
    return this._selectedDimensions
  }

  /**
   * Sets the selected dimensions.
   *
   * @memberof detailGraphStore
   */
  set selectedDimensions(dimensions: Dimension[]) {
    dimensions.forEach(it => {
      if (!it) {
        throw new Error('UNDEFINED OR NULL!')
      }
      if (!this.colorIndexMap.has(it.toString())) {
        this.colorIndexMap.set(it.toString(), this.firstFreeColorIndex++)
      }
    })
    this._selectedDimensions = dimensions
  }

  /**
   * string of requested dimensions for a detail graph,
   * formatted as 'bench1:metric1.1:metric1.2::bench2:metric2.1' etc.
   *
   * @readonly
   * @private
   * @type {string}
   * @memberof DetailGraphStore
   */
  private get dimensionString(): string {
    let resultString: string = ''

    const groupedDimensions: {
      [key: string]: Dimension[]
    } = this._selectedDimensions.reduce(
      (benchmarkGroup: { [key: string]: Dimension[] }, cur: Dimension) => {
        ;(benchmarkGroup[cur.benchmark] =
          benchmarkGroup[cur.benchmark] || []).push(cur)
        return benchmarkGroup
      },
      {}
    )

    const groupedDimensionsArray: Dimension[][] = Object.keys(
      groupedDimensions
    ).map(benchmark => {
      return groupedDimensions[benchmark]
    })

    groupedDimensionsArray.forEach((benchmarkGroup: Dimension[]) => {
      resultString = resultString.concat(benchmarkGroup[0].benchmark)
      benchmarkGroup.forEach((dimension: Dimension) => {
        resultString = resultString.concat(':' + dimension.metric)
      })
      resultString = resultString.concat('::')
    })

    return resultString.slice(0, resultString.length - 2)
  }
  //  <!--</editor-fold>-->

  // <!--<editor-fold desc="GET / SET GRAPH">-->
  /**
   * Returns the locally stored data for a repo detail graph
   *
   * @readonly
   * @type {DetailDataPoint[]}
   * @memberof detailGraphStore
   */
  get detailGraph(): DetailDataPoint[] {
    const unbufferedGraph = this._detailGraph.filter(
      (datapoint: DetailDataPoint) =>
        datapoint.committerTime >= this.startTime &&
        datapoint.committerTime <= this.endTime
    )

    return this.dayEquidistantGraph
      ? spaceDayEquidistant(unbufferedGraph)
      : unbufferedGraph
  }

  /**
   * Returns an approximation of the currently visible number of points.
   */
  get visiblePoints(): number {
    const startValue = vxm.detailGraphModule.zoomXStartValue
    const endValue = vxm.detailGraphModule.zoomXEndValue

    // TODO: Is this a performance problem? There might be 10.000+ items here
    // and this method is called every time the slider is dragged or the user
    // zooms using the mouse wheel
    let visibleDataPoints = 0
    for (const point of this._detailGraph) {
      if (
        (startValue === null || point.committerTime.getTime() >= startValue) &&
        (endValue === null || point.committerTime.getTime() <= endValue)
      ) {
        visibleDataPoints += this._selectedDimensions.length
      }
    }
    return visibleDataPoints
  }

  /**
   * Returns the id of the repo that is currently selected in the repo detail view.
   *
   * Returns an empty string if none.
   *
   * @readonly
   * @type {string}
   * @memberof RepoDetailStore
   */
  get selectedRepoId(): RepoId {
    return this._selectedRepoId
  }

  /**
   * Sets the id of the repo that is currently selected in the repo detail view.
   *
   * @memberof RepoDetailStore
   */
  set selectedRepoId(selectedRepoId: RepoId) {
    this._selectedRepoId = selectedRepoId
    // reset colors
    this.colorIndexMap = new Map()
    this.firstFreeColorIndex = 0

    // rebuild selected dimensions so colors are correct
    const repo = vxm.repoModule.repoById(selectedRepoId)

    if (repo) {
      const isInRepo = (dimension: Dimension) =>
        repo.dimensions.find(it => it.equals(dimension)) !== undefined

      // we need to trigger the setter
      vxm.detailGraphModule.selectedDimensions =
        this._selectedDimensions.filter(isInRepo)
    } else {
      // we need to trigger the setter
      vxm.detailGraphModule.selectedDimensions = []
    }
  }

  get colorIndex(): (dimension: DimensionId) => number | undefined {
    return dimension => this.colorIndexMap.get(dimensionIdToString(dimension))
  }
  //  <!--</editor-fold>-->

  // <!--<editor-fold desc="GET / SET PERMANENT LINK">-->

  /**
   * Returns a permanent link to the current detail graph state
   */
  get permanentLink(): (options?: PermanentLinkOptions) => string {
    return options => {
      const route = router.resolve({
        name: 'repo-detail',
        params: { id: this.selectedRepoId },
        query: {
          zoomYStart: respectOptions(
            options,
            'includeYZoom',
            orUndefined(this.zoomYStartValue)
          ),
          zoomYEnd: respectOptions(
            options,
            'includeYZoom',
            orUndefined(this.zoomYEndValue)
          ),
          zoomXStart:
            options && options.includeXZoom
              ? orElse(this.zoomXStartValue, this.startTime.getTime())
              : orUndefined(this.startTime.getTime()),
          zoomXEnd:
            options && options.includeXZoom
              ? orElse(this.zoomXEndValue, this.endTime.getTime())
              : orUndefined(this.endTime.getTime()),
          dimensions: respectOptions(
            options,
            'includeDataRestrictions',
            this.dimensionString
          ),
          dayEquidistant: this.dayEquidistantGraph ? 'true' : undefined
        }
      })

      return location.origin + route.href
    }
  }
  //  <!--</editor-fold>-->

  /**
   * Converts a given store to a pure object that can be serialized.
   *
   * @param store the store to convert
   */
  static toPlainObject(store: DetailGraphStore): unknown {
    return {
      _selectedRepoId: store._selectedRepoId,
      _selectedDimensions: store._selectedDimensions,
      referenceDatapoint: store.referenceDatapoint,
      _startTime: store._startTime,
      _endTime: store._endTime,
      zoomXStartValue: store.zoomXStartValue,
      zoomXEndValue: store.zoomXEndValue,
      zoomYStartValue: store.zoomYStartValue,
      zoomYEndValue: store.zoomYEndValue,
      firstFreeColorIndex: store.firstFreeColorIndex,
      colorIndexMap: store.colorIndexMap,
      commitToCompare: store.commitToCompare,
      beginYScaleAtZero: store.beginYScaleAtZero,
      dayEquidistantGraph: store.dayEquidistantGraph,
      selectedTab: store.selectedTab,
      selectedDimensionSelector: store.selectedDimensionSelector
    }
  }
}
