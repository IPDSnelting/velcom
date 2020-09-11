import { createModule, mutation, action } from 'vuex-class-component'
import { RepoId, Dimension, DetailDataPoint } from '@/store/types'
import axios from 'axios'
import { detailDataPointFromJson } from '@/util/GraphJsonHelper'

const VxModule = createModule({
  namespaced: 'detailGraphModule',
  strict: false
})

export function detailGraphStoreFromJson(json?: string): any {
  if (!json) {
    return {}
  }
  const hydrateDimension = (it: Dimension) =>
    new Dimension(it.benchmark, it.metric, it.unit, it.interpretation)
  const hydrateDetailPoint = (it: DetailDataPoint) => {
    return new DetailDataPoint(
      it.hash,
      it.parents,
      it.author,
      new Date(it.authorDate),
      it.summary,
      new Map(it.values)
    )
  }

  const parsed = JSON.parse(json)
  // Convert flat json to real object
  parsed._selectedDimensions = parsed._selectedDimensions.map(hydrateDimension)
  if (parsed._referenceDatapoint) {
    parsed._referenceDatapoint = {
      dimension: hydrateDimension(parsed._referenceDatapoint.dimension),
      dataPoint: hydrateDetailPoint(parsed._referenceDatapoint.dataPoint)
    }
  }

  return parsed
}

export function detailGraphStoreToJson(store: DetailGraphStore): string {
  let referenceDataPoint = (store as any)._referenceDatapoint
  if (referenceDataPoint) {
    const persistablePoint = Object.assign({}, referenceDataPoint.dataPoint)
    persistablePoint.values = Array.from(persistablePoint.values.entries())

    referenceDataPoint = {
      dataPoint: persistablePoint,
      dimension: referenceDataPoint.dimension
    }
  }
  return JSON.stringify({
    _selectedRepoId: (store as any)._selectedRepoId,
    _selectedDimensions: (store as any)._selectedDimensions,
    _referenceDatapoint: referenceDataPoint
  })
}

export class DetailGraphStore extends VxModule {
  private _detailGraph: DetailDataPoint[] = []
  private _selectedRepoId: RepoId = ''
  private _selectedDimensions: Dimension[] = []
  private _referenceDatapoint: {
    dataPoint: DetailDataPoint
    dimension: Dimension
  } | null = null

  commitToCompare: DetailDataPoint | null = null

  /**
   * If true the user is locked to the relative commit, if false the
   * relative commit will be ignored.
   *
   * @type {boolean}
   * @memberof RepoDetailStore
   */
  lockedToRelativeCommit: boolean = false
  relativeToCommit: string = ''

  // One week in the past, as elegant as ever
  private _defaultStartTime: Date = new Date(
    new Date().setDate(new Date().getDate() - 7)
  )
  private _defaultEndTime: Date = new Date()
  private _startTime: Date = this._defaultStartTime
  private _endTime: Date = this._defaultEndTime
  private _duration: number = 7

  /**
   * Fetches the data necessary to display the data points
   * in a detail graph of a given time frame.
   *
   * @returns {Promise<DetailDataPoint[]>}
   * @memberof detailGraphStore
   */
  @action
  async fetchDetailGraph(): Promise<DetailDataPoint[]> {
    const effectiveStartTime: number = Math.floor(
      this._startTime.getTime() / 1000
    )
    const effectiveEndTime: number = Math.floor(
      this._endTime.getTime() / 1000 + 60 * 60 * 24
    )

    const response = await axios.get(`/graph/detail/${this._selectedRepoId}`, {
      snackbarTag: 'commit-history',
      params: {
        start_time: effectiveStartTime,
        end_time: effectiveEndTime,
        duration: undefined,
        dimensions: this.dimensionString
      }
    })

    const dataPoints: DetailDataPoint[] = response.data.commits.map((it: any) =>
      detailDataPointFromJson(it, this._selectedDimensions)
    )

    this.setDetailGraph(dataPoints)

    return dataPoints
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

    const groupedDimensions = this._selectedDimensions.reduce(
      (benchmarkGroup: { [key: string]: any[] }, cur: Dimension) => {
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

  @mutation
  setDetailGraph(graph: DetailDataPoint[]): void {
    this._detailGraph = graph
  }

  /**
   * Returns the locally stored data for a repo detail graph
   *
   * @readonly
   * @type {DetailDataPoint[]}
   * @memberof detailGraphStore
   */
  get detailGraph(): DetailDataPoint[] {
    return this._detailGraph
  }

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
    })
    this._selectedDimensions = dimensions
  }

  /**
   * Returns the reference data point.
   *
   * @type {({
   *     dataPoint: DetailDataPoint
   *     dimension: Dimension
   *   } | null)}
   * @memberof detailGraphStore
   */
  get referenceDatapoint(): {
    dataPoint: DetailDataPoint
    dimension: Dimension
  } | null {
    if (!this._referenceDatapoint) {
      return null
    }
    return this._referenceDatapoint
  }

  /**
   * Sets the reference data point.
   *
   * @memberof RepoDetailStore
   */
  set referenceDatapoint(
    datapoint: {
      dataPoint: DetailDataPoint
      dimension: Dimension
    } | null
  ) {
    this._referenceDatapoint = datapoint
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
  }

  get startTime(): Date {
    return this._startTime
  }

  set startTime(start: Date) {
    this._startTime = start
  }

  get endTime(): Date {
    return this._endTime
  }

  set endTime(end: Date) {
    this._endTime = end
  }

  get duration(): number {
    return this._duration
  }

  set duration(duration: number) {
    this._duration = duration
  }
}
