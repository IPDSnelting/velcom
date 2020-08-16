import { createModule, mutation, action } from 'vuex-class-component'
import { Run, RunWithDifferences } from '@/store/types'
import axios from 'axios'
import {
  runFromJson,
  differenceFromJson
} from '@/util/CommitComparisonJsonHelper'

const VxModule = createModule({
  namespaced: 'newsModule',
  strict: false
})

export class NewsStore extends VxModule {
  private _recentRuns: Run[] = []
  private _recentSignificantRuns: RunWithDifferences[] = []

  /**
   * Fetches all recent runs from the server.
   *
   * @param {number} amount the amount of runs to fetch
   * @returns {Promise<Run[]>} a promise resolving with the
   * reactive recent runs
   * @memberof NewsModuleStore
   */
  @action
  async fetchRecentRuns(amount: number): Promise<Run[]> {
    const response = await axios.get(`/recent/runs`, {
      params: {
        n: amount
      }
    })

    const runs = response.data.runs.map((it: any) => runFromJson(it.run))

    this.setRecentRuns(runs)

    return runs
  }

  /**
   * Fetches all recent significant runs from the server.
   *
   * @param {number} amount the amount of runs to fetch
   * @returns {Promise<RunWithDifferences[]>} a promise resolving with the
   * reactive recent runs
   * @memberof NewsModuleStore
   */
  @action
  async fetchRecentSignificantRuns(
    amount: number
  ): Promise<RunWithDifferences[]> {
    const response = await axios.get(`/recent/runs`, {
      params: {
        n: amount,
        significant: true
      }
    })

    const runs = response.data.runs.map(
      (it: any) =>
        new RunWithDifferences(
          runFromJson(it.run),
          it.significant_dimensions.map(differenceFromJson)
        )
    )

    this.setRecentSignificantRuns(runs)
    return runs
  }

  /**
   * Sets the recent runs.
   *
   * @param {Run[]} recentRuns the recent runs
   * @memberof NewsModuleStore
   */
  @mutation
  setRecentRuns(recentRuns: Run[]) {
    this._recentRuns = recentRuns.slice()
  }

  /**
   * Sets the recent significant runs.
   *
   * @param {RunWithDifferences[]} recentSignificantRuns the new runs
   * @memberof NewsModuleStore
   */
  @mutation
  setRecentSignificantRuns(recentSignificantRuns: RunWithDifferences[]) {
    this._recentSignificantRuns = recentSignificantRuns.slice()
  }

  /**
   * Returns all recent runs.
   *
   * @readonly
   * @type {Run[]}
   * @memberof NewsModuleStore
   */
  get recentRuns(): Run[] {
    return this._recentRuns
  }

  /**
   * Returns the recent runs that match a significance threshold.
   *
   * @readonly
   * @type {RunWithDifferences[]}
   * @memberof NewsModuleStore
   */
  get recentSignificantRuns(): RunWithDifferences[] {
    return this._recentSignificantRuns
  }
}
