import { createModule, mutation, action } from 'vuex-class-component'
import { Repo, CommitComparison } from '@/store/types'
import Vue from 'vue'
import axios from 'axios'

const VxModule = createModule({
  namespaced: 'repoModule',
  strict: false
})

export class RepoDetailStore extends VxModule {
  private comparisonsByRepoId: { [key: string]: CommitComparison[] } = {}

  /**
   * Fetches all data points for a given repo.
   *
   * @param {string} id the id of the repo
   * @returns {Promise<CommitComparison[]>} a promise containing the fetched
   * datapoints
   * @memberof RepoDetailStore
   */
  @action
  async fetchRepoDatapoints(id: string): Promise<CommitComparison[]> {
    const response = await axios.get('/all-repos')

    let comparisons: CommitComparison[] = []
    let jsonTasks: any[] = response.data.tasks

    jsonTasks.forEach((item: any) => {
      comparisons.push(
        new CommitComparison(item.first, item.second, item.differences)
      )
    })

    this.setRepoComparisons({ repoId: id, comparisons: comparisons })
    return comparisons
  }

  /**
   * Sets the repo comparisons for a single repo.
   *
   * @param {{
   *     repoId: string
   *     comparisons: CommitComparison[]
   *   }} payload the payload to set it with
   * @memberof RepoDetailStore
   */
  @mutation
  setRepoComparisons(payload: {
    repoId: string
    comparisons: CommitComparison[]
  }) {
    Vue.set(
      this.comparisonsByRepoId,
      payload.repoId,
      payload.comparisons.slice() // copy it
    )
  }

  /**
   * Returns the `CommitComparison`s for a single repository.
   *
   * Returns an empty array if there are none or the repo id is unknown.
   * @readonly
   * @memberof RepoDetailStore
   */
  get repoDatapoints(): (repoId: string) => CommitComparison[] {
    return (repoId: string) => this.comparisonsByRepoId[repoId] || []
  }
}
