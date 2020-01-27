import { createModule, mutation, action } from 'vuex-class-component'
import {
  CommitComparison,
  Commit,
  Measurement,
  MeasurementID,
  Run,
  Difference
} from '@/store/types'
import axios from 'axios'
import {
  commitFromJson,
  comparisonFromJson
} from '@/util/CommitComparisonJsonHelper'

const VxModule = createModule({
  namespaced: 'newsModule',
  strict: false
})

export class NewsStore extends VxModule {
  private _recentRuns: CommitComparison[] = []
  private _recentSignificantRuns: CommitComparison[] = []

  @action
  async fetchRuns(payload: {
    amount: number
    significant: boolean
  }): Promise<CommitComparison[]> {
    const response = await axios.get('/recently-benchmarked-commits', {
      snackbarTag: 'recent-commits',
      params: {
        amount: payload.amount,
        significant_only: payload.significant
      }
    })

    let jsonComparisons: any[] = response.data.commits

    let comparisons: CommitComparison[] = jsonComparisons.map(it =>
      comparisonFromJson(it)
    )

    return comparisons
  }

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
    let payload = { amount: amount, significant: false }
    let comparisons = await this.fetchRuns(payload)

    this.setRecentRuns(comparisons)
    return comparisons
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
    let payload = { amount: amount, significant: true }
    let comparisons = await this.fetchRuns(payload)

    this.setRecentSignificantRuns(comparisons)
    return comparisons
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
