import { createModule, mutation, action } from 'vuex-class-component'
import { ComparisonDataPoint, Dimension } from '@/store/types'
import Vue from 'vue'
import axios from 'axios'
import { vxm } from '..'
import { comparisonDataPointFromJson } from '@/util/GraphJsonHelper'
import { dimensionFromJson } from '@/util/RepoJsonHelper'

const VxModule = createModule({
  namespaced: 'comparisonGraphModule',
  strict: false
})

export function comparisonGraphStoreFromJson(json?: string): any {
  return json ? JSON.parse(json) : {}
}

export function comparisonGraphStoreToJson(
  store: ComparisonGraphStore
): string {
  return JSON.stringify({
    _selectedRepos: (store as any)._selectedRepos,
    _selectedBranchesByRepoId: (store as any)._selectedBranchesByRepoId,
    startTime: (store as any).startTime,
    stopTime: (store as any).stopTime,
    selectedMetric: store.selectedMetric,
    selectedBenchmark: store.selectedBenchmark
  })
}

/**
 * Builds a string out of the requested repos and branches
 *
 * @returns {string}
 */
function formatRepos(repos: { repoId: string; branches: string[] }[]): string {
  return (
    repos
      // ignore repos without branches, as we later assume it has at least one
      .filter(it => it.branches.length > 0)
      .map(({ repoId, branches }) => repoId + ':' + branches.join(':'))
      .join('::')
  )
}

export class ComparisonGraphStore extends VxModule {
  private _selectedRepos: string[] = []
  private _selectedBranchesByRepoId: { [key: string]: string[] } = {}
  private _datapointsByRepoId: {
    [key: string]: ComparisonDataPoint[]
  } = {}

  referenceCommit: ComparisonDataPoint | null = null

  selectedBenchmark: string = ''
  selectedMetric: string = ''
  selectedDimension: Dimension | null = null

  // One week in the past
  private _defaultStartTime: string = new Date(
    new Date().setDate(new Date().getDate() - 7)
  )
    .toISOString()
    .substring(0, 10)
  private _defaultStopTime: string = new Date().toISOString().substring(0, 10)

  private startTime: string = this._defaultStartTime
  private stopTime: string = this._defaultStopTime

  /**
   * Fetches the data neccessary to display the data points
   * of the comparison graph in given time frame.
   *
   * @param {({
   *     benchmark: string
   *     metric: string
   *     startTime?: string | null
   *     endTime?: string | null
   *   })} payload
   * @returns {Promise<{ [key: string]: DataPoint[] }>}
   * @memberof ComparisonGraphStore
   */
  @action
  async fetchComparisonGraph(payload: {
    benchmark: string
    metric: string
    startTime?: string | null
    endTime?: string | null
  }): Promise<{ [key: string]: ComparisonDataPoint[] }> {
    this.cleanupSelectedBranches()

    let effectiveStartTime: number | undefined
    if (payload.startTime) {
      effectiveStartTime = new Date(payload.startTime).getTime()
    } else if (payload.startTime === null) {
      effectiveStartTime = undefined
    } else {
      effectiveStartTime = this.startDate.getTime() / 1000
    }

    let effectiveEndTime: number | undefined
    if (payload.endTime) {
      effectiveEndTime = new Date(payload.endTime).getTime()
    } else if (payload.startTime === null) {
      effectiveEndTime = undefined
    } else {
      effectiveEndTime = this.stopDate.getTime() / 1000 + 60 * 60 * 24
    }

    const response = await axios.get('/graph/comparison', {
      params: {
        repos: formatRepos(this.selectedReposWithBranches),
        start_time: effectiveStartTime,
        stop_time: effectiveEndTime,
        dimension: `${payload.benchmark}:${payload.metric}`
      },
      snackbarTag: 'repo-comparison'
    })

    const datapoints: { [key: string]: ComparisonDataPoint[] } = {}
    const jsonRepos: any[] = response.data.repos

    jsonRepos.forEach((item: any) => {
      datapoints[item.repo_id] = item.commits.map((datapoint: any) =>
        comparisonDataPointFromJson(datapoint, item.repo_id)
      )
    })

    this.setDatapoints(datapoints)
    this.selectedDimension = dimensionFromJson(response.data.dimension)

    return this.allDatapoints
  }

