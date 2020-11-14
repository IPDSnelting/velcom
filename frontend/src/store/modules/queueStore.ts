import { createModule, mutation, action } from 'vuex-class-component'
import {
  Worker,
  Task,
  RepoId,
  CommitHash,
  TaskId,
  CommitDescription,
  StreamedRunnerOutput
} from '@/store/types'
import axios from 'axios'
import {
  streamedRunnerOutputFromJson,
  taskFromJson,
  workerFromJson
} from '@/util/QueueJsonHelper'

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
   * @returns {Promise<Commit[]>} a promise completing the commits
   * @memberof QueueModuleStore
   */
  @action
  async fetchQueue(): Promise<Task[]> {
    const response = await axios.get('/queue', {
      snackbarTag: 'queue'
    })

    const jsonTasks: any[] = response.data.tasks
    const tasks: Task[] = jsonTasks.map(taskFromJson)

    const workers: Worker[] = response.data.runners.map(workerFromJson)

    this.setOpenTasks(tasks)
    this.setWorkers(workers)

    return tasks
  }

  @action
  async startManualTask(payload: {
    hash: CommitHash
    repoId: RepoId
  }): Promise<void> {
    await axios.post(
      `/queue/commit/${payload.repoId}/${payload.hash}`,
      {},
      {
        showSuccessSnackbar: true
      }
    )
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
    await axios.patch(`/queue/${id}`, {}, { showSuccessSnackbar: true })
    this.prioritizeOpenTask(id)
  }

  /**
   * Queues all commits upwards of (and including) the passed commit.
   *
   * @param {CommitDescription} commit the base commit to prioritize
   * @returns {Promise<void>} a promise completing with an optional error
   * @memberof QueueModuleStore
   */
  @action
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  async dispatchQueueUpwardsOf(commit: CommitDescription): Promise<void> {
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
      await this.fetchQueue()
    }
  }

  /**
   * Cancels all tasks currently in the queue the user has access to.
   *
   * @returns {Promise<void>} a promise completing with an optional error
   */
  @action
  async dispatchDeleteAllOpenTasks(): Promise<void> {
    await axios.delete(`/queue/`)
    await this.fetchQueue()
  }

  /**
   * Fetches the runner output for a given task. Returns null if the task is not
   * currently being executed.
   *
   * @param taskId the id of the task
   */
  @action
  async fetchRunnerOutput(
    taskId: string
  ): Promise<StreamedRunnerOutput | null> {
    let response
    try {
      response = await axios.get(`/queue/task/${taskId}/progress`, {
        hideFromSnackbar: true
      })
    } catch (e) {
      return null
    }

    if (response.status === 404) {
      return null
    }

    return streamedRunnerOutputFromJson(response.data)
  }

  /**
   * Sets all open tasks.
   *
   * @param {Task[]} payload the tasks
   * @memberof QueueModuleStore
   */
  @mutation
  setOpenTasks(payload: Task[]): void {
    this._openTasks = payload.slice()
  }

  /**
   * Moves a given manual task to the top of the queue. Locally!
   *
   * @param {string} id the id of the task
   * @memberof QueueModuleStore
   */
  @mutation
  prioritizeOpenTask(id: TaskId): void {
    const oldIndex = this._openTasks.findIndex(task => task.id === id)
    const task = this._openTasks[oldIndex]
    if (oldIndex < 0) {
      return
    }
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
  deleteOpenTask(id: TaskId): void {
    const target = this._openTasks.findIndex(task => task.id === id)
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
  setWorkers(payload: Worker[]): void {
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
