import { GetterTree } from 'vuex'
import { NewsState, RootState } from '../../types'

export const getters: GetterTree<NewsState, RootState> = {
  recentRuns: state => {
    return state.recentRuns
  },

  recentSignificantRuns: state => {
    return state.recentSignificantRuns
  }
}
