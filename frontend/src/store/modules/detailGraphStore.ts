import { action, createModule, mutation } from 'vuex-class-component'
import {
  DetailDataPoint,
  Dimension,
  DimensionId,
  dimensionIdEqual,
  RepoId
} from '@/store/types'
import axios from 'axios'
import { detailDataPointFromJson } from '@/util/GraphJsonHelper'
import { dimensionFromJson } from '@/util/RepoJsonHelper'
import { CustomKeyEqualsMap } from '@/util/CustomKeyEqualsMap'
import { vxm } from '@/store'

const VxModule = createModule({
  namespaced: 'detailGraphModule',
  strict: false
})

export type DimensionDetailPoint = {
  dataPoint: DetailDataPoint
  dimension: Dimension
}

export class DetailGraphStore extends VxModule {
  private _detailGraph: DetailDataPoint[] = []
  private _selectedRepoId: RepoId = ''
  private _selectedDimensions: Dimension[] = []

  private colorIndexMap: CustomKeyEqualsMap<
    DimensionId,
    number
  > = new CustomKeyEqualsMap([], dimensionIdEqual)
  private firstFreeColorIndex: number = 0

  referenceDatapoint: DimensionDetailPoint | null = null
  commitToCompare: DimensionDetailPoint | null = null

  zoomStartValue: number | null = null
  zoomEndValue: number | null = null

  // One week in the past, as elegant as ever
  startTime: Date = new Date(new Date().setDate(new Date().getDate() - 7))
  endTime: Date = new Date()
  duration: number = 7

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
      this.startTime.getTime() / 1000
    )
    const effectiveEndTime: number = Math.floor(
      this.endTime.getTime() / 1000 + 60 * 60 * 24
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

    const dimensions = response.data.dimensions
      .map(dimensionFromJson)
      // This map is not needed as hopefully the CustomKeyEqualsMap should be used
      // but it increases performance [O(n) => O(1)]
      .map((dim: Dimension) =>
        this._selectedDimensions.find(savedDimension =>
          savedDimension.equals(dim)
        )
      )

    const dataPoints: DetailDataPoint[] = response.data.commits.map((it: any) =>
      detailDataPointFromJson(it, dimensions)
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
      if (!this.colorIndexMap.has(it)) {
        this.colorIndexMap.set(it, this.firstFreeColorIndex++)
      }
    })
    this._selectedDimensions = dimensions
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
    this.colorIndexMap = new CustomKeyEqualsMap([], dimensionIdEqual)
    this.firstFreeColorIndex = 0

    // rebuild selected dimensions so colors are correct
    const repo = vxm.repoModule.repoById(selectedRepoId)

    if (repo) {
      const isInRepo = (dimension: Dimension) =>
        repo.dimensions.find(it => it.equals(dimension)) !== undefined

      // we need to trigger the setter
      vxm.detailGraphModule.selectedDimensions = this._selectedDimensions.filter(
        isInRepo
      )
    } else {
      // we need to trigger the setter
      vxm.detailGraphModule.selectedDimensions = []
    }
  }

  get colorIndex(): (dimension: DimensionId) => number | undefined {
    return dimension => this.colorIndexMap.get(dimension)
  }
}
