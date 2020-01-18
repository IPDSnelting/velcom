import { createModule, mutation, action } from 'vuex-class-component'
import axios from 'axios'

const VxModule = createModule({
  namespaced: 'userModule',
  strict: false
})

export class UserStore extends VxModule {
  private role: string | null = null
  private token: string | null = null

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
    const response = await axios.get('/test-token', {
      auth: {
        username: payload.role,
        password: payload.token
      },
      params: {
        repo_id: payload.asRepoAdmin && payload.role
      }
    })

    // was a 200
    this.role = payload.role
    this.token = payload.token
  }

  @action
  async logOut() {
    this.setRole(null)
    this.setToken(null)
  }

  get getToken(): string | null {
    return this.token
  }

  get getRole(): string | null {
    return this.role
  }

  /**
   * Returns whether the user is logged in. True if `getRole` and `getToken` return a string.
   *
   * @readonly
   * @type {boolean} true if the user is logged in
   * @memberof UserStore
   */
  get loggedIn(): boolean {
    return (
      this.role !== null &&
      this.role.length > 0 &&
      this.token !== null &&
      this.token.length > 0
    )
  }

  get authorized(): (payload: string) => boolean {
    return (payload: string) => this.role === 'ADMIN' || this.role === payload
  }
}
