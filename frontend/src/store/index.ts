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
import { StatusComparisonStore } from './modules/statusComparisonStore'
import {
  deletedOutdatedLocalData,
  persistenceLocalStorage,
  persistenceSessionStorage
} from './persistence'
import { RunSearchStore } from '@/store/modules/runSearchStore'

export interface RootState {
  colorModule: ColorStore
  commitDetailComparisonModule: CommitDetailComparisonStore
  newsModule: NewsStore
  queueModule: QueueStore
  comparisonGraphModule: ComparisonGraphStore
  repoModule: RepoStore
  detailGraphModule: DetailGraphStore
  userModule: UserStore
  runSearchModule: RunSearchStore
  statusComparisonModule: StatusComparisonStore
}

deletedOutdatedLocalData()

Vue.use(Vuex)

export const store = new Vuex.Store({
  state: {} as RootState,
  modules: {
    ...extractVuexModule(ColorStore),
    ...extractVuexModule(CommitDetailComparisonStore),
    ...extractVuexModule(NewsStore),
    ...extractVuexModule(QueueStore),
    ...extractVuexModule(ComparisonGraphStore),
    ...extractVuexModule(DetailGraphStore),
    ...extractVuexModule(RepoStore),
    ...extractVuexModule(UserStore),
    ...extractVuexModule(RunSearchStore),
    ...extractVuexModule(StatusComparisonStore)
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
  detailGraphModule: createProxy(store, DetailGraphStore),
  runSearchModule: createProxy(store, RunSearchStore),
  statusComparisonModule: createProxy(store, StatusComparisonStore)
}
