import { ActionTree } from 'vuex'
import axios, { AxiosPromise } from 'axios'
import { RepoState, RootState, Repo } from '../../types'

export declare class AddRepoRequest {
  public readonly repoName: string
  public readonly remoteUrl: string
  public readonly repoToken: string | null
}

export const actions: ActionTree<RepoState, RootState> = {
  async fetchRepos({ commit }): Promise<Array<Repo>> {
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

    commit('SET_REPOS', repos)
    return repos
  },

  async fetchRepoByID({ commit }, id: string): Promise<Repo> {
    const response = await axios.get('/repo', {
      params: {
        repo_id: id
      }
    })

    let repo = response.data.map((item: any) => {
      new Repo(
        item.id,
        item.name,
        item.branches,
        item.tracked_branches,
        item.measurements,
        item.remote_url
      )
    })

    commit('SET_REPO', repo)
    return repo
  },

  addRepo({ commit, rootGetters }, payload: AddRepoRequest): Promise<Repo> {
    return axios
      .post('/repo', {
        auth: {
          username: rootGetters['userModule/repoID'],
          password: rootGetters['userModule/token']
        },
        params: {
          name: payload.repoName,
          remote_url: payload.remoteUrl,
          token: payload.repoToken
        }
      })
      .then(response => {
        const repo: Repo = response.data
        commit('SET_REPO', repo)
        return repo
      })
  },

  deleteRepo({ commit, rootGetters }, repoID: string) {
    axios
      .delete('/repo', {
        auth: {
          username: rootGetters['userModule/repoID'],
          password: rootGetters['userModule/token']
        },
        params: {
          repo_id: repoID
        }
      })
      .then(
        response => {
          commit('REMOVE_REPO', repoID)
        },
        error => {
          console.log('error: could not remove repo ' + repoID)
          console.log(error)
        }
      )
  },

  updateRepo({ commit, dispatch, rootGetters }, payload) {
    axios
      .patch('/repo', {
        auth: {
          username: rootGetters['userModule/repoID'],
          password: rootGetters['userModule/token']
        },
        params: {
          repo_id: payload.id,
          name: payload.name,
          token: payload.token,
          remote_url: payload.remoteUrl
        }
      })
      .then(
        response => {
          dispatch('fetchRepoByID', payload.id)
        },
        error => {
          console.log('error: could not change repo ' + payload.name)
          console.log(error)
        }
      )
  }
}
