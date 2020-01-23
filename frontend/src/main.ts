import Vue from 'vue'
import App from './App.vue'
import router from './router'
import { store, vxm } from './store'
import axios from 'axios'
import vuetify from './plugins/vuetify'
import { extractErrorMessage } from './util/ErrorUtils'

Vue.config.productionTip = false

axios.defaults.baseURL = store.state.baseUrl

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

// Intercepts requests and show a loading indicator / errors
axios.interceptors.request.use(
  function(config) {
    if (!config.hideLoadingSnackbar && !config.hideFromSnackbar) {
      vue.$globalSnackbar.setLoading()
    }
    return config
  },
  function(error) {
    // Always display network errors
    if (!error.response) {
      vue.$globalSnackbar.setError(extractErrorMessage(error))
    }
    return Promise.reject(error)
  }
)

// Intercept responses to show errors
axios.interceptors.response.use(
  function(response) {
    if (
      !response.config.hideSuccessSnackbar &&
      !response.config.hideFromSnackbar
    ) {
      vue.$globalSnackbar.finishedLoading()
    }
    return response
  },
  function(error) {
    if (!error.hideFromSnackbar && !error.hideFromSnackbar) {
      vue.$globalSnackbar.setError(extractErrorMessage(error))
    }
    return Promise.reject(error)
  }
)

vue.$mount('#app')
