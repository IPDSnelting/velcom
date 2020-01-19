import { createModule, mutation, action } from 'vuex-class-component'
import { Run } from '@/store/types'
import Vue from 'vue'
import axios from 'axios'

const VxModule = createModule({
  namespaced: 'repoComparisonModule',
  strict: false
})

export class RepoComparisonStore extends VxModule {
  private runsByRepoId: { [key: string]: Run[] } = {}

  /**
   * Fetches all data points.
   *
   * @param {{
   *     repos: string[]
   *     startTime: string
   *     endTime: string
   *   }} payload the payload with the parameters to pass to the server
   * @returns {Promise<{ [key: string]: Run[] }>} a promise containing all runs
   * @memberof RepoComparisonStore
   */
  @action
  async fetchDatapoints(payload: {
    repos: { [key: string]: string[] }
    startTime: number
    endTime: number
    benchmark: string
    metric: string
  }): Promise<{ [key: string]: Run[] }> {
    let repos: any[] = []
    const repoIDs: string[] = Object.keys(payload.repos)
    repoIDs.forEach(repoID => {
      repos.push({ repo_id: repoID, branches: payload.repos[repoID] })
    })
    console.log(repos)
    const response = await axios.post('/repo-comparison-graph', {
      repos: repos,
      start_time: payload.startTime + '',
      end_time: payload.endTime + '',
      benchmark: payload.benchmark,
      metric: payload.metric
    })

    let datapoints: { [key: string]: Run[] } = {}
    let jsonData: any[] = response.data.repos

    jsonData.forEach((item: any) => {
      var runs: Run[] = []
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
      datapoints[item.repo] = item.runs
    })

    this.setDataPoints(datapoints)
    return this.allRuns
  }

  /**
   * Sets all data points.
   *
   * @param {{ [key: string]: Run[] }} payload the payload
   * to set it with
   * @memberof RepoComparisonStore
   */
  @mutation
  setDataPoints(payload: { [key: string]: Run[] }) {
    Array.from(Object.keys(payload)).forEach(key => {
      Vue.set(this.runsByRepoId, key, payload[key])
    })
  }

  /**
   * Returns all known runs.
   *
   * @readonly
   * @type {{ [key: string]: Run[] }}
   * @memberof RepoComparisonStore
   */
  get allRuns(): { [key: string]: Run[] } {
    return this.runsByRepoId
  }
}
