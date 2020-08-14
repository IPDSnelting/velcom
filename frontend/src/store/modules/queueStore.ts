import { createModule, mutation, action } from 'vuex-class-component'
import { Worker, Commit, Task, RepoId, CommitHash, TaskId } from '@/store/types'
import axios from 'axios'
import { taskFromJson, workerFromJson } from '@/util/QueueJsonHelper'

const VxModule = createModule({
  namespaced: 'queueModule',
  strict: false
})

export class QueueStore extends VxModule {
  private _openTasks: Task[] = []
  private _workers: Worker[] = []

  /**
   * Fetches the whole queue.
   *
   * @param {{
   *     hideFromSnackbar?: boolean
   *   }} [payload] the payload. If hideFromSnackbar is true,
   * it will not be shown in the snackbar.
   * @returns {Promise<Commit[]>} a promise completing the commits
   * @memberof QueueModuleStore
   */
  @action
  async fetchQueue(): Promise<Task[]> {
    const response = await axios.get('/queue', {
      snackbarTag: 'queue'
    })

    let jsonTasks: any[] = response.data.tasks
    let tasks: Task[] = jsonTasks.map(taskFromJson)

    let workers: Worker[] = response.data.runners.map(workerFromJson)

    this.setOpenTasks(tasks)
    this.setWorkers(workers)

    return tasks
  }

  @action
  async startManualTask(payload: {
    hash: CommitHash
    repoId: RepoId
  }): Promise<void> {
    await axios.post(`/queue/${payload.repoId}/${payload.hash}`, {
      showSuccessSnackbar: true
    })
    // We do not insert the task locally as we don't know where!
    // Fetching the queue is not needed, as this option is only called from
    // other pages
  }

  /**
   * Sends a prioritize request to the server.
   *
   * @param {string} id the id of the task to prioritize
   * @returns {Promise<void>} a promise completing with an optional error
   * @memberof QueueModuleStore
   */
  @action
  async dispatchPrioritizeOpenTask(id: TaskId): Promise<void> {
    await axios.patch(`/queue/${id}`, { showSuccessSnackbar: true })
    this.prioritizeOpenTask(id)
  }

  /**
   * Queues all commits upwards of (and including) the passed commit.
   *
   * @param {Commit} commit the base commit to prioritize
   * @returns {Promise<void>} a promise completing with an optional error
   * @memberof QueueModuleStore
   */
  @action
  async dispatchQueueUpwardsOf(task: Commit): Promise<void> {
    // FIXME: Adjust API?
    throw new Error('Not implementable!')
  }

  /**
   * Sends a delete request for an open task to the server.
   *
   * @param {{
   *     id: string,
   *     suppressRefetch?: boolean,
   *     suppressSnackbar?: boolean,
   *   }} payload the task to delete. If `suppressRefetch` is false or not present, it will also call "fetchQueue"
   * @returns {Promise<void>} a promise completing with an optional error
   * @memberof QueueModuleStore
   */
  @action
  async dispatchDeleteOpenTask(payload: {
    id: string
    suppressRefetch?: boolean
  }): Promise<void> {
    await axios.delete(`/queue/${payload.id}`)
    if (!payload.suppressRefetch) {
      this.fetchQueue()
    }
  }

  /**
   * Sets all open tasks.
   *
   * @param {Task[]} payload the tasks
   * @memberof QueueModuleStore
   */
  @mutation
  setOpenTasks(payload: Task[]) {
    this._openTasks = payload.slice()
  }

  /**
   * Moves a given manual task to the top of the queue. Locally!
   *
   * @param {string} id the id of the task
   * @memberof QueueModuleStore
   */
  @mutation
  prioritizeOpenTask(id: TaskId) {
    let oldIndex = this._openTasks.findIndex(task => task.id === id)
    if (oldIndex < 0) {
      return
    }
    let task = this._openTasks[oldIndex]
    this._openTasks.splice(oldIndex, 1)
    this._openTasks.unshift(task)
  }

  /**
   * Deletes an open task *locally*!
   *
   * @param {string} id the id of the task
   * @memberof QueueModuleStore
   */
  @mutation
  deleteOpenTask(id: TaskId) {
    let target = this._openTasks.findIndex(task => task.id === id)
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
   * @type {Task[]}
   * @memberof QueueModuleStore
   */
  get openTasks(): Task[] {
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
}
