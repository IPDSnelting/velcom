import { createModule, mutation, action } from 'vuex-class-component'
import { RepoId, Dimension, DataPoint } from '@/store/types'
import axios from 'axios'
import { dataPointFromJson } from '@/util/GraphJsonHelper'

const VxModule = createModule({
  namespaced: 'deatilGraphModule',
  strict: false
})

export class DetailGraphStore extends VxModule {
  private _detailGraph: DataPoint[] = []
  private _selectedRepoId: RepoId = ''
  // Needs to be a primitive so persistence works.
  private _selectedDimensions: Dimension[] = []
  // Not a real object, needs to be translated so persistence works.
  private _referenceDatapoint: {
    dataPoint: DataPoint
    dimension: Dimension
  } | null = null

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
  private _defaultStopTime: Date = new Date()
  private startTime: Date = this._defaultStartTime
  private stopTime: Date = this._defaultStopTime

  /**
   * Fetches the data neccessary to display the data points
   * in a detail graph of a given time frame.
   *
   * @param {{
   *     repoId: RepoId
   *     startTime: Date
   *     endTime: Date
   *     duration: number
   *     dimensions: Dimension[]
   *   }} payload
   * @returns {Promise<DataPoint[]>}
   * @memberof detailGraphStore
   */
  @action
  async fetchDetailGraph(payload: {
    repoId: RepoId
    startTime: Date
    endTime: Date
    duration: number
    dimensions: Dimension[]
  }): Promise<DataPoint[]> {
    const response = await axios.get(`/graph/deatail/${payload.repoId}`, {
      snackbarTag: 'commit-history',
      params: {
        start_time: payload.startTime,
        end_time: payload.endTime,
        duration: payload.duration,
        dimensions: this.formatDimensions(payload.dimensions)
      }
    })

    const dataPoints: DataPoint[] = response.data.commits.map((it: any) =>
      dataPointFromJson(it)
    )

    this.setDetailGraph(dataPoints)

    return dataPoints
  }

  /**
   * Builds a string out of the requested dimensions for a detail graph.
   *
   * @private
   * @param {Dimension[]} dimensions
   * @returns {string}
   * @memberof detailGraphStore
   */
  private formatDimensions(dimensions: Dimension[]): string {
    const resultString: string = ''

    const groupedDimensions = dimensions.reduce(
      (benchmarkGroup: { [key: string]: any[] }, cur: Dimension) => {
        ;(benchmarkGroup[cur.benchmark] =
          benchmarkGroup[cur.benchmark] || []).push(cur)
        console.log(benchmarkGroup)
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
      resultString.concat(benchmarkGroup[0].benchmark)
      benchmarkGroup.forEach((dimension: Dimension) => {
        resultString.concat(':' + dimension.metric)
      })
      resultString.concat('::')
    })

    return resultString.slice(0, resultString.length - 2)
  }

  @mutation
  setDetailGraph(graph: DataPoint[]): void {
    this._detailGraph = graph
  }

  /**
   * Returns the locally stored data for a repo detail graph
   *
   * @readonly
   * @type {DataPoint[]}
   * @memberof detailGraphStore
   */
  get detailGraph(): DataPoint[] {
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
  set selectedMeasurements(dimensions: Dimension[]) {
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
   *     dataPoint: DataPoint
   *     dimension: Dimension
   *   } | null)}
   * @memberof detailGraphStore
   */
  get referenceDatapoint(): {
    dataPoint: DataPoint
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
      dataPoint: DataPoint
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
  get selectedRepoId(): string {
    return this._selectedRepoId
  }

  /**
   * Sets the id of the repo that is currently selected in the repo detail view.
   *
   * @memberof RepoDetailStore
   */
  set selectedRepoId(selectedRepoId: string) {
    this._selectedRepoId = selectedRepoId
  }
}
