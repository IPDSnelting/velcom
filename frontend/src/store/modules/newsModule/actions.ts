import { ActionTree } from 'vuex'
import axios from 'axios'
import { NewsState, RootState, CommitComparison } from '../../types'

export const actions: ActionTree<NewsState, RootState> = {
  async fetchRecentRuns(
    { commit },
    amount: number
  ): Promise<Array<CommitComparison>> {
    const response = await axios.get('/recently-benchmarked-commits', {
      params: {
        amount: amount,
        significant_only: false
      }
    })

    let runs: Array<CommitComparison> = []
    let jsonData: Array<any> = response.data.commits

    jsonData.forEach((item: any) => {
      runs.push(new CommitComparison(item.first, item.second, item.differences))
    })

    commit('SET_RECENT_RUNS', runs)
    return runs
  },

  async fetchRecentSignificantRuns(
    { commit },
    amount: number
  ): Promise<Array<CommitComparison>> {
    const response = await axios.get('/recently-benchmarked-commits', {
      params: {
        amount: amount,
        significant_only: true
      }
    })

    let runs: Array<CommitComparison> = []
    let jsonData: Array<any> = response.data.commits

    jsonData.forEach((item: any) => {
      runs.push(new CommitComparison(item.first, item.second, item.differences))
    })

    commit('SET_RECENT_SIGNIFICANT_RUNS', runs)
    return runs
  }
}
