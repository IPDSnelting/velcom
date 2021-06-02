import { action, createModule, mutation } from 'vuex-class-component'
import { Dimension, RepoId, StatusComparisonPoint } from '@/store/types'
import axios from 'axios'
import { statusComparisonPointFromJson } from '@/util/json/StatusComparisonJsonHelper'
import { formatRepos } from '@/util/Texts'
import Vue from 'vue'

const VxModule = createModule({
  namespaced: 'statusComparisonModule',
  strict: false
})

export class StatusComparisonStore extends VxModule {
  private selectedBranches: { [repoId: string]: string[] } = {}

  graph: StatusComparisonPoint[] = []
  baselineRepoId: string | null = null
  selectedDimensions: Dimension[] = []
  selectedDimensionSelector: 'tree' | 'matrix' = 'matrix'

  @action
  async fetch(): Promise<StatusComparisonPoint[]> {
    const repos = this.selectedBranchesMap

    // Nothing selected
    if (repos.size === 0) {
      this.graph = []
      return []
    }

    const response = await axios.get('/graph/status-comparison', {
      params: {
        repos: formatRepos(repos)
      }
    })

    this.graph = response.data.runs.map(statusComparisonPointFromJson)

    return this.graph
  }

  @mutation
  setSelectedBranchesForRepo(payload: {
    repoId: RepoId
    branches: string[]
  }): void {
    Vue.set(this.selectedBranches, payload.repoId, payload.branches)
  }

  @mutation
  toggleRepoBranch(payload: { repoId: RepoId; branch: string }): void {
    let branches: string[] = this.selectedBranches[payload.repoId] || []

    if (branches.includes(payload.branch)) {
      branches = branches.filter(it => it !== payload.branch)
    } else {
      branches.push(payload.branch)
    }

    Vue.set(this.selectedBranches, payload.repoId, branches)
  }

  get selectedBranchesForRepo(): (repoId: RepoId) => string[] {
    return repoId => this.selectedBranches[repoId] || []
  }

  get selectedBranchesMap(): Map<RepoId, string[]> {
    const repos = new Map<RepoId, string[]>()

    Object.entries(this.selectedBranches).forEach(([repoId, branches]) => {
      if (branches.length > 0) {
        repos.set(repoId, branches)
      }
    })

    return repos
  }
}
