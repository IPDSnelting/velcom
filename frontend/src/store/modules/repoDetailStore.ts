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
  differenceFromJson
} from '@/util/CommitComparisonJsonHelper'

const VxModule = createModule({
  namespaced: 'repoDetailModule',
  strict: false
})

export class RepoDetailStore extends VxModule {
  private historyByRepoId: {
    [repoId: string]: [Commit, CommitComparison][]
  } = {}

  @action
  async fetchHistoryForRepo(payload: {
    repoId: string
    amount: number
    skip: number
  }): Promise<[Commit, CommitComparison][]> {
    let response = await axios.get('/commit-history', {
      params: {
        repo_id: payload.repoId,
        amount: payload.amount,
        skip: payload.skip
      }
    })

    let commitPairArray: any[] = response.data.commits

    let resultArray: [Commit, CommitComparison][] = commitPairArray.map(
      ({ commit: jsonCommit, comparison: jsonComparison }) => {
        let commit: Commit = commitFromJson(jsonCommit)
        let firstRun: Run = runFromJson(jsonComparison.first)
        let secondRun: Run = runFromJson(jsonComparison.second)
        let differences: Difference[] = jsonComparison.differences.map(
          (it: any) => differenceFromJson(it)
        )
        const commitComparison = new CommitComparison(
          firstRun,
          secondRun,
          differences
        )
        return [commit, commitComparison]
      }
    )

    this.setHistoryForRepo({ repoId: payload.repoId, history: resultArray })

    return Promise.resolve(resultArray)
  }

  @mutation
  setHistoryForRepo(payload: {
    repoId: string
    history: [Commit, CommitComparison][]
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
  get historyForRepoId(): (repoId: string) => [Commit, CommitComparison][] {
    return (repoId: string) =>
      this.historyByRepoId[repoId] ? this.historyByRepoId[repoId] : []
  }
}
