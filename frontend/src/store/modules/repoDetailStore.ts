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
import {
  commitFromJson,
  runFromJson,
  differenceFromJson,
  comparisonFromJson
} from '@/util/CommitComparisonJsonHelper'

const VxModule = createModule({
  namespaced: 'repoDetailModule',
  strict: false
})

export class RepoDetailStore extends VxModule {
  private historyByRepoId: {
    [repoId: string]: { commit: Commit; comparison: CommitComparison }[]
  } = {}
  private _selectedRepoId: string = ''

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
    }[] = commitArray.map(
      ({ commit: jsonCommit, comparison: jsonComparison }) => {
        let commit: Commit = commitFromJson(jsonCommit)
        const commitComparison: CommitComparison = comparisonFromJson(
          jsonComparison
        )
        return { commit: commit, comparison: commitComparison }
      }
    )

    this.setHistoryForRepo({ repoId: payload.repoId, history: resultArray })

    return Promise.resolve(resultArray)
  }

  @mutation
  setHistoryForRepo(payload: {
    repoId: string
    history: { commit: Commit; comparison: CommitComparison }[]
  }) {
    Vue.set(this.historyByRepoId, payload.repoId, payload.history)
  }

  /**
   * Returns the locally stored history for a single repo.
   *
   * Empty array if there is no fetched history.
   *
   * @readonly
   * @memberof RepoDetailStore
   */
  get historyForRepoId(): (
    repoId: string
  ) => { commit: Commit; comparison: CommitComparison }[] {
    return (repoId: string) =>
      this.historyByRepoId[repoId] ? this.historyByRepoId[repoId] : []
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
