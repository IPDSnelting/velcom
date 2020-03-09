import {
  createModule,
  mutation,
  action,
  getRawActionContext
} from 'vuex-class-component'
import { Datapoint, Repo, Commit } from '@/store/types'
import Vue from 'vue'
import axios from 'axios'
import { vxm } from '..'
import { runFromJson, commitFromJson } from '@/util/CommitComparisonJsonHelper'

const VxModule = createModule({
  namespaced: 'repoComparisonModule',
  strict: false
})

export class RepoComparisonStore extends VxModule {
  private _selectedRepos: string[] = []
  private _selectedBranchesByRepoID: { [key: string]: string[] } = {}
  private _datapointsByRepoId: { [key: string]: Datapoint[] } = {}
  private _interpretation: 'LESS_IS_BETTER' | 'MORE_IS_BETTER' | 'NEUTRAL' =
    'NEUTRAL'
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

  @action
  async fetchComparisonData(payload: {
    benchmark: string
    metric: string
    startTime?: string | null
    stopTime?: string | null
  }): Promise<{ [key: string]: Datapoint[] }> {
    this.cleanupSelectedBranches()

    let effectiveStartTime: number | undefined
    if (payload.startTime) {
      effectiveStartTime = new Date(payload.startTime).getTime()
    } else if (payload.startTime === null) {
      effectiveStartTime = undefined
    } else {
      effectiveStartTime = this.startDate.getTime() / 1000
    }

    let effectiveStopTime: number | undefined
    if (payload.stopTime) {
      effectiveStopTime = new Date(payload.stopTime).getTime()
    } else if (payload.startTime === null) {
      effectiveStopTime = undefined
    } else {
      effectiveStopTime = this.stopDate.getTime() / 1000 + 60 * 60 * 24
    }

    const response = await axios.post(
      '/repo-comparison-graph',
      {
        repos: this.selectedReposWithBranches,
        start_time: effectiveStartTime,
        stop_time: effectiveStopTime,
        benchmark: payload.benchmark,
        metric: payload.metric
      },
      { snackbarTag: 'repo-comparison' }
    )

    let datapoints: { [key: string]: Datapoint[] } = {}
    let jsonRepos: any[] = response.data.repos

    jsonRepos.forEach((item: any) => {
      datapoints[item.repo_id] = item.commits.map(
        (datapoint: any) =>
          new Datapoint(commitFromJson(datapoint.commit), datapoint.value)
      )
    })

    this.setDatapoints(datapoints)

    // ugly and wrong, only for demonstration purposes
    this._interpretation = jsonRepos[0].interpretation
    this._unit = jsonRepos[0].unit

    return this.allDatapoints
  }

  /**
   * Deletes all selected repositories that are no longer found in repoModule.allRepos.
   *
   * @memberof RepoComparisonStore
   */
  @mutation
  cleanupSelectedBranches() {
    // cleanup selected branches
    let allRepos = vxm.repoModule.allRepos
    let keysToRemove = Object.keys(this._selectedBranchesByRepoID)
    allRepos.forEach(repo => {
      let index = keysToRemove.findIndex(it => it === repo.id)
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
  }) {
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
  setDatapoints(payload: { [key: string]: Datapoint[] }) {
    this._datapointsByRepoId = {} // reset it
    Array.from(Object.keys(payload)).forEach(key => {
      Vue.set(this._datapointsByRepoId, key, payload[key])
    })
  }

  get interpretation(): 'LESS_IS_BETTER' | 'MORE_IS_BETTER' | 'NEUTRAL' {
    return this._interpretation
  }

  set interpretation(
    interpretation: 'LESS_IS_BETTER' | 'MORE_IS_BETTER' | 'NEUTRAL'
  ) {
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

  get referenceDatapoint(): Datapoint | undefined {
    if (
      this.referenceCommit === null ||
      this._datapointsByRepoId[this.referenceCommit.repoID] === undefined
    ) {
      return undefined
    }

    return this._datapointsByRepoId[this.referenceCommit.repoID].find(it => {
      return (
        this.referenceCommit !== null &&
        it.commit.hash === this.referenceCommit.hash
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
  get allDatapoints(): { [key: string]: Datapoint[] } {
    return this._datapointsByRepoId
  }

  get runsByRepoID(): (repoID: string) => Datapoint[] {
    return (repoID: string) => this._datapointsByRepoId[repoID]
  }

  get selectedRepos(): string[] {
    return this._selectedRepos
  }

  set selectedRepos(selectedRepos: string[]) {
    this._selectedRepos = selectedRepos
  }

  get selectedBranchesByRepoID(): { [key: string]: string[] } {
    return (
      vxm.repoModule.allRepos
        .map(repo => ({
          id: repo.id,
          branches:
            // all branches are selected if user has never selected any manually
            this._selectedBranchesByRepoID[repo.id] || repo.branches.slice()
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
    let repos: { repo_id: string; branches: string[] }[] = []
    Object.keys(this.selectedBranchesByRepoID).forEach(repoID => {
      if (
        this.selectedRepos.includes(repoID) &&
        this.selectedBranchesByRepoID[repoID].length !== 0
      ) {
        repos.push({
          repo_id: repoID,
          branches: this.selectedBranchesByRepoID[repoID]
        })
      }
    })
    return repos
  }

  get defaultStartTime() {
    return this._defaultStartTime
  }

  get defaultStopTime() {
    return this._defaultStopTime
  }
}
