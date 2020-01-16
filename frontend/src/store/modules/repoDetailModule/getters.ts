import { GetterTree } from 'vuex'
import { RepoDetailState, RootState } from '../../types'

export const getters: GetterTree<RepoDetailState, RootState> = {
  repoDatapoints: state => (repoID: string) {
    return state.comparisonsByRepoID.get(repoID)
  }
}
