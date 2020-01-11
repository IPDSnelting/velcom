import { Module } from 'vuex'
import { RootState, RepoState } from '../../types'
import { mutations } from './mutations'
import { actions } from './actions'
import { getters } from './getters'

export const state: RepoState = {
  repos: {}
}

const namespaced: boolean = true

export const repoModule: Module<RepoState, RootState> = {
  namespaced,
  state,
  mutations,
  actions,
  getters
}
