import { action, createModule, mutation } from 'vuex-class-component'
import {
  AttributedDatapoint,
  ComparisonDataPoint,
  Dimension,
  RepoId
} from '@/store/types'
import Vue from 'vue'
import axios from 'axios'
import { comparisonDatapointFromJson } from '@/util/GraphJsonHelper'

const VxModule = createModule({
  namespaced: 'comparisonGraphModule',
  strict: false
})

function defaultStartDate() {
  // FIXME: 7 not 14
  // One week in the past
  return new Date(
    new Date(new Date().setDate(new Date().getDate() - 14)).setHours(0, 0, 0, 0)
  )
}

function defaultEndDate() {
  // Today at midnight / start of tomorrow
  return new Date(new Date().setHours(24, 0, 0, 0))
}

function roundDateUp(date: Date): Date {
  const copy = new Date(date)
  if (!(date.getHours() !== 0)) {
    copy.setHours(24, 0, 0, 0) // next midnight
  }
  return copy
}

function roundDateDown(date: Date): Date {
  const copy = new Date(date)
  if (!(date.getHours() !== 0)) {
    copy.setHours(0, 0, 0, 0) // this midnight
  }
  return copy
}

function formatRepos(repos: Map<RepoId, string[]>): string {
  return Array.from(repos.entries())
    .filter(([, branches]) => branches.length > 0)
    .map(([repoId, branches]) => {
      return repoId + ':' + branches.join(':')
    })
    .join('::')
}

export class ComparisonGraphStore extends VxModule {
  private _selectedBranches: { [id: string]: string[] } = {}

  startTime: Date = defaultStartDate()
  endTime: Date = defaultEndDate()

  selectedDimension: Dimension | null = null
  zoomXStart: Date | null = null
  zoomXEnd: Date | null = null
  zoomYStart: Date | null = null
  zoomYEnd: Date | null = null

  commitToCompare: AttributedDatapoint | null = null
  referenceDatapoint: AttributedDatapoint | null = null

  @action
  async fetchComparisonGraph(): Promise<ComparisonDataPoint[]> {
    if (this.selectedDimension === null) {
      return []
    }

    const adjustedStartDate = roundDateDown(this.startTime).getTime() / 1000
    const adjustedEndDate = roundDateUp(this.endTime).getTime() / 1000

    const response = await axios.get('/graph/comparison', {
      params: {
        start_time: adjustedStartDate,
        end_time: adjustedEndDate,
        dimension:
          this.selectedDimension.benchmark +
          ':' +
          this.selectedDimension.metric,
        repos: formatRepos(this.selectedBranches)
      }
    })

    return response.data.repos.flatMap((repo: any) =>
      comparisonDatapointFromJson(this.selectedDimension!, repo)
    )
  }

  @mutation
  setSelectedBranchesForRepo(payload: {
    repoId: RepoId
    branches: string[]
  }): void {
    Vue.set(this._selectedBranches, payload.repoId, payload.branches)
  }

  @mutation
  toggleRepoBranch(payload: { repoId: RepoId; branch: string }): void {
    let branches: string[] = this._selectedBranches[payload.repoId] || []

    if (branches.includes(payload.branch)) {
      branches = branches.filter(it => it !== payload.branch)
    } else {
      branches.push(payload.branch)
    }

    Vue.set(this._selectedBranches, payload.repoId, branches)
  }

  get selectedBranchesForRepo(): (repoId: RepoId) => string[] {
    return repoId => this._selectedBranches[repoId] || []
  }

  get selectedBranches(): Map<RepoId, string[]> {
    const map: Map<RepoId, string[]> = new Map()
    Object.keys(this._selectedBranches).forEach(repoId => {
      const branches = this._selectedBranches[repoId]
      if (branches && branches.length > 0) {
        map.set(repoId, branches)
      }
    })
    return map
  }
}
