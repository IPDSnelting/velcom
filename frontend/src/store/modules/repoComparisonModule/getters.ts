import { GetterTree } from 'vuex'
import { RepoComparisonState, RootState } from '../../types'

export const getters: GetterTree<RepoComparisonState, RootState> = {
  datapoints: state => {
    return state.runsByRepoID
  }
}
