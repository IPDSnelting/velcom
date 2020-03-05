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
    return commitInfoFromJson(response.data)
  }
}
