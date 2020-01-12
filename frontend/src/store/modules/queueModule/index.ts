import { Module } from 'vuex'
import { RootState, QueueState } from '../../types'
import { mutations } from './mutations'
import { actions } from './actions'
import { getters } from './getters'

export const state: QueueState = {
  openTasks: [],
  workers: []
}

const namespaced: boolean = true

export const queueModule: Module<QueueState, RootState> = {
  namespaced,
  state,
  mutations,
  actions,
  getters
}
