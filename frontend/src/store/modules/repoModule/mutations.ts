import { MutationTree } from 'vuex'
import { RepoState, Repo } from '../../types'
import Vue from 'vue'

export const mutations: MutationTree<RepoState> = {
  SET_REPO: (state: RepoState, payload: Repo) => {
    Vue.set(state.repos, payload.id, { ...payload })
  },

  SET_REPOS: (state: RepoState, payload: Array<Repo>) => {
    payload.forEach(repo => {
      Vue.set(state.repos, repo.id, { ...repo })
    })
  },

  REMOVE_REPO: (state: RepoState, payload: string) => {
    Vue.delete(state.repos, payload)
  }
}
