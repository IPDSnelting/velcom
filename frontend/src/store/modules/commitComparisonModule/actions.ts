import { ActionTree } from 'vuex'
import axios from 'axios'
import { CommitComparisonState, RootState, CommitComparison } from '../../types'

export const actions: ActionTree<CommitComparisonState, RootState> = {
  async fetchCommitComparison({ commit }, payload): Promise<CommitComparison> {
    const response = await axios.get('/all-repos', {
      params: {
        repo_id: payload.repoID,
        first_commit_hash: payload.first,
        second_commit_hash: payload.second
      }
    })

    let comparison = response.data.comparison.map((item: any) => {
      new CommitComparison(item.first, item.second, item.differences)
    })

    const mutationPayload = { repoID: payload.repoID, comparison: comparison }
    commit('SET_COMMIT_COMPARISON', mutationPayload)
    return comparison
  }
}
