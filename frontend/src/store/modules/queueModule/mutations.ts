import { MutationTree } from 'vuex'
import { QueueState, Worker, Commit } from '../../types'

export const mutations: MutationTree<QueueState> = {
  SET_OPEN_TASKS: (state: QueueState, payload: Array<Commit>) => {
    state.openTasks = payload
  },

  PRIORITIZE_OPEN_TASK: (state: QueueState, payload: Commit) => {
    var oldIndex = state.openTasks.findIndex(task => {
      return task.repoID === payload.repoID && task.hash === payload.hash
    })
    if (oldIndex !== -1) {
      state.openTasks.splice(0, 0, state.openTasks.splice(oldIndex)[0])
    } else {
      state.openTasks.splice(0,0, payload)
    }
  },

  DELETE_OPEN_TASK: (state: QueueState, payload: Commit) => {
    var target = state.openTasks.findIndex(task => {
      return task.repoID === payload.repoID && task.hash === payload.hash
    })
    if (target !== -1) {
      state.openTasks.splice(target, 1)
    }
  },

  SET_WORKERS: (state: QueueState, payload: Array<Worker>) => {
    state.workers = payload
  }
}
