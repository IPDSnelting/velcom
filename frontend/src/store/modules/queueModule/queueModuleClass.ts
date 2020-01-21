import { createModule, mutation, action } from 'vuex-class-component'
import { Worker, Commit } from '@/store/types'
import axios from 'axios'

const VxModule = createModule({
  namespaced: 'queueModule',
  strict: false
})

export class QueueStore extends VxModule {
  private _openTasks: Commit[] = []
  private _workers: Worker[] = []

  /**
   * Fetches the whole queue.
   *
   * @returns {Promise<Commit[]>} a promise completing the commits
   * @memberof QueueModuleStore
   */
  @action
  async fetchQueue(): Promise<Commit[]> {
    const response = await axios.get('/queue')

    let tasks: Commit[] = []
    let jsonTasks: any[] = response.data.tasks

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

    let workers: Worker[] = []
    let jsonWorkers: any[] = response.data.workers

    jsonWorkers.forEach((item: any) => {
      let currentTask = null
      if (item.working_on) {
        currentTask = new Commit(
          item.working_on.repo_id,
          item.working_on.hash,
          item.working_on.author,
          item.working_on.author_date,
          item.working_on.committer,
          item.working_on.committer_date,
          item.working_on.message,
          item.working_on.parents
        )
      }

      workers.push(new Worker(item.name, item.machine_info, currentTask))
    })

    this.setOpenTasks(tasks)
    this.setWorkers(workers)

    return tasks
  }

  /**
   * Sends a prioritize request to the server.
   *
   * @param {Commit} payload the commit to prioritize
   * @returns {Promise<void>} a promise completing with an optional error
   * @memberof QueueModuleStore
   */
  @action
  dispatchPrioritizeOpenTask(payload: Commit): Promise<void> {
    return axios
      .post('/queue', {
        repo_id: payload.repoID,
        commit_hash: payload.hash
      })
      .then(() => {
        this.prioritizeOpenTask(payload)
      })
  }

  /**
   *Sends a delete request for an open task to the server.
   *
   * @param {Commit} payload the commit to delete
   * @returns {Promise<void>} a promise completing with an optional error
   * @memberof QueueModuleStore
   */
  @action
  dispatchDeleteOpenTask(payload: Commit): Promise<void> {
    return axios
      .delete('/queue', {
        params: {
          repo_id: payload.repoID,
          commit_hash: payload.hash
        }
      })
      .then(() => {
        this.deleteOpenTask(payload)
      })
  }

  /**
   * Sets all open tasks.
   *
   * @param {Commit[]} payload the tasks
   * @memberof QueueModuleStore
   */
  @mutation
  setOpenTasks(payload: Commit[]) {
    this._openTasks = payload.slice()
  }

  /**
   * Moves a given manual task to the top of the queue. Locally!
   *
   * @param {Commit} payload the payload of the commit
   * @memberof QueueModuleStore
   */
  @mutation
  prioritizeOpenTask(payload: Commit) {
    let oldIndex = this._openTasks.findIndex(task => {
      return task.repoID === payload.repoID && task.hash === payload.hash
    })
    if (oldIndex !== -1) {
      // TODO: Does this get reactive wrapped?
      this._openTasks.splice(oldIndex, 1)
    }
    this._openTasks.unshift(payload)
  }

  /**
   * Deletes the open task for a given commit. Locally!
   *
   * @param {Commit} payload the commit to delete
   * @memberof QueueModuleStore
   */
  @mutation
  deleteOpenTask(payload: Commit) {
    let target = this._openTasks.findIndex(task => {
      return task.repoID === payload.repoID && task.hash === payload.hash
    })
    if (target !== -1) {
      this._openTasks.splice(target, 1)
    }
  }

  /**
   * Sets all workers.
   *
   * @param {Worker[]} payload the new workers
   * @memberof QueueModuleStore
   */
  @mutation
  setWorkers(payload: Worker[]) {
    this._workers = payload.slice()
  }

  /**
   * Returns all open tasks.
   *
   * @readonly
   * @type {Commit[]}
   * @memberof QueueModuleStore
   */
  get openTasks(): Commit[] {
    return this._openTasks
  }

  /**
   * Returns all workers.
   *
   * @readonly
   * @type {Worker[]}
   * @memberof QueueModuleStore
   */
  get workers(): Worker[] {
    return this._workers
  }

  /**
   * Returns all open tasks for a given repo id.
   *
   * @readonly
   * @memberof QueueModuleStore
   */
  get openTasksByRepoID(): (repoId: string) => Commit[] {
    return (repoId: string) =>
      this.openTasks.filter(task => task.repoID === repoId)
  }

  /**
   * Returns the open task with the given repo id and commit hash.
   *
   * @readonly
   * @memberof QueueModuleStore
   */
  get openTask(): (repoID: string, hash: string) => Commit | null {
    return (repoId: string, hash: string) => {
      let tasks = this.openTasks.filter(
        task => task.repoID === repoId && task.hash === hash
      )
      if (tasks.length === 0) {
        return null
      } else if (tasks.length > 1) {
        throw Error('Found more than one matching task! ' + repoId + ' ' + hash)
      }
      return tasks[0]
    }
  }
}
