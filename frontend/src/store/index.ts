import Vuex from 'vuex'
import { extractVuexModule, createProxy } from 'vuex-class-component'
import Vue from 'vue'
import { RepoStore } from './modules/repoStore'
import { UserStore } from './modules/userStore'
import { ColorStore } from './modules/colorStore'
import { CommitComparisonStore } from './modules/commitComparisonStore'
import { NewsStore } from './modules/newsStore'
import { QueueStore } from './modules/queueStore'
import { RepoComparisonStore } from './modules/repoComparisonStore'
import { RepoDetailStore } from './modules/repoDetailStore'
Vue.use(Vuex)

export const store = new Vuex.Store({
  state: {
    baseUrl: 'http://localhost:8080/'
  },
  modules: {
    ...extractVuexModule(ColorStore),
    ...extractVuexModule(CommitComparisonStore),
    ...extractVuexModule(NewsStore),
    ...extractVuexModule(QueueStore),
    ...extractVuexModule(RepoComparisonStore),
    ...extractVuexModule(RepoDetailStore),
    ...extractVuexModule(RepoStore),
    ...extractVuexModule(UserStore)
  }
})

export const vxm = {
  colorModule: createProxy(store, ColorStore),
  commitComparisonModule: createProxy(store, CommitComparisonStore),
  newsModule: createProxy(store, NewsStore),
  queueModule: createProxy(store, QueueStore),
  repoComparisonModule: createProxy(store, RepoComparisonStore),
  repoModule: createProxy(store, RepoStore),
  userModule: createProxy(store, UserStore)
}
