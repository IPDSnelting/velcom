import Vue from 'vue'
import './class-components-router-hooks' // Register custom hooks
import App from './App.vue'
import router from './router'
import { store, vxm, restoreFromPassedSession } from './store'
import axios, { AxiosRequestConfig } from 'axios'
import vuetify from './plugins/vuetify'
import { extractErrorMessage } from './util/ErrorUtils'

Vue.config.productionTip = false

axios.defaults.baseURL = store.state.baseUrl

window.addEventListener('storage', event => {
  if (!event.newValue) {
    return
  }

  if (event.key === 'persisted_session_state' && sessionStorage.length === 0) {
    restoreFromPassedSession(event.newValue)
  }
})

if (sessionStorage.length === 0) {
  restoreFromPassedSession(null)
}

const vue = new Vue({
  router,
  store,
  vuetify,
  render: h => h(App)
})

axios.interceptors.request.use(function(config) {
  if (vxm.userModule.loggedIn) {
    config.auth = {
      username: vxm.userModule.role!,
      password: vxm.userModule.token!
    }
  }
  return config
})

let loadingElements: Set<number> = new Set()

// Intercept requests and show a loading indicator / errors
axios.interceptors.request.use(
  function(config) {
    if (!config.hideLoadingSnackbar && !config.hideFromSnackbar) {
      let prefix = config.snackbarTag || ''
      let randomTag = Math.floor(Math.random() * Number.MAX_VALUE)
      config.randomTag = randomTag
      loadingElements.add(randomTag)

      setTimeout(() => {
        if (loadingElements.has(randomTag)) {
          vue.$globalSnackbar.setLoading(prefix)
        }
      }, 1000)
    }
    return config
  },
  function(error) {
    loadingElements.delete(error.config.randomTag)

    // Always display network errors
    if (!error.response) {
      let prefix = error.config.snackbarTag || ''
      vue.$globalSnackbar.setError(prefix, extractErrorMessage(error))
    }
    return Promise.reject(error)
  }
)

// Intercept responses to show errors
axios.interceptors.response.use(
  function(response) {
    loadingElements.delete(response.config.randomTag!)

    let prefix = response.config.snackbarTag || ''
    vue.$globalSnackbar.finishedLoading(prefix)

    if (response.config.showSuccessSnackbar) {
      vue.$globalSnackbar.setSuccess(prefix, 'Success!')
    }

    return response
  },
  function(error) {
    loadingElements.delete(error.config.randomTag)

    if (!error.config.hideFromSnackbar && !error.config.hideErrorSnackbar) {
      let prefix = error.config.snackbarTag || ''
      vue.$globalSnackbar.setError(prefix, extractErrorMessage(error))
    }
    return Promise.reject(error)
  }
)

vue.$mount('#app')
