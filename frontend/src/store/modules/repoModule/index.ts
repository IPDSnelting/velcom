import { Module } from 'vuex'
import { RootState, RepoState, Repo } from '../../types'
import { mutations } from './mutations'
import { actions } from './actions'
import { getters } from './getters'

export const state: RepoState = {
  repos: {},

  getRepo(id: string) {
    return this.repos[id]
  }
}

const namespaced: boolean = true

export const repoModule: Module<RepoState, RootState> = {
  namespaced,
  state,
  mutations,
  actions,
  getters
}
