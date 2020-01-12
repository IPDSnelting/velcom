import { Module } from 'vuex'
import { RootState, UserState } from '../../types'
import { mutations } from './mutations'
import { actions } from './actions'
import { getters } from './getters'

export const state: UserState = {
  role: null,
  repoID: null,
  token: null
}

const namespaced: boolean = true

export const userModule: Module<UserState, RootState> = {
  namespaced,
  state,
  mutations,
  actions,
  getters
}
