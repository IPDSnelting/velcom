import { MutationTree } from 'vuex'
import { RepoComparisonState, Run } from '../../types'

export const mutations: MutationTree<RepoComparisonState> = {
  SET_DATAPOINTS: (state: RepoComparisonState, payload: Map<string, Array<Run>>) => {
    state.runsByRepoID = payload
  }
}
