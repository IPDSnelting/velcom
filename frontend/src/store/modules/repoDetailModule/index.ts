import { Module } from 'vuex'
import { RootState, RepoDetailState, CommitComparison } from '../../types'
import { mutations } from './mutations'
import { actions } from './actions'
import { getters } from './getters'

export const state: RepoDetailState = {
  comparisonsByRepoID: new Map<string, Array<CommitComparison>>
}

const namespaced: boolean = true

export const repoDetailModule: Module<RepoDetailState, RootState> = {
  namespaced,
  state,
  mutations,
  actions,
  getters
}
