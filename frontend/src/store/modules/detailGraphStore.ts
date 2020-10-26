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
import router from '@/router'
import { Route } from 'vue-router'
import { dateFromRelative } from '@/util/TimeUtil'

const VxModule = createModule({
  namespaced: 'detailGraphModule',
  strict: false
})

export type DimensionDetailPoint = {
  dataPoint: DetailDataPoint
  dimension: Dimension
}

export type PermanentLinkOptions = Partial<{
  includeXZoom: boolean
  includeYZoom: boolean
  includeDimensions: boolean
}>

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

  zoomXStartValue: number | null = null
  zoomXEndValue: number | null = null
  zoomYStartValue: number | null = null
  zoomYEndValue: number | null = null

  // One week in the past, as elegant as ever
  startTime: Date = new Date(new Date().setDate(new Date().getDate() - 7))
  endTime: Date = new Date()

  beginYScaleAtZero: boolean = false

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

    const extractFloat: (
      name: string,
      action: (value: number) => void
    ) => void = (name, action) => {
      const queryValue = link.query[name]
      if (queryValue && typeof queryValue === 'string') {
        if (!isNaN(parseFloat(queryValue))) {
          action(parseFloat(queryValue))
        }
      }
    }

    const extractDate: (
      name: string,
      relative: Date,
      action: (value: number) => void
    ) => void = (name, relative, action) => {
      const queryValue = link.query[name]
      if (queryValue && typeof queryValue === 'string') {
        if (queryValue.match(/^([+-])?(\d|\.)+$/)) {
          action(parseFloat(queryValue))
          return
        }
        const relativeDate = dateFromRelative(queryValue, relative)
        console.log(queryValue, relativeDate)
        if (relativeDate) {
          action(relativeDate.getTime())
        }
      }
    }

    // Anchors to the current date
    extractDate('zoomXEnd', new Date(), value => {
      this.zoomXEndValue = value
      this.endTime = new Date(value)
    })
    // Anchors to the end date (or the current one if not specified)
    extractDate(
      'zoomXStart',
      new Date(this.zoomXEndValue || new Date().getTime()),
      value => {
        this.zoomXStartValue = value
        this.startTime = new Date(value)
      }
    )
    extractFloat('zoomYStart', value => {
      this.zoomYStartValue = value
    })
    extractFloat('zoomYEnd', value => {
      this.zoomYEndValue = value
    })

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
  }

  get duration(): number {
    const timeDiff =
      vxm.detailGraphModule.endTime.getTime() -
      vxm.detailGraphModule.startTime.getTime()

    return Math.ceil(timeDiff / (1000 * 3600 * 24))
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
   * Returns a permanent link to the current detail graph state
   */
  get permanentLink(): (options?: PermanentLinkOptions) => string {
    return options => {
      const orUndefined = (it: any) => (it ? '' + it : undefined)
      function respectOptions<T>(
        name: keyof PermanentLinkOptions,
        value: T
      ): T | undefined {
        if (!options || options[name]) {
          return value
        }
        return undefined
      }

      const route = router.resolve({
        name: 'repo-detail',
        params: { id: this.selectedRepoId },
        query: {
          zoomYStart: respectOptions(
            'includeYZoom',
            orUndefined(this.zoomYStartValue)
          ),
          zoomYEnd: respectOptions(
            'includeYZoom',
            orUndefined(this.zoomYEndValue)
          ),
          zoomXStart:
            options && options.includeXZoom
              ? orUndefined(this.zoomXStartValue)
              : orUndefined(this.startTime.getTime()),
          zoomXEnd:
            options && options.includeXZoom
              ? orUndefined(this.zoomXEndValue)
              : orUndefined(this.endTime.getTime()),
          dimensions: respectOptions('includeDimensions', this.dimensionString)
        }
      })

      return decodeURIComponent(location.origin + route.href)
    }
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
        (startValue === null || point.authorDate.getTime() >= startValue) &&
        (endValue === null || point.authorDate.getTime() <= endValue)
      ) {
        visibleDataPoints += this._selectedDimensions.length
      }
    }
    return visibleDataPoints
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
