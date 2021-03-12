import { createModule, mutation } from 'vuex-class-component'
import { RepoId } from '@/store/types'
import Vue from 'vue'

const VxModule = createModule({
  namespaced: 'comparisonGraphModule',
  strict: false
})

function defaultStartDate() {
  // One week in the past
  return new Date(
    new Date(new Date().setDate(new Date().getDate() - 7)).setHours(0, 0, 0, 0)
  )
}

function defaultEndDate() {
  // Today at midnight / start of tomorrow
  return new Date(new Date().setHours(24, 0, 0, 0))
}

export class ComparisonGraphStore extends VxModule {
  private _selectedBranches: { [id: string]: string[] } = {}

  startTime: Date = defaultStartDate()
  endTime: Date = defaultEndDate()

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
}
