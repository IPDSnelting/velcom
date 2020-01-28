import { createModule, mutation, action } from 'vuex-class-component'
import { Repo } from '@/store/types'
import Vue from 'vue'
import axios from 'axios'
import { vxm } from '..'

const VxModule = createModule({
  namespaced: 'repoModule',
  strict: false
})

export class RepoStore extends VxModule {
  private repos: { [repoID: string]: Repo } = {}
  private currentRepoIndex: number = 0
  private repoIndices: { [repoID: string]: number } = {}

  @action
  async fetchRepos() {
    const response = await axios.get('/all-repos', {
      snackbarTag: 'all repos',
      hideLoadingSnackbar: true,
      hideSuccessSnackbar: true
      // still show errors
    })

    let repos: Array<Repo> = []
    let jsonData: Array<any> = response.data.repos

    jsonData.forEach((item: any) => {
      repos.push(
        new Repo(
          item.id,
          item.name,
          item.branches,
          item.tracked_branches,
          item.measurements,
          item.remote_url
        )
      )
    })

    this.setRepos(repos)
    return repos
  }

  @action
  async fetchRepoByID(payload: string) {
    const response = await axios.get('/repo', {
      snackbarTag: 'repo-detail',
      params: {
        repo_id: payload
      }
    })

    let item = response.data.repo
    let repo = new Repo(
      item.id,
      item.name,
      item.branches,
      item.tracked_branches,
      item.measurements,
      item.remote_url
    )

    this.setRepo(repo)
    return this.repoByID(repo.id)!
  }

  @action
  async addRepo(payload: {
    repoName: string
    remoteUrl: string
    repoToken: string | undefined
  }) {
    return axios
      .post('/repo', {
        name: payload.repoName,
        remote_url: payload.remoteUrl,
        token: payload.repoToken
      })
      .then(response => {
        let item = response.data['repo']

        let repo = new Repo(
          item.id,
          item.name,
          item.branches,
          item.tracked_branches,
          item.measurements,
          item.remote_url
        )

        this.setRepo(repo)
        return repo
      })
  }

  /**
   * Deletes the repo with the given id.
   *
   * @param {string} payload the repo id
   * @returns a promise completing when the repo was deleted.
   * @memberof RepoStore
   */
  @action
  async deleteRepo(payload: string): Promise<void> {
    return axios
      .delete('/repo', {
        params: {
          repo_id: payload
        }
      })
      .then(response => {
        this.removeRepo(payload)
      })
  }

  @action
  async updateRepo(payload: {
    id: string
    name: string | undefined
    repoToken: string | undefined | null
    remoteUrl: string | undefined
    trackedBranches: string[] | undefined
  }): Promise<void> {
    return axios
      .patch('/repo', {
        repo_id: payload.id,
        name: payload.name,
        token: payload.repoToken,
        remote_url: payload.remoteUrl,
        tracked_branches: payload.trackedBranches
      })
      .then(response => {
        this.fetchRepoByID(payload.id)
      })
  }

  @mutation
  setIndexForRepo(repoID: string) {
    if (!this.repoIndices[repoID]) {
      this.repoIndices[repoID] = this.currentRepoIndex++
    }
  }

  @mutation
  setRepo(payload: Repo) {
    if (this.repos[payload.id]) {
      const existing = this.repos[payload.id]
      existing.branches = payload.branches.slice()
      existing.trackedBranches = payload.trackedBranches.slice()
      existing.name = payload.name
      existing.remoteURL = payload.remoteURL
      existing.measurements = payload.measurements.slice()
    } else {
      Vue.set(this.repos, payload.id, { ...payload })
      vxm.repoModule.setIndexForRepo(payload.id)
    }
  }

  @mutation
  setRepos(repos: Repo[]) {
    repos.forEach(repo => {
      if (this.repos[repo.id]) {
        Object.assign(this.repos[repo.id], repo)
      } else {
        Vue.set(this.repos, repo.id, { ...repo })
        vxm.repoModule.setIndexForRepo(repo.id)
      }
    })
  }

  @mutation
  removeRepo(payload: string) {
    Vue.delete(this.repos, payload)
  }

  get allRepos() {
    return Array.from(Object.values(this.repos))
  }

  get repoByID(): (payload: string) => Repo | undefined {
    return (payload: string) => this.repos[payload]
  }

  get branchesByRepoID(): { [key: string]: string[] } {
    var branchesByRepoID: { [key: string]: string[] } = {}
    this.allRepos.forEach(repo => {
      branchesByRepoID[repo.id] = repo.trackedBranches
    })
    return branchesByRepoID
  }

  get repoIndex(): (repoID: string) => number {
    return (repoID: string) => this.repoIndices[repoID]
  }

  get occuringBenchmarks(): (selectedRepos: string[]) => string[] {
    return (selectedRepos: string[]) => {
      let benchmarks = Object.values(this.repos)
        .filter(repo => selectedRepos.includes(repo.id))
        .flatMap(repo => repo.measurements)
        .map(measurement => measurement.benchmark)
        .reduce(
          (benchmarks, newBenchmark) => benchmarks.add(newBenchmark),
          new Set<string>()
        )

      return Array.from(benchmarks).sort()
    }
  }

  get metricsForBenchmark(): (benchmark: string) => string[] {
    return (benchmark: string) => {
      let metrics = Object.values(this.repos)
        .flatMap(repo => repo.measurements)
        .filter(measurement => measurement.benchmark === benchmark)
        .map(measurement => measurement.metric)
        .reduce(
          (metrics, newMetric) => metrics.add(newMetric),
          new Set<string>()
        )
      return Array.from(metrics).sort()
    }
  }
}
