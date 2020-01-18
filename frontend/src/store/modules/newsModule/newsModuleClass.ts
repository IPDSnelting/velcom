import { createModule, mutation, action } from 'vuex-class-component'
import { CommitComparison } from '@/store/types'
import axios from 'axios'

const VxModule = createModule({
  namespaced: 'newsModule',
  strict: false
})

export class NewsModuleStore extends VxModule {
  private _recentRuns: CommitComparison[] = []
  private _recentSignificantRuns: CommitComparison[] = []

  /**
   * Fetches all recent runs from the server.
   *
   * @param {number} amount the amount of runs to fetch
   * @returns {Promise<CommitComparison[]>} a promise resolving with the
   * reactive recent runs
   * @memberof NewsModuleStore
   */
  @action
  async fetchRecentRuns(amount: number): Promise<CommitComparison[]> {
    const response = await axios.get('/recently-benchmarked-commits', {
      params: {
        amount: amount,
        significant_only: false
      }
    })

    let runs: CommitComparison[] = []
    let jsonData: any[] = response.data.commits

    jsonData.forEach((item: any) => {
      runs.push(new CommitComparison(item.first, item.second, item.differences))
    })

    this.setRecentRuns(runs)
    return this.recentRuns
  }

  /**
   * Fetches all recent significant runs from the server.
   *
   * @param {number} amount the amount of runs to fetch
   * @returns {Promise<CommitComparison[]>} a promise resolving with the
   * reactive recent runs
   * @memberof NewsModuleStore
   */
  @action
  async fetchRecentSignificantRuns(
    amount: number
  ): Promise<CommitComparison[]> {
    const response = await axios.get('/recently-benchmarked-commits', {
      params: {
        amount: amount,
        significant_only: true
      }
    })

    let runs: CommitComparison[] = []
    let jsonData: any[] = response.data.commits

    jsonData.forEach((item: any) => {
      runs.push(new CommitComparison(item.first, item.second, item.differences))
    })

    this.setRecentSignificantRuns(runs)
    return runs
  }

  /**
   * Sets the recent runs.
   *
   * @param {CommitComparison[]} recentRuns the recent runs
   * @memberof NewsModuleStore
   */
  @mutation
  setRecentRuns(recentRuns: CommitComparison[]) {
    this._recentRuns = recentRuns.slice()
  }

  /**
   * Sets the recent significant runs.
   *
   * @param {CommitComparison[]} recentSignificantRuns the new runs
   * @memberof NewsModuleStore
   */
  @mutation
  setRecentSignificantRuns(recentSignificantRuns: CommitComparison[]) {
    this._recentSignificantRuns = recentSignificantRuns.slice()
  }

  /**
   * Returns all recent runs.
   *
   * @readonly
   * @type {CommitComparison[]}
   * @memberof NewsModuleStore
   */
  get recentRuns(): CommitComparison[] {
    return this._recentRuns
  }

  /**
   * Returns the recent runs that match a significance threshold.
   *
   * @readonly
   * @type {CommitComparison[]}
   * @memberof NewsModuleStore
   */
  get recentSignificantRuns(): CommitComparison[] {
    return this._recentSignificantRuns
  }
}
