import { ActionTree } from 'vuex'
import axios from 'axios'
import { RepoComparisonState, RootState, Run } from '../../types'

export const actions: ActionTree<RepoComparisonState, RootState> = {
  async setDatapoints(
    { commit, rootGetters },
    payload
  ): Promise<Map<string, Array<Run>>> {
    var repos = new Array<string>()
    Array.from(payload.repos.keys()).forEach(repoID => {
      repos.push(
        JSON.stringify({ repo_id: repoID, branches: payload.repos.get(repoID) })
      )
    })
    const response = await axios.post('/repo-comparison-graph', {
      params: {
        repos: repos,
        start_time: payload.startTime,
        end_time: payload.endTime
      }
    })

    let datapoints = new Map<string, Array<Run>>()
    let jsonData: Array<any> = response.data.repos

    jsonData.forEach((item: any) => {
      var runs = new Array<Run>()
      item.runs.forEach((run: any) => {
        runs.push(
          new Run(
            run.commit,
            run.start_time,
            run.stop_time,
            run.measurements,
            run.error_mesage
          )
        )
      })
      datapoints.set(item.repo, item.runs)
    })

    commit('SET_DATAPOINTS', datapoints)
    return datapoints
  }
}
