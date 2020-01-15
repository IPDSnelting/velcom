import { Module } from 'vuex'
import { RootState, RepoComparisonState, Run } from '../../types'
import { mutations } from './mutations'
import { actions } from './actions'
import { getters } from './getters'

export const state: RepoComparisonState = {
  runsByRepoID: new Map<string, Array<Run>>()
}

const namespaced: boolean = true

export const repoComparisonModule: Module<RepoComparisonState, RootState> = {
  namespaced,
  state,
  mutations,
  actions,
  getters
}
