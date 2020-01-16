import { GetterTree } from 'vuex'
import { QueueState, RootState } from '../../types'

export const getters: GetterTree<QueueState, RootState> = {
  openTasks: state => {
    return state.openTasks
  },

  workers: state => {
    return state.workers
  },

  openTasksByRepoID: state => (repoID: string) => {
    return state.openTasks.filter(task => task.repoID === repoID)
  },

  openTask: state => (repoID: string, hash: string) => {
    return state.openTasks.filter(
      task => task.repoID === repoID && task.hash === hash
    )
  }
}
