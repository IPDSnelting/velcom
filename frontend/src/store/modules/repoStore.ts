import { createModule, mutation, action } from 'vuex-class-component'
import { Repo, RepoId, Dimension } from '@/store/types'
import Vue from 'vue'
import axios from 'axios'
import { vxm } from '..'
import { repoFromJson } from '@/util/RepoJsonHelper'

const VxModule = createModule({
  namespaced: 'repoModule',
  strict: false
})

export class RepoStore extends VxModule {
  private repos: { [repoId: string]: Repo } = {}
  private currentRepoIndex: number = 0
  private repoIndices: { [repoID: string]: number } = {}

  @action
  async fetchRepos(): Promise<Repo[]> {
    const response = await axios.get('/all-repos', {
      snackbarTag: 'all repos',
      hideLoadingSnackbar: true
      // still show errors
    })

    const repos: Repo[] = response.data.repos.map(repoFromJson)
    repos.sort((a, b) => a.id.localeCompare(b.id))

    this.setRepos(repos)
    return repos
  }

  @action
  async fetchRepoById(repoId: string): Promise<Repo> {
    const response = await axios.get(`/repo/${repoId}`, {
      snackbarTag: 'repo-detail'
    })

    const repo: Repo = repoFromJson(response.data.repo)
    this.setRepo(repo)
    return this.repoById(repo.id)!
  }

  @action
  async addRepo(payload: {
    repoName: string
    remoteUrl: string
  }): Promise<Repo> {
    const response = await axios.post('/repo', {
      name: payload.repoName,
      remote_url: payload.remoteUrl
    })

    const repo: Repo = repoFromJson(response.data.repo)
    this.setRepo(repo)
    return this.repoById(repo.id)!
  }

  /**
   * Deletes the repo with the given id.
   *
   * @param {RepoId} repoId the repo id
   * @returns a promise completing when the repo was deleted.
   * @memberof RepoStore
   */
  @action
  async deleteRepo(repoId: RepoId): Promise<void> {
    await axios.delete(`/repo/${repoId}`)
    this.removeRepo(repoId)
  }

  /**
   * Updates a repo.
   * @param payload contains the id, the new name (or undefined if unchanged),
   * the new remote URL (or undefined if unchanged), the new tracked branches
   * (or undefined if unchanged)
   */
  @action
  async updateRepo(payload: {
    id: RepoId
    name: string | undefined
    remoteUrl: string | undefined
    trackedBranches: string[] | undefined
  }): Promise<void> {
    await axios.patch(`/repo/${payload.id}`, {
      name: payload.name,
      remote_url: payload.remoteUrl,
      tracked_branches: payload.trackedBranches
    })
    await this.fetchRepoById(payload.id)
  }

  @action
  async triggerListenerFetch(): Promise<void> {
    await axios.post(`/listener/fetch-all`, undefined, {
      snackbarPriority: 2
    })
  }

  @mutation
  setIndexForRepo(repoId: RepoId): void {
    if (!this.repoIndices[repoId]) {
      this.repoIndices[repoId] = this.currentRepoIndex++
    }
  }

  @mutation
  setRepo(payload: Repo): void {
    if (this.repos[payload.id]) {
      const existing = this.repos[payload.id]
      existing.branches = payload.branches.slice()
      existing.name = payload.name
      existing.remoteURL = payload.remoteURL
      existing.dimensions = payload.dimensions.slice()
    } else {
      Vue.set(this.repos, payload.id, payload)
      vxm.repoModule.setIndexForRepo(payload.id)
    }
  }

  @mutation
  setRepos(repos: Repo[]): void {
    Array.from(Object.keys(this.repos)).forEach(it =>
      Vue.delete(this.repos, it)
    )
    repos.forEach(repo => vxm.repoModule.setRepo(repo))
  }

  @mutation
  removeRepo(payload: RepoId): void {
    Vue.delete(this.repos, payload)
  }

  get allRepos(): Repo[] {
    return Array.from(Object.values(this.repos))
  }

  get repoById(): (payload: RepoId) => Repo | undefined {
    return (payload: RepoId) => this.repos[payload]
  }

  get trackedBranchesByRepoID(): { [key: string]: string[] } {
    const branchesByRepoID: { [key: string]: string[] } = {}
    this.allRepos.forEach(repo => {
      branchesByRepoID[repo.id] = repo.branches
        .filter(it => it.tracked)
        .map(it => it.name)
    })
    return branchesByRepoID
  }

  get repoIndex(): (repoID: RepoId) => number {
    return (repoID: string) => this.repoIndices[repoID]
  }

  get occuringDimensions(): (selectedRepos: RepoId[]) => Dimension[] {
    return (selectedRepos: RepoId[]) => {
      return Object.values(this.repos)
        .filter(repo => selectedRepos.includes(repo.id))
        .flatMap(repo => repo.dimensions)
        .filter(
          (dimension, index, dimensionArray) =>
            index === dimensionArray.findIndex(dim => dim.equals(dimension))
        )
    }
  }

  get occuringBenchmarks(): (selectedRepos: RepoId[]) => string[] {
    return (selectedRepos: string[]) => {
      const benchmarks = Object.values(this.repos)
        .filter(repo => selectedRepos.includes(repo.id))
        .flatMap(repo => repo.dimensions)
        .map(dimension => dimension.benchmark)
        .reduce(
          (benchmarks, newBenchmark) => benchmarks.add(newBenchmark),
          new Set<string>()
        )

      return Array.from(benchmarks).sort((a, b) =>
        a.localeCompare(b, undefined, { sensitivity: 'base' })
      )
    }
  }

  get metricsForBenchmark(): (benchmark: string) => string[] {
    return (benchmark: string) => {
      const metrics = Object.values(this.repos)
        .flatMap(repo => repo.dimensions)
        .filter(dimension => dimension.benchmark === benchmark)
        .map(dimension => dimension.metric)
        .reduce(
          (metrics, newMetric) => metrics.add(newMetric),
          new Set<string>()
        )
      return Array.from(metrics).sort((a, b) =>
        a.localeCompare(b, undefined, { sensitivity: 'base' })
      )
    }
  }

  /**
   * Converts a given store to a pure object that can be serialized.
   *
   * @param store the store to convert
   */
  static toPlainObject(store: RepoStore): unknown {
    return {
      repos: store.repos,
      repoIndex: store.currentRepoIndex,
      repoIndices: store.repoIndices
    }
  }
}
