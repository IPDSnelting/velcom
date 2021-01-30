import { action, createModule } from 'vuex-class-component'
import axios from 'axios'
import { RepoId, RunId, ShortRunDescription } from '@/store/types'
import { shortRunDescriptionFromJson } from '@/util/RunSearchJsonHelper'

const VxModule = createModule({
  namespaced: 'runSearchModule',
  strict: false
})

export type RunQuery = {
  latestRunsOnly?: boolean
  limit?: number
  repoId?: RepoId
  commitHash?: string
  description?: string
  orderBy?: 'run_start_time' | 'committer_date'
}

export class RunSearchStore extends VxModule {
  @action
  public async searchRun(query: RunQuery): Promise<ShortRunDescription[]> {
    const response = await axios.get('/run/search', {
      hideFromSnackbar: true,
      params: {
        latest_runs_only: query.latestRunsOnly,
        limit: query.limit,
        repo_id: query.repoId,
        commit_hash: query.commitHash,
        description: query.description,
        order_by_run_start_time: query.orderBy === 'run_start_time',
        order_by_committer_time: query.orderBy === 'committer_date'
      }
    })

    return response.data.runs.map(shortRunDescriptionFromJson)
  }

  @action
  public async fetchShortRun(
    runId: RunId
  ): Promise<ShortRunDescription | null> {
    try {
      const response = await axios.get(`/run/${runId}/short`)
      return shortRunDescriptionFromJson(response.data.run)
    } catch (e) {
      if (e.response && e.response.status === 404) {
        return null
      }
      throw e
    }
  }
}
