import { createModule, mutation } from 'vuex-class-component'
import { RepoId } from '@/store/types'
import Vue from 'vue'

const VxModule = createModule({
  namespaced: 'comparisonGraphModule',
  strict: false
})

export class ComparisonGraphStore extends VxModule {
  private _selectedBranches: { [id: string]: string[] } = {}

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
