import { MutationTree } from 'vuex'
import { NewsState, CommitComparison } from '../../types'

export const mutations: MutationTree<NewsState> = {
  SET_RECENT_RUNS: (state: NewsState, payload: Array<CommitComparison>) => {
    state.recentRuns = payload
  },

  SET_RECENT_SIGNIFICANT_RUNS: (state: NewsState, payload: Array<CommitComparison>) => {
    state.recentSignificantRuns = payload
  }
}
