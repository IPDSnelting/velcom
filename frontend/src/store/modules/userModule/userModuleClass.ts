import { createModule, mutation, action } from 'vuex-class-component'
import axios from 'axios'

const VxModule = createModule({
  namespaced: 'userModule',
  strict: false
})

export class UserStore extends VxModule {
  private _role: string | null = null
  private _token: string | null = null

  @action
  async logIn(payload: { role: string; asRepoAdmin: boolean; token: string }) {
    const response = await axios.get('/test-token', {
      auth: {
        username: payload.role,
        password: payload.token
      },
      params: {
        repo_id: payload.asRepoAdmin ? payload.role : undefined
      }
    })

    // was a 200
    this._role = payload.role
    this._token = payload.token
  }

  @action
  async logOut() {
    this.setRole(null)
    this.setToken(null)
  }

  @mutation
  setRole(payload: string | null) {
    this._role = payload
  }

  @mutation
  setToken(payload: string | null) {
    this._token = payload
  }

  get token(): string | null {
    return this._token
  }

  get role(): string | null {
    return this._role
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

  get isAdmin(): boolean {
    return this._role === 'ADMIN'
  }

  get authorized(): (payload: string) => boolean {
    return (payload: string) => this._role === 'ADMIN' || this._role === payload
  }
}
