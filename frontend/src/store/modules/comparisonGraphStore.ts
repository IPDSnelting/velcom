import { createModule, mutation, action } from 'vuex-class-component'
import { DataPoint, Commit, DimensionInterpretation } from '@/store/types'
import Vue from 'vue'
import axios from 'axios'
import { vxm } from '..'
import { dataPointFromJson } from '@/util/GraphJsonHelper'

const VxModule = createModule({
  namespaced: 'repoComparisonModule',
  strict: false
})

export class ComparisonGraphStore extends VxModule {
  private _selectedRepos: string[] = []
  private _selectedBranchesByRepoID: { [key: string]: string[] } = {}
  private _datapointsByRepoId: { [key: string]: DataPoint[] } = {}

  private _interpretation: DimensionInterpretation = 'NEUTRAL'
  private _unit: string = ''

  referenceCommit: Commit | null = null

  selectedBenchmark: string = ''
  selectedMetric: string = ''

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
  }): Promise<{ [key: string]: DataPoint[] }> {
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

    const response = await axios.post(
      '/graph/comparison',
      {
        repos: this.formatRepos,
        start_time: effectiveStartTime,
        stop_time: effectiveEndTime,
        benchmark: payload.benchmark,
        metric: payload.metric
      },
      { snackbarTag: 'repo-comparison' }
    )

    const datapoints: { [key: string]: DataPoint[] } = {}
    const jsonRepos: any[] = response.data.repos

    jsonRepos.forEach((item: any) => {
      datapoints[item.repo_id] = item.commits.map((it: any) =>
        dataPointFromJson(it)
      )
    })

    this.setDatapoints(datapoints)
    // TODO: where does the following line come from? I'm scared.
    // ugly and wrong, only for demonstration purposes
    this._interpretation = jsonRepos[0].interpretation
    this._unit = jsonRepos[0].unit

    return this.allDatapoints
  }

  /**
   * Builds a string out of the requested repos and branches
   *
   * @private
   * @returns {string}
   * @memberof ComparisonGraphStore
   */
  private formatRepos(): string {
    const resultString: string = ''

    this.selectedReposWithBranches.forEach(
      (repo: { repo_id: string; branches: string[] }) => {
        resultString.concat(repo.repo_id)
        repo.branches.forEach((branch: string) => {
          resultString.concat(':' + branch)
        })
        resultString.concat('::')
      }
    )

    return resultString.slice(0, resultString.length - 2)
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
    const keysToRemove = Object.keys(this._selectedBranchesByRepoID)
    allRepos.forEach(repo => {
      const index = keysToRemove.findIndex(it => it === repo.id)
      if (index >= 0) {
        keysToRemove.splice(index, 1)
      }
    })
    keysToRemove.forEach(key => {
      Vue.delete(this._selectedBranchesByRepoID, key)
    })
  }

  @mutation
  setSelectedBranchesForRepo(payload: {
    repoID: string
    selectedBranches: string[]
  }): void {
    Vue.set(
      this._selectedBranchesByRepoID,
      payload.repoID,
      payload.selectedBranches
    )
  }

  /**
   * Sets all data points.
   *
   * @param {{ [key: string]: Run[] }} payload the payload
   * to set it with
   * @memberof RepoComparisonStore
   */
  @mutation
  setDatapoints(payload: { [key: string]: DataPoint[] }): void {
    this._datapointsByRepoId = {} // reset it
    Array.from(Object.keys(payload)).forEach(key => {
      Vue.set(this._datapointsByRepoId, key, payload[key])
    })
  }

  get interpretation(): DimensionInterpretation {
    return this._interpretation
  }

  set interpretation(interpretation: DimensionInterpretation) {
    this._interpretation = interpretation
  }

  get unit(): string {
    return this._unit
  }

  set unit(unit: string) {
    this._unit = unit
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

  get referenceDatapoint(): DataPoint | undefined {
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
  get allDatapoints(): { [key: string]: DataPoint[] } {
    return this._datapointsByRepoId
  }

  get runsByRepoID(): (repoID: string) => DataPoint[] {
    return (repoID: string) => this._datapointsByRepoId[repoID]
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
              this._selectedBranchesByRepoID[repo.id] || repo.branches.slice()
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

  // eslint-disable-next-line camelcase
  get selectedReposWithBranches(): { repo_id: string; branches: string[] }[] {
    // eslint-disable-next-line camelcase
    const repos: { repo_id: string; branches: string[] }[] = []
    Object.keys(this.selectedBranchesByRepoId).forEach(repoID => {
      if (
        this.selectedRepos.includes(repoID) &&
        this.selectedBranchesByRepoId[repoID].length !== 0
      ) {
        repos.push({
          repo_id: repoID,
          branches: this.selectedBranchesByRepoId[repoID]
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
