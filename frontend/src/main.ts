import Vue from 'vue'
import './class-components-router-hooks' // Register custom hooks
import App from './App.vue'
import router from './router'
import { store, vxm } from './store'
import axios from 'axios'
import vuetify from './plugins/vuetify'
import { extractErrorMessage } from './util/ErrorUtils'
import { restoreFromPassedSession } from './store/persistence'

Vue.config.productionTip = false

axios.defaults.baseURL = store.state.baseUrl

window.addEventListener('storage', event => {
  if (!event.newValue) {
    return
  }

  if (event.key === 'persisted_session_state' && sessionStorage.length === 0) {
    Vue.nextTick(() => restoreFromPassedSession())
  }
})

if (sessionStorage.length === 0) {
  // noinspection JSIgnoredPromiseFromCall
  restoreFromPassedSession()
}

const vue = new Vue({
  router,
  store,
  vuetify,
  render: h => h(App)
})

axios.interceptors.request.use(config => {
  if (vxm.userModule.isAdmin) {
    config.auth = {
      username: 'admin',
      password: vxm.userModule.token!
    }
  }
  return config
})

const loadingElements: Set<number> = new Set()

// Intercept requests and show a loading indicator / errors
axios.interceptors.request.use(
  config => {
    if (!config.hideLoadingSnackbar && !config.hideFromSnackbar) {
      const prefix = config.snackbarTag || ''
      const randomTag = Math.floor(Math.random() * Number.MAX_VALUE)
      config.randomTag = randomTag
      loadingElements.add(randomTag)

      setTimeout(() => {
        if (loadingElements.has(randomTag)) {
          vue.$globalSnackbar.setLoading(prefix, config.snackbarPriority)
        }
      }, 1000)
    }
    return config
  },
  error => {
    loadingElements.delete(error.config.randomTag)

    // Always display network errors
    if (!error.response) {
      const prefix = error.config.snackbarTag || ''
      vue.$globalSnackbar.setError(
        prefix,
        extractErrorMessage(error),
        error.config.snackbarPriority
      )
    }
    return Promise.reject(error)
  }
)

// Intercept responses to show errors
axios.interceptors.response.use(
  response => {
    loadingElements.delete(response.config.randomTag!)

    const prefix = response.config.snackbarTag || ''
    vue.$globalSnackbar.finishedLoading(
      prefix,
      response.config.snackbarPriority
    )

    if (response.config.showSuccessSnackbar) {
      vue.$globalSnackbar.setSuccess(
        prefix,
        'Success!',
        response.config.snackbarPriority
      )
    }

    return response
  },
  error => {
    loadingElements.delete(error.config.randomTag)

    if (!error.config.hideFromSnackbar && !error.config.hideErrorSnackbar) {
      const prefix = error.config.snackbarTag || ''
      vue.$globalSnackbar.setError(
        prefix,
        extractErrorMessage(error),
        error.config.snackbarPriority
      )
    }
    return Promise.reject(error)
  }
)

vue.$mount('#app')
