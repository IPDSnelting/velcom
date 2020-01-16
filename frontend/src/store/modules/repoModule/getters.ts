import { GetterTree } from 'vuex'
import { RepoState, RootState } from '../../types'

export const getters: GetterTree<RepoState, RootState> = {
  allRepos: state => {
    return Array.from(Object.values(state.repos))
  },

  repoByID: state => (id: string) => {
    return state.repos[id]
  }
}
