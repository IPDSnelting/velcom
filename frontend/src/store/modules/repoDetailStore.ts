import { createModule, mutation, action } from 'vuex-class-component'
import {
  CommitComparison,
  Run,
  Difference,
  MeasurementID,
  Commit,
  Measurement
} from '@/store/types'
import Vue from 'vue'
import axios from 'axios'

const VxModule = createModule({
  namespaced: 'repoDetailModule',
  strict: false
})

export class RepoDetailStore extends VxModule {
  private comparisonsByRepoId: { [key: string]: CommitComparison[] } = {}

  /**
   * Fetches all data points for a given repo.
   *
   * @param {string} id the id of the repo
   * @returns {Promise<CommitComparison[]>} a promise containing the fetched
   * datapoints
   * @memberof RepoDetailStore
   */
  @action
  async fetchRepoDatapoints(payload: {
    id: string
    amount: number
    skip: number
  }): Promise<CommitComparison[]> {
    const response = await axios.get('/commit-history', {
      params: {
        repo_id: payload.id,
        amount: payload.amount,
        skip: payload.skip
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
    this.setRepoComparisons({ repoId: payload.id, comparisons: comparisons })
    return this.repoDatapoints(payload.id)
  }

  /**
   * Sets the repo comparisons for a single repo.
   *
   * @param {{
   *     repoId: string
   *     comparisons: CommitComparison[]
   *   }} payload the payload to set it with
   * @memberof RepoDetailStore
   */
  @mutation
  setRepoComparisons(payload: {
    repoId: string
    comparisons: CommitComparison[]
  }) {
    Vue.set(
      this.comparisonsByRepoId,
      payload.repoId,
      payload.comparisons.slice() // copy it
    )
  }

  /**
   * Returns the `CommitComparison`s for a single repository.
   *
   * Returns an empty array if there are none or the repo id is unknown.
   * @readonly
   * @memberof RepoDetailStore
   */
  get repoDatapoints(): (repoId: string) => CommitComparison[] {
    return (repoId: string) => this.comparisonsByRepoId[repoId] || []
  }

  get repoRuns(): (repoId: string) => Run[] {
    return (repoId: string) => {
      let runs: Run[] = []
      if (this.comparisonsByRepoId[repoId]) {
        this.comparisonsByRepoId[repoId].forEach(comparison => {
          if (runs.indexOf(comparison.first) === -1) {
            runs.push(comparison.first)
          }
        })
      }
      return runs
    }
  }
}
