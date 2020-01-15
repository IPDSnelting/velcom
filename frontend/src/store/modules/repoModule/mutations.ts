import { MutationTree } from 'vuex'
import { RepoState, Repo } from '../../types'

export const mutations: MutationTree<RepoState> = {
  SET_REPO: (state: RepoState, payload: Repo) => {
    state.repos.set(payload.id, payload)
  },

  SET_REPOS: (state: RepoState, payload: Array<Repo>) => {
    payload.forEach(repo => {
      state.repos.set(repo.id, repo)
    })
  },

  REMOVE_REPO: (state: RepoState, payload: string) => {
    state.repos.delete(payload)
  }
}
