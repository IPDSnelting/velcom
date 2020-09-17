import { createModule, mutation, action } from 'vuex-class-component'
import { RunDescriptionWithDifferences, RunDescription } from '@/store/types'
import axios from 'axios'
import {
  differenceFromJson,
  runDescriptionFromJson
} from '@/util/CommitComparisonJsonHelper'

const VxModule = createModule({
  namespaced: 'newsModule',
  strict: false
})

export class NewsStore extends VxModule {
  private _recentRuns: RunDescription[] = []
  private _recentSignificantRuns: RunDescriptionWithDifferences[] = []

  /**
   * Fetches all recent runs from the server.
   *
   * @param {number} amount the amount of runs to fetch
   * @returns {Promise<RunDescription[]>} a promise resolving with the
   * reactive recent runs
   * @memberof NewsModuleStore
   */
  @action
  async fetchRecentRuns(amount: number): Promise<RunDescription[]> {
    const response = await axios.get(`/recent/runs`, {
      params: {
        n: amount
      }
    })

    const runs: RunDescription[] = response.data.runs.map((it: any) =>
      runDescriptionFromJson(it.run)
    )

    this.setRecentRuns(runs)

    return runs
  }

  /**
   * Fetches all recent significant runs from the server.
   *
   * @param {number} amount the amount of runs to fetch
   * @returns {Promise<RunDescriptionWithDifferences[]>} a promise resolving with the
   * reactive recent runs
   * @memberof NewsModuleStore
   */
  @action
  async fetchRecentSignificantRuns(
    amount: number
  ): Promise<RunDescriptionWithDifferences[]> {
    const response = await axios.get(`/recent/runs`, {
      params: {
        n: amount,
        significant: true
      }
    })

    const runs = response.data.runs.map(
      (it: any) =>
        new RunDescriptionWithDifferences(
          runDescriptionFromJson(it.run),
          it.significant_dimensions.map(differenceFromJson)
        )
    )

    this.setRecentSignificantRuns(runs)
    return runs
  }

  /**
   * Sets the recent runs.
   *
   * @param {RunDescription[]} recentRuns the recent runs
   * @memberof NewsModuleStore
   */
  @mutation
  setRecentRuns(recentRuns: RunDescription[]): void {
    this._recentRuns = recentRuns.slice()
  }

  /**
   * Sets the recent significant runs.
   *
   * @param {RunDescriptionWithDifferences[]} recentSignificantRuns the new runs
   * @memberof NewsModuleStore
   */
  @mutation
  setRecentSignificantRuns(
    recentSignificantRuns: RunDescriptionWithDifferences[]
  ): void {
    this._recentSignificantRuns = recentSignificantRuns.slice()
  }

  /**
   * Returns all recent runs.
   *
   * @readonly
   * @type {RunDescription[]}
   * @memberof NewsModuleStore
   */
  get recentRuns(): RunDescription[] {
    return this._recentRuns
  }

  /**
   * Returns the recent runs that match a significance threshold.
   *
   * @readonly
   * @type {RunDescriptionWithDifferences[]}
   * @memberof NewsModuleStore
   */
  get recentSignificantRuns(): RunDescriptionWithDifferences[] {
    return this._recentSignificantRuns
  }
}
