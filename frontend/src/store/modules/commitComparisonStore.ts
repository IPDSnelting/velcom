import { createModule, mutation, action } from 'vuex-class-component'
import { CommitComparison, CommitInfo } from '@/store/types'
import axios from 'axios'
import Vue from 'vue'
import {
  comparisonFromJson,
  commitDetailFromJson as commitInfoFromJson
} from '@/util/CommitComparisonJsonHelper'

const VxModule = createModule({
  namespaced: 'commitComparisonModule',
  strict: false
})

export class CommitComparisonStore extends VxModule {
  private commitInfos: { [key: string]: CommitInfo[] } = {}

  /**
   * Fetches the commit comparison for two commits in a repo
   * from the server.
   *
   * @param {{
   *     repoId: string
   *     first: string
   *     second: string | undefined
   *   }} payload the playload to fetch
   * @returns {Promise<CommitInfo>} a promise resolving to
   * the comparison
   * @memberof CommitComparisonModuleStore
   */
  @action
  async fetchCommitInfo(payload: {
    repoId: string
    first: string | undefined
    second: string | undefined
  }): Promise<CommitInfo> {
    const response = await axios.get('/commit-compare', {
      snackbarTag: 'commit-comparison',
      params: {
        repo_id: payload.repoId,
        first_commit_hash: payload.first,
        second_commit_hash: payload.second
      }
    })

    let info = commitInfoFromJson(response.data)

    const mutationPayload = { repoId: payload.repoId, info: info }
    this.setCommitInfo(mutationPayload)
    return info
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
  setCommitInfo(payload: { info: CommitInfo; repoId: string }) {
    let comparisons = this.commitInfos[payload.repoId]
    if (!comparisons) {
      Vue.set(this.commitInfos, payload.repoId, [payload.info])
    } else {
      let current = comparisons.findIndex(detail => {
        return (
          detail.comparison.first === payload.info.comparison.first &&
          detail.comparison.second === payload.info.comparison.second
        )
      })

      if (current !== -1) {
        comparisons.splice(current, 1)
      }

      comparisons.push(payload.info)
    }
  }

  /**
   * Returns a commit comparison given a repo and two commits.
   * Returns null if not found.
   *
   * @readonly
   * @memberof CommitComparisonModuleStore
   */
  get commitInfo(): (
    repoId: string,
    first: string | null,
    second: string
  ) => CommitInfo | null {
    return (repoId: string, first: string | null, second: string) => {
      let commitInfo = this.commitInfos[repoId]
      if (!commitInfo) {
        return null
      }
      let info = commitInfo.find(info => {
        let comparison = info.comparison
        if (!comparison.secondCommit) {
          return false
        }
        if (!first) {
          return info.comparison.secondCommit.hash === second
        }
        if (!comparison.firstCommit) {
          return false
        }
        return (
          comparison.firstCommit.hash === first &&
          comparison.secondCommit.hash === second
        )
      })
      return info || null
    }
  }
}
