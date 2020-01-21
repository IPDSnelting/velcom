import { createModule, mutation, action } from 'vuex-class-component'
import { Run, Repo } from '@/store/types'
import Vue from 'vue'
import axios from 'axios'
const VxModule = createModule({
  namespaced: 'repoComparisonModule',
  strict: false
})

export class RepoComparisonStore extends VxModule {
  private _selectedRepos: string[] = []
  private _selectedBranchesByRepoID: { [key: string]: string[] } = {}
  private runsByRepoId: { [key: string]: Run[] } = {}

  /**
   * Fetches all data points.
   *
   * @param {{
   *     repos: string[]
   *     startTime: string
   *     endTime: string
   *   }} payload the payload with the parameters to pass to the server
   * @returns {Promise<{ [key: string]: Run[] }>} a promise containing all runs
   * @memberof RepoComparisonStore
   */
  @action
  async fetchDatapoints(payload: {
    startTime: number
    endTime: number
    benchmark: string
    metric: string
  }): Promise<{ [key: string]: Run[] }> {
    const response = await axios.post('/repo-comparison-graph', {
      repos: this.selectedReposWithBranches,
      start_time: payload.startTime,
      end_time: payload.endTime,
      benchmark: payload.benchmark,
      metric: payload.metric
    })

    let datapoints: { [key: string]: Run[] } = {}
    let jsonData: any[] = response.data.repos

    jsonData.forEach((item: any) => {
      var runs: Run[] = []
      item.runs.forEach((run: any) => {
        runs.push(
          new Run(
            run.commit,
            run.start_time,
            run.stop_time,
            run.measurements,
            run.error_mesage
          )
        )
      })
      datapoints[item.repo.id] = item.runs
    })

    this.setDataPoints(datapoints)
    return this.allRuns
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
  setDataPoints(payload: { [key: string]: Run[] }) {
    this.runsByRepoId = {} // reset it
    Array.from(Object.keys(payload)).forEach(key => {
      Vue.set(this.runsByRepoId, key, payload[key])
    })
  }

  /**
   * Returns all known runs.
   *
   * @readonly
   * @type {{ [key: string]: Run[] }}
   * @memberof RepoComparisonStore
   */
  get allRuns(): { [key: string]: Run[] } {
    return this.runsByRepoId
  }

  get selectedRepos(): string[] {
    return this._selectedRepos
  }

  set selectedRepos(selectedRepos: string[]) {
    this._selectedRepos = selectedRepos
  }

  get selectedBranchesByRepoID(): { [key: string]: string[] } {
    return this._selectedBranchesByRepoID
  }

  get selectedReposWithBranches(): string[] {
    let repos: any[] = []
    Object.keys(this._selectedBranchesByRepoID).forEach(repoID => {
      if (
        this.selectedRepos.indexOf(repoID) > -1 &&
        this._selectedBranchesByRepoID[repoID].length !== 0
      ) {
        repos.push({
          repo_id: repoID,
          branches: this._selectedBranchesByRepoID[repoID]
        })
      }
    })
    return repos
  }
}
