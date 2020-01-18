import { createModule, mutation, action } from 'vuex-class-component'
import axios from 'axios'

const VxModule = createModule({
  namespaced: 'userModule',
  strict: false
})

export class UserStore extends VxModule {
  private role: string | null = ''
  private token: string | null = ''

  @mutation
  setRole(payload: string | null) {
    this.role = payload
  }

  @mutation
  setToken(payload: string | null) {
    this.token = payload
  }

  @action
  async logIn(payload: { role: string; asRepoAdmin: boolean; token: string }) {
    axios
      .get('/test-token', {
        auth: {
          username: payload.role,
          password: payload.token
        },
        params: {
          repo_id: payload.asRepoAdmin && payload.role
        }
      })
      .then(response => {
        this.role = payload.role
        this.token = payload.token
      })
  }

  @action
  async logOut() {
    this.setRole(null)
    this.setToken(null)
  }

  get loggedIn(): boolean {
    return this.role === null
  }

  get authorized(): (payload: string) => boolean {
    return (payload: string) => this.role === 'ADMIN' || this.role === payload
  }
}
