import { GetterTree } from 'vuex'
import { CommitComparisonState, RootState } from '../../types'

export const getters: GetterTree<CommitComparisonState, RootState> = {
  commitComparison: state => (
    repoID: string,
    first: string,
    second: string
  ) => {
    let comparisons = state.comparisons.get(repoID)
    if (!comparisons) {
      return null
    } else {
      var target = comparisons.findIndex(comparison => {
        return (
          comparison.first.commit.hash === first &&
          comparison.second.commit.hash === second
        )
      })
      if (target !== -1) {
        return comparisons[target]
      }
    }
  }
}
