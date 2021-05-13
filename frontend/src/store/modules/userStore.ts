import { createModule, mutation, action } from 'vuex-class-component'
import axios from 'axios'

const VxModule = createModule({
  namespaced: 'userModule',
  strict: false
})

export class UserStore extends VxModule {
  private _token: string | null = null

  private _darkThemeSelected: boolean | undefined = undefined

  @action
  async logIn(token: string): Promise<void> {
    const response = await axios.get('/test-token', {
      auth: {
        username: 'admin',
        password: token
      },
      snackbarTag: 'login'
    })

    if (response.status === 200 || response.status === 204) {
      this.setToken(token)
    }
  }

  @action
  async logOut(): Promise<void> {
    this.setToken(null)
  }

  @mutation
  setToken(payload: string | null): void {
    this._token = payload
  }

  get token(): string | null {
    return this._token
  }

  get isAdmin(): boolean {
    return this.token !== null && this.token.length > 0
  }

  get darkThemeSelected(): boolean {
    if (this._darkThemeSelected !== undefined) {
      return this._darkThemeSelected
    }
    return this.browserPrefersDarkTheme
  }

  set darkThemeSelected(selected: boolean) {
    this._darkThemeSelected = selected
  }

  get browserPrefersDarkTheme(): boolean {
    return window.matchMedia('(prefers-color-scheme: dark)').matches
  }

  get usesBrowsersThemePreferences(): boolean {
    return this._darkThemeSelected === undefined
  }

  /**
   * Converts a given store to a pure object that can be serialized.
   *
   * @param store the store to convert
   */
  static toPlainObject(store: UserStore): unknown {
    return {
      _token: store._token,
      _darkThemeSelected: store._darkThemeSelected
    }
  }
}
