import { GetterTree } from 'vuex'
import { RepoState, RootState } from '../../types'

export const getters: GetterTree<RepoState, RootState> = {
  allRepos: state => {
    return Array.from(state.repos.values())
  },

  repoByID: state => (id: string) => {
    return state.repos.get(id)
  }
}
