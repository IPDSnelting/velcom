import { createModule, mutation, action } from 'vuex-class-component'
import { CommitComparison, MeasurementID, Commit } from '@/store/types'
import axios from 'axios'
import { comparisonFromJson } from '@/util/CommitComparisonJsonHelper'
import { vxm } from '..'

const VxModule = createModule({
  namespaced: 'repoDetailModule',
  strict: false
})

export class RepoDetailStore extends VxModule {
  private _repoHistory: {
    commit: Commit
    comparison: CommitComparison
  }[] = []
  private _selectedRepoId: string = ''

  selectedMetric: string = ''
  selectedBenchmark: string = ''
  selectedFetchAmount: string = '10'
  selectedSkipAmount: string = '0'
  referenceDatapoint: {
    commit: Commit
    comparison: CommitComparison
  } | null = null

  @action
  async fetchHistoryForRepo(payload: {
    repoId: string
    amount: number
    skip: number
  }): Promise<{ commit: Commit; comparison: CommitComparison }[]> {
    let response = await axios.get('/commit-history', {
      snackbarTag: 'commit-history',
      params: {
        repo_id: payload.repoId,
        amount: payload.amount,
        skip: payload.skip
      }
    })

    let commitArray: any[] = response.data.commits

    let resultArray: {
      commit: Commit
      comparison: CommitComparison
    }[] = commitArray.map(jsonComparison => {
      const commitComparison: CommitComparison = comparisonFromJson(
        jsonComparison
      )
      return {
        commit: commitComparison.secondCommit,
        comparison: commitComparison
      }
    })

    this.setRepoHistory(resultArray)

    return Promise.resolve(resultArray)
  }

  @action
  dispatchDeleteMeasurements(payload: {
    measurementId: MeasurementID
    repoId: string
  }): Promise<void> {
    return (
      axios
        .delete('/measurements', {
          snackbarTag: 'delete-measurements',
          params: {
            repo_id: payload.repoId,
            benchmark: payload.measurementId.benchmark,
            metric: payload.measurementId.metric
          }
        })
        // udpate repo
        .then(() => {
          return vxm.repoModule.fetchRepoByID(payload.repoId)
        })
        // delete result
        .then(it => {})
    )
  }

  @mutation
  setRepoHistory(history: { commit: Commit; comparison: CommitComparison }[]) {
    this._repoHistory = history
  }

  /**
   * Returns the locally stored history for a single repo.
   *
   * Empty array if there is no fetched history.
   *
   * @readonly
   * @memberof RepoDetailStore
   */
  get repoHistory(): { commit: Commit; comparison: CommitComparison }[] {
    return this._repoHistory
  }

  /**
   * Returns the id of the repo that is currently selected in the repo detail view.
   *
   * Returns an empty string if none.
   *
   * @readonly
   * @type {string}
   * @memberof RepoDetailStore
   */
  get selectedRepoId(): string {
    return this._selectedRepoId
  }

  /**
   * Sets the id of the repo that is currently selected in the repo detail view.
   *
   * @memberof RepoDetailStore
   */
  set selectedRepoId(selectedRepoId: string) {
    this._selectedRepoId = selectedRepoId
  }
}
