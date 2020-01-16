import { GetterTree } from 'vuex'
import { UserState, RootState } from '../../types'

export const getters: GetterTree<UserState, RootState> = {
  loggedIn: state => {
    return state.role == null
  },

  authorized: state => (repoID: string) => {
    return state.repoID === repoID
  },

  repoID: state => {
    return state.repoID
  },

  troken: state => {
    return state.token
  }
}
