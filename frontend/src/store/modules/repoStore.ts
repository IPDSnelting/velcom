import { createModule, mutation, action } from 'vuex-class-component'
import { Repo } from '@/store/types'
import Vue from 'vue'
import axios from 'axios'

const VxModule = createModule({
  namespaced: 'repoModule',
  strict: false
})

export class RepoStore extends VxModule {
  private repos: { [key: string]: Repo } = {}

  @action
  async fetchRepos() {
    const response = await axios.get('/all-repos')

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
    }
  }

  @mutation
  setRepos(repos: Repo[]) {
    repos.forEach(repo => {
      if (this.repos[repo.id]) {
        Object.assign(this.repos[repo.id], repo)
      } else {
        Vue.set(this.repos, repo.id, { ...repo })
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

  get occuringBenchmarks() {
    var benchmarks: string[] = []
    const repos = Object.keys(this.repos).map(key => this.repos[key])

    repos.forEach(repo => {
      var measurements = repo.measurements
      measurements.forEach(measurement => {
        if (!benchmarks.includes(measurement.benchmark)) {
          benchmarks.push(measurement.benchmark)
        }
      })
    })
    return benchmarks
  }

  get metricsForBenchmark(): (payload: string) => string[] {
    return (payload: string) => {
      var metrics: string[] = []
      const repos = Object.keys(this.repos).map(key => this.repos[key])

      repos.forEach(repo => {
        var measurements = repo.measurements
        measurements.forEach(measurement => {
          if (
            measurement.benchmark === payload &&
            !metrics.includes(measurement.metric)
          ) {
            metrics.push(measurement.metric)
          }
        })
      })
      return metrics
    }
  }
}
