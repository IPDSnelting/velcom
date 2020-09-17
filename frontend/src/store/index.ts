import Vuex from 'vuex'
import { extractVuexModule, createProxy } from 'vuex-class-component'
import Vue from 'vue'
import { RepoStore } from './modules/repoStore'
import { UserStore } from './modules/userStore'
import { ColorStore } from './modules/colorStore'
import { CommitDetailComparisonStore } from './modules/commitDetailComparisonStore'
import { NewsStore } from './modules/newsStore'
import { QueueStore } from './modules/queueStore'
import { ComparisonGraphStore } from './modules/comparisonGraphStore'
import { DetailGraphStore } from './modules/detailGraphStore'
import {
  persistenceLocalStorage,
  persistenceSessionStorage
} from './persistence'

export interface RootState {
  baseUrl: string
  colorModule: ColorStore
  commitDetailComparisonModule: CommitDetailComparisonStore
  newsModule: NewsStore
  queueModule: QueueStore
  comparisonGraphModule: ComparisonGraphStore
  repoModule: RepoStore
  detailGraphModule: DetailGraphStore
  userModule: UserStore
}

Vue.use(Vuex)

export const store = new Vuex.Store({
  state: {
    baseUrl: process.env.VUE_APP_BASE_URL
  } as RootState,
  modules: {
    ...extractVuexModule(ColorStore),
    ...extractVuexModule(CommitDetailComparisonStore),
    ...extractVuexModule(NewsStore),
    ...extractVuexModule(QueueStore),
    ...extractVuexModule(ComparisonGraphStore),
    ...extractVuexModule(DetailGraphStore),
    ...extractVuexModule(RepoStore),
    ...extractVuexModule(UserStore)
  },
  plugins: [persistenceLocalStorage.plugin, persistenceSessionStorage.plugin]
})

export const vxm = {
  colorModule: createProxy(store, ColorStore),
  commitDetailComparisonModule: createProxy(store, CommitDetailComparisonStore),
  newsModule: createProxy(store, NewsStore),
  queueModule: createProxy(store, QueueStore),
  comparisonGraphModule: createProxy(store, ComparisonGraphStore),
  repoModule: createProxy(store, RepoStore),
  userModule: createProxy(store, UserStore),
  detailGraphModule: createProxy(store, DetailGraphStore)
}
