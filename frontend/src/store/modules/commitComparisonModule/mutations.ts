import { MutationTree } from 'vuex'
import { CommitComparisonState } from '../../types'

export const mutations: MutationTree<CommitComparisonState> = {
  SET_COMMIT_COMPARISON: (state: CommitComparisonState, payload) => {
    let comparisons = state.comparisons.get(payload.repoID)
    if (!comparisons) {
      state.comparisons.set(payload.repoID, payload.comparison)
    } else {
      var current = comparisons.findIndex(comparison => {
        return (
          comparison.first === payload.comparison.first &&
          comparison.second === payload.comparison.second
        )
      })

      if (current !== -1) {
        comparisons.splice(current, 1)
      }

      comparisons.push(payload.comparison)
    }
  }
}
