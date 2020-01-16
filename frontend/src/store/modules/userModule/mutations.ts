import { MutationTree } from 'vuex'
import { UserState } from '../../types'

export const mutations: MutationTree<UserState> = {
  SET_ROLE: (state: UserState, payload) => {
    state.role = payload
  },

  SET_REPO_ID: (state: UserState, repoID: string) => {
    state.repoID = repoID
  },

  SET_TOKEN: (state: UserState, token: string) => {
    state.token = token
  }
}
