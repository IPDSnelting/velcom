import { createModule, mutation, action } from 'vuex-class-component'
import { CommitComparison } from '@/store/types'
import axios from 'axios'
import Vue from 'vue'

const VxModule = createModule({
  namespaced: 'commitComparisonModule',
  strict: false
})

export class CommitComparisonModuleStore extends VxModule {
  private comparisons: { [key: string]: CommitComparison[] } = {}

  /**
   * Fetches the commit comparison for two commits in a repo
   * from the server.
   *
   * @param {{
   *     repoId: string
   *     first: string
   *     second: string
   *   }} payload the playload to fetch
   * @returns {Promise<CommitComparison>} a promise resolving to
   * the comparison
   * @memberof CommitComparisonModuleStore
   */
  @action
  async fetchCommitComparison(payload: {
    repoId: string
    first: string
    second: string
  }): Promise<CommitComparison> {
    const response = await axios.get('/all-repos', {
      params: {
        repo_id: payload.repoId,
        first_commit_hash: payload.first,
        second_commit_hash: payload.second
      }
    })

    let comparison = response.data.comparison.map((item: any) => {
      new CommitComparison(item.first, item.second, item.differences)
    })

    const mutationPayload = { repoId: payload.repoId, comparison: comparison }
    this.setCommitComparison(mutationPayload)
    return comparison
  }

  /**
   * Sets the commit comparison for a given repo.
   *
   * @param {{
   *     comparison: CommitComparison
   *     repoId: string
   *   }} payload the payload to set
   * @memberof CommitComparisonModuleStore
   */
  @mutation
  setCommitComparison(payload: {
    comparison: CommitComparison
    repoId: string
  }) {
    let comparisons = this.comparisons[payload.repoId]
    if (!comparisons) {
      Vue.set(this.comparisons, payload.repoId, payload.comparison)
    } else {
      let current = comparisons.findIndex(comparison => {
        return (
          comparison.first === payload.comparison.first &&
          comparison.second === payload.comparison.second
        )
      })

      if (current !== -1) {
        comparisons.splice(current, 1)
      }

      comparisons.push(payload.comparison)
    }
  }

  /**
   * Returns a commit comparison given a repo and two commits.
   * Returns null if not found.
   *
   * @readonly
   * @memberof CommitComparisonModuleStore
   */
  get commitComparison(): (
    repoId: string,
    first: string,
    second: string
  ) => CommitComparison | null {
    return (repoId: string, first: string, second: string) => {
      let comparisons = this.comparisons[repoId]
      if (!comparisons) {
        return null
      }
      let comparison = comparisons.find(
        comparison =>
          comparison.first.commit.hash === first &&
          comparison.second.commit.hash === second
      )
      return comparison || null
    }
  }
}
