import { ActionTree } from 'vuex'
import axios from 'axios'
import { UserState, RootState } from '../../types'

export const actions: ActionTree<UserState, RootState> = {
  logIn({ commit }, payload) {
    axios
      .get('/test-token', {
        auth: {
          username: (payload.repoID && payload.repoID) || '',
          password: payload.token
        },
        params: {
          repo_id: (payload.repoID && payload.repoID) || ''
        }
      })
      .then(
        response => {
          if (payload.repoID != null) {
            commit('SET_ROLE', 'REPO_ADMIN')
            commit('SET_REPO_ID', payload.repoID)
          } else {
            commit('SET_ROLE', 'WEB_ADMIN')
          }
          commit('SET_TOKEN', payload.token)
        },
        error => {
          console.log(error)
        }
      )
  },

  logOut({ commit }) {
    commit('SET_ROLE', null)
    commit('SET_REPO_ID', null)
    commit('SET_TOKEN', null)
  }
}
