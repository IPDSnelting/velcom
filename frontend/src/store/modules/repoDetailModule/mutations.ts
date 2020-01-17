import { MutationTree } from 'vuex'
import { RepoDetailState } from '../../types'

export const mutations: MutationTree<RepoDetailState> = {
  SET_REPO_DATAPOINTS: (state: RepoDetailState, payload) => {
    state.comparisonsByRepoID.set(payload.repoID, payload.comparisons)
  }
}
