import { Module } from 'vuex'
import { RootState, CommitComparisonState, CommitComparison } from '../../types'
import { mutations } from './mutations'
import { actions } from './actions'
import { getters } from './getters'

export const state: CommitComparisonState = {
  comparisons: new Map<string, Array<CommitComparison>>()
}

const namespaced: boolean = true

export const commitComparisonModule: Module<
  CommitComparisonState,
  RootState
> = {
  namespaced,
  state,
  mutations,
  actions,
  getters
}
