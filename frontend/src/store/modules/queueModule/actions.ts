import { ActionTree } from 'vuex'
import axios from 'axios'
import { QueueState, RootState, Repo, Commit, Worker } from '../../types'

export const actions: ActionTree<QueueState, RootState> = {
  async fetchQueue({ commit }): Promise<Array<Commit>> {
    const response = await axios.get('/all-repos')

    let tasks: Array<Commit> = []
    let jsonTasks: Array<any> = response.data.tasks

    jsonTasks.forEach((item: any) => {
      tasks.push(
        new Commit(
          item.repo_id,
          item.hash,
          item.author,
          item.author_date,
          item.committer,
          item.committer_date,
          item.message,
          item.parents
        )
      )
    })

    let workers: Array<Worker> = []
    let jsonWorkers: Array<any> = response.data.tasks

    jsonWorkers.forEach((item: any) => {
      var currentTask = new Commit(
        item.working_on.repo_id,
        item.working_on.hash,
        item.working_on.author,
        item.working_on.authorDate,
        item.working_on.committer,
        item.working_on.committer_date,
        item.working_on.message,
        item.working_on.parents
      )

      workers.push(new Worker(item.name, item.hash, currentTask))
    })

    commit('SET_OPEN_TASKS', tasks)
    commit('SET_WORKERS', workers)

    return tasks
  },

  prioritizeOpenTask({ commit, rootGetters }, payload: Commit) {
    axios
      .post('/queue', {
        auth: {
          username: rootGetters['userModule/repoID'],
          password: rootGetters['userModule/token']
        },
        params: {
          repo_id: payload.repoID,
          commit_hash: payload.hash
        }
      })
      .then(
        response => {
          commit('PRIORIZE_OPEN_TASK', payload)
        },
        error => {
          console.log('error: could not prioritize task ' + payload.hash)
          console.log(error)
        }
      )
  },

  deleteOpenTask({ commit, rootGetters }, payload) {
    axios
      .delete('/queue', {
        auth: {
          username: rootGetters['userModule/repoID'],
          password: rootGetters['userModule/token']
        },
        params: {
          repo_id: payload.repoID,
          commit_hash: payload.hash
        }
      })
      .then(
        response => {
          commit('DELETE_OPEN_TASK', payload)
        },
        error => {
          console.log('error: could not remove task ' + payload.hash)
          console.log(error)
        }
      )
  }
}
