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
      params: {
        amount: payload.amount,
        significant_only: payload.significant
      }
    })

    let comparisons: CommitComparison[] = []
    let jsonComparisons: any[] = response.data.commits

    jsonComparisons.forEach((comparison: any) => {
      let firstCommit: Commit = new Commit(
        comparison.first.commit.repo_id,
        comparison.first.commit.hash,
        comparison.first.commit.author,
        comparison.first.commit.author_date,
        comparison.first.commit.committer,
        comparison.first.commit.committer_date,
        comparison.first.commit.message,
        comparison.first.commit.parents
      )
      let secondCommit: Commit = new Commit(
        comparison.second.commit.repo_id,
        comparison.second.commit.hash,
        comparison.second.commit.author,
        comparison.second.commit.author_date,
        comparison.second.commit.committer,
        comparison.second.commit.committer_date,
        comparison.second.commit.message,
        comparison.second.commit.parents
      )

      let firstMeasurements: Measurement[] = []
      if (comparison.first.measurements) {
        comparison.first.measurements.forEach((measurement: any) => {
          let id: MeasurementID = new MeasurementID(
            measurement.benchmark,
            measurement.metric
          )
          firstMeasurements.push(
            new Measurement(
              id,
              measurement.unit,
              measurement.interpretation,
              measurement.values,
              measurement.value,
              measurement.error_message
            )
          )
        })
      }
      let secondMeasurements: Measurement[] = []
      if (comparison.second.measurements) {
        comparison.second.measurements.forEach((measurement: any) => {
          let id: MeasurementID = new MeasurementID(
            measurement.benchmark,
            measurement.metric
          )
          firstMeasurements.push(
            new Measurement(
              id,
              measurement.unit,
              measurement.interpretation,
              measurement.values,
              measurement.value,
              measurement.error_message
            )
          )
        })
      }

      let first = new Run(
        firstCommit,
        comparison.first.start_time,
        comparison.first.stop_time,
        firstMeasurements,
        comparison.first.error_message
      )
      let second = new Run(
        secondCommit,
        comparison.second.start_time,
        comparison.second.stop_time,
        secondMeasurements,
        comparison.second.error_message
      )

      let differences: Difference[] = []
      comparison.differences.forEach((difference: any) => {
        let measurement = new MeasurementID(
          difference.benchmark,
          difference.metric
        )
        differences.push(new Difference(measurement, difference.difference))
      })
      comparisons.push(new CommitComparison(first, second, differences))
    })

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
  get recentRuns(): Run[] {
    let runs: Run[] = []
    this._recentRuns.forEach(comparison => {
      if (runs.indexOf(comparison.second!) === -1) {
        runs.push(comparison.second!)
      }
    })
    return runs
  }

  /**
   * Returns the recent runs that match a significance threshold.
   *
   * @readonly
   * @type {CommitComparison[]}
   * @memberof NewsModuleStore
   */
  get recentSignificantRuns(): Run[] {
    let runs: Run[] = []
    this._recentSignificantRuns.forEach(comparison => {
      if (runs.indexOf(comparison.second!) === -1) {
        runs.push(comparison.second!)
      }
    })
    return runs
  }
}