  /**
   * Deletes all selected repositories that are no longer found in repoModule.allRepos.
   *
   * @memberof RepoComparisonStore
   */
  @mutation
  cleanupSelectedBranches(): void {
    // cleanup selected branches
    const allRepos = vxm.repoModule.allRepos
    const keysToRemove = Object.keys(this._selectedBranchesByRepoId)
    allRepos.forEach(repo => {
      const index = keysToRemove.findIndex(it => it === repo.id)
      if (index >= 0) {
        keysToRemove.splice(index, 1)
      }
    })
    keysToRemove.forEach(key => {
      Vue.delete(this._selectedBranchesByRepoId, key)
    })
  }

  @mutation
  setSelectedBranchesForRepo(payload: {
    repoId: string
    selectedBranches: string[]
  }): void {
    Vue.set(
      this._selectedBranchesByRepoId,
      payload.repoId,
      payload.selectedBranches
    )
  }

  /**
   * Sets all data points.
   *
   * @param payload the payload to set it with
   * @memberof RepoComparisonStore
   */
  @mutation
  setDatapoints(payload: { [key: string]: ComparisonDataPoint[] }): void {
    // TODO: Is this reactive?
    this._datapointsByRepoId = {} // reset it
    Array.from(Object.keys(payload)).forEach(key => {
      Vue.set(this._datapointsByRepoId, key, payload[key])
    })
  }

  get startDate(): Date {
    return new Date(this.startTime)
  }

  set startDate(start: Date) {
    this.startTime = start.toISOString().substring(0, 10)
  }

  get stopDate(): Date {
    return new Date(this.stopTime)
  }

  set stopDate(stop: Date) {
    this.stopTime = stop.toISOString().substring(0, 10)
  }

  get referenceDatapoint(): ComparisonDataPoint | undefined {
    if (
      this.referenceCommit === null ||
      this._datapointsByRepoId[this.referenceCommit.repoId] === undefined
    ) {
      return undefined
    }

    return this._datapointsByRepoId[this.referenceCommit.repoId].find(it => {
      return (
        this.referenceCommit !== null && it.hash === this.referenceCommit.hash
      )
    })
  }

  /**
   * Returns all known runs.
   *
   * @readonly
   * @type {{ [key: string]: Run[] }}
   * @memberof RepoComparisonStore
   */
  get allDatapoints(): { [key: string]: ComparisonDataPoint[] } {
    return this._datapointsByRepoId
  }

  get runsByRepoId(): (repoId: string) => ComparisonDataPoint[] {
    return (repoId: string) => this._datapointsByRepoId[repoId]
  }

  get selectedRepos(): string[] {
    return this._selectedRepos
  }

  set selectedRepos(selectedRepos: string[]) {
    this._selectedRepos = selectedRepos
  }

  get selectedBranchesByRepoId(): { [key: string]: string[] } {
    return (
      vxm.repoModule.allRepos
        .map(repo => ({
          id: repo.id,
          branches:
            // all branches are selected if user has never selected any manually
            (
              this._selectedBranchesByRepoId[repo.id] || repo.branches.slice()
            ).filter(it =>
              repo.branches
                .filter(b => b.tracked)
                .map(b => b.name)
                .includes(it)
            )
        }))
        // reduce list above to required object structure
        .reduce((accumulated, repoBranch) => {
          Vue.set(accumulated, repoBranch.id, repoBranch.branches)
          return accumulated
        }, {})
    )
  }

  get selectedReposWithBranches(): { repoId: string; branches: string[] }[] {
    const repos: { repoId: string; branches: string[] }[] = []
    Object.keys(this.selectedBranchesByRepoId).forEach(repoId => {
      if (
        this.selectedRepos.includes(repoId) &&
        this.selectedBranchesByRepoId[repoId].length !== 0
      ) {
        repos.push({
          repoId: repoId,
          branches: this.selectedBranchesByRepoId[repoId]
        })
      }
    })
    return repos
  }

  get defaultStartTime(): string {
    return this._defaultStartTime
  }

  get defaultStopTime(): string {
    return this._defaultStopTime
  }
}
