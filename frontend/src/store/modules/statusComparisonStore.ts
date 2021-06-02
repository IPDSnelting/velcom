import { action, createModule } from 'vuex-class-component'
import {
  Dimension,
  dimensionIdEquals,
  RepoId,
  RunResultSuccess,
  StatusComparisonPoint
} from '@/store/types'
import axios from 'axios'
import { statusComparisonPointFromJson } from '@/util/json/StatusComparisonJsonHelper'
import { vxm } from '@/store'

const VxModule = createModule({
  namespaced: 'statusComparisonModule',
  strict: false
})

function formatRepos(repos: Map<RepoId, string[]>): string {
  return Array.from(repos.entries())
    .filter(([, branches]) => branches.length > 0)
    .map(([repoId, branches]) => {
      return repoId + ':' + branches.join(':')
    })
    .join('::')
}

export class StatusComparisonStore extends VxModule {
  graph: StatusComparisonPoint[] = []

  baselineRepoId: string | null = null

  selectedDimensions: Dimension[] = []

  @action
  async fetch(): Promise<StatusComparisonPoint[]> {
    const repos = new Map<RepoId, string[]>()

    vxm.repoModule.allRepos.forEach(repo => {
      repos.set(repo.id, repo.trackedBranches)
    })

    const response = await axios.get('/graph/status-comparison', {
      params: {
        repos: formatRepos(repos)
      }
    })

    this.graph = response.data.runs.map(statusComparisonPointFromJson)

    return this.graph
  }
}
