import { createModule, mutation, action } from 'vuex-class-component'
import { CommitComparison } from '@/store/types'
import axios from 'axios'
import Vue from 'vue'
import { comparisonFromJson } from '@/util/CommitComparisonJsonHelper'

const VxModule = createModule({
  namespaced: 'commitComparisonModule',
  strict: false
})

export class CommitComparisonStore extends VxModule {
  private comparisons: { [key: string]: CommitComparison[] } = {}

  /**
   * Fetches the commit comparison for two commits in a repo
   * from the server.
   *
   * @param {{
   *     repoId: string
   *     first: string
   *     second: string | undefined
   *   }} payload the playload to fetch
   * @returns {Promise<CommitComparison>} a promise resolving to
   * the comparison
   * @memberof CommitComparisonModuleStore
   */
  @action
  async fetchCommitComparison(payload: {
    repoId: string
    first: string | undefined
    second: string | undefined
  }): Promise<CommitComparison> {
    const response = await axios.get('/commit-compare', {
      snackbarTag: 'commit-comparison',
      params: {
        repo_id: payload.repoId,
        first_commit_hash: payload.first,
        second_commit_hash: payload.second
      }
    })

    let comparison = comparisonFromJson(response.data.comparison)

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
      Vue.set(this.comparisons, payload.repoId, [payload.comparison])
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
    first: string | null,
    second: string
  ) => CommitComparison | null {
    return (repoId: string, first: string | null, second: string) => {
      let comparisons = this.comparisons[repoId]
      if (!comparisons) {
        return null
      }
      let comparison = comparisons.find(comparison => {
        if (!comparison.secondCommit) {
          console.log('No second commit ' + comparison)

          return false
        }
        if (!first) {
          return comparison.secondCommit.hash === second
        }
        if (!comparison.firstCommit) {
          return false
        }
        return (
          comparison.firstCommit.hash === first &&
          comparison.secondCommit.hash === second
        )
      })
      return comparison || null
    }
  }
}
