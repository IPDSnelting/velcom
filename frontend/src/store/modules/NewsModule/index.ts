import { Module } from 'vuex'
import { RootState, NewsState } from '../../types'
import { mutations } from './mutations'
import { actions } from './actions'
import { getters } from './getters'

export const state: NewsState = {
  recentRuns: [],
  recentSignificantRuns: []
}

const namespaced: boolean = true

export const newsModule: Module<NewsState, RootState> = {
  namespaced,
  state,
  mutations,
  actions,
  getters
}
