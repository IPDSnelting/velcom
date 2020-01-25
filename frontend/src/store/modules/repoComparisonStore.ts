import {
  createModule,
  mutation,
  action,
  getRawActionContext
} from 'vuex-class-component'
import { Run, Repo } from '@/store/types'
import Vue from 'vue'
import axios from 'axios'
import { vxm } from '..'
import { runFromJson } from '@/util/CommitComparisonJsonHelper'

const VxModule = createModule({
  namespaced: 'repoComparisonModule',
  strict: false
})

export class RepoComparisonStore extends VxModule {
  private _selectedRepos: string[] = []
  private _selectedBranchesByRepoID: { [key: string]: string[] } = {}
  private _runsByRepoId: { [key: string]: Run[] } = {}
  // One week in the past
  private startTime: string = new Date(
    new Date().setDate(new Date().getDate() - 7)
  )
    .toISOString()
    .substring(0, 10)
  private stopTime: string = new Date().toISOString().substring(0, 10)

  @action
  async fetchDatapoints(payload: {
    benchmark: string
    metric: string
  }): Promise<{ [key: string]: Run[] }> {
    this.cleanupSelectedBranches()

    const response = await axios.post('/repo-comparison-graph', {
      repos: this.selectedReposWithBranches,
      start_time: this.startDate.getTime() / 1000,
      stop_time: this.stopDate.getTime() / 1000,
      benchmark: payload.benchmark,
      metric: payload.metric
    })

    let datapoints: { [key: string]: Run[] } = {}
    let jsonData: any[] = response.data.repos

    jsonData.forEach((item: any) => {
      datapoints[item.repo.id] = item.runs.map((run: any) => runFromJson(run))
    })

    this.setDataPoints(datapoints)
    return this.allRuns
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
  setDataPoints(payload: { [key: string]: Run[] }) {
    this._runsByRepoId = {} // reset it
    Array.from(Object.keys(payload)).forEach(key => {
      Vue.set(this._runsByRepoId, key, payload[key])
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

  /**
   * Returns all known runs.
   *
   * @readonly
   * @type {{ [key: string]: Run[] }}
   * @memberof RepoComparisonStore
   */
  get allRuns(): { [key: string]: Run[] } {
    return this._runsByRepoId
  }

  get runsByRepoID(): (repoID: string) => Run[] {
    return (repoID: string) => this._runsByRepoId[repoID]
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
