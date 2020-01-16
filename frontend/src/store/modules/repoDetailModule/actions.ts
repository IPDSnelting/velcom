import { ActionTree } from 'vuex'
import axios from 'axios'
import {
  RepoDetailState,
  RootState,
  CommitComparison,
  Commit
} from '../../types'

export const actions: ActionTree<RepoDetailState, RootState> = {
  async fetchRepoDatapoints(
    { commit },
    id: string
  ): Promise<Array<CommitComparison>> {
    const response = await axios.get('/all-repos')

    let comparisons: Array<CommitComparison> = []
    let jsonTasks: Array<any> = response.data.tasks

    jsonTasks.forEach((item: any) => {
      comparisons.push(
        new CommitComparison(item.first, item.second, item.differences)
      )
    })

    commit('SET_REPO_DATAPOINTS', { repoID: id, comparisons: comparisons })
    return comparisons
  }
}
