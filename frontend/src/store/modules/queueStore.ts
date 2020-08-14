import { createModule, mutation, action } from 'vuex-class-component'
import { Worker, Commit, Task } from '@/store/types'
import axios from 'axios'
import { taskFromJson } from '@/util/QueueJsonHelper'

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

    this.setOpenTasks(tasks)

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
      .post(
        '/queue',
        {
          repo_id: payload.repoID,
          commit_hash: payload.hash
        },
        { showSuccessSnackbar: true }
      )
      .then(() => {
        this.prioritizeOpenTask(payload)
      })
  }

  /**
   * Queues all commits upwards of (and including) the passed commit.
   *
   * @param {Commit} commit the base commit to prioritize
   * @returns {Promise<void>} a promise completing with an optional error
   * @memberof QueueModuleStore
   */
  @action
  dispatchQueueUpwardsOf(commit: Commit): Promise<void> {
    return axios
      .post(
        '/queue',
        {
          repo_id: commit.repoID,
          commit_hash: commit.hash,
          include_all_commits_after: true
        },
        { showSuccessSnackbar: true }
      )
      .then(() => {
        this.prioritizeOpenTask(commit)
      })
  }

  /**
   * Sends a delete request for an open task to the server.
   *
   * @param {{
   *     commit: Commit,
   *     suppressRefetch?: boolean,
   *     suppressSnackbar?: boolean,
   *   }} payload the commit to delete. If `suppressRefetch` is false or not present, it will also call "fetchQueue"
   * @returns {Promise<void>} a promise completing with an optional error
   * @memberof QueueModuleStore
   */
  @action
  dispatchDeleteOpenTask(payload: {
    commit: Commit
    suppressRefetch?: boolean
  }): Promise<void> {
    return axios
      .delete('/queue', {
        params: {
          repo_id: payload.commit.repoID,
          commit_hash: payload.commit.hash
        }
      })
      .then(() => {
        if (!payload.suppressRefetch) {
          this.fetchQueue()
        }
      })
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
   * @param {Commit} payload the payload of the commit
   * @memberof QueueModuleStore
   */
  @mutation
  prioritizeOpenTask(payload: Commit) {
    let oldIndex = this._openTasks.findIndex(task => {
      return task.repoID === payload.repoID && task.hash === payload.hash
    })
    if (oldIndex !== -1) {
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
