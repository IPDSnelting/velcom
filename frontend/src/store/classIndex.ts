import Vuex from 'vuex'
import { extractVuexModule, createProxy } from 'vuex-class-component'
import Vue from 'vue'
import { RepoStore } from '../store/modules/repoModule/repoModuleClass'
import { UserStore } from '../store/modules/userModule/userModuleClass'

Vue.use(Vuex)

export const store = new Vuex.Store({
  modules: {
    ...extractVuexModule(RepoStore),
    ...extractVuexModule(UserStore)
  }
})

export const vxm = {
  repoModule: createProxy(store, RepoStore),
  userModule: createProxy(store, UserStore)
}
