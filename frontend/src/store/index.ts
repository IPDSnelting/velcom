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
import VuexPersistence from 'vuex-persist'

interface RootState {
  baseUrl: string
  colorModule: ColorStore
  commitComparisonModule: CommitComparisonStore
  newsModule: NewsStore
  queueModule: QueueStore
  repoComparisonModule: RepoComparisonStore
  repoModule: RepoStore
  repoDetailModule: RepoDetailStore
  userModule: UserStore
}

const persistenceLocalStorage = new VuexPersistence<RootState>({
  storage: window.localStorage,
  reducer: state => {
    return {
      userModule: state.userModule,
      colorModule: state.colorModule
    }
  }
})

const persistenceSessionStorage = new VuexPersistence<RootState>({
  storage: window.sessionStorage,
  reducer: state => {
    return {
      repoComparisonModule: {
        // Dirty hack as those states are private but still need to be persisted...
        _selectedRepos: (state.repoComparisonModule as any)._selectedRepos,
        _selectedBranchesByRepoID: (state.repoComparisonModule as any)
          ._selectedBranchesByRepoID,
        startTime: (state.repoComparisonModule as any).startTime,
        stopTime: (state.repoComparisonModule as any).stopTime
      },
      repoDetailModule: {
        _selectedRepoId: (state.repoDetailModule as any)._selectedRepoId
      }
    }
  }
})

Vue.use(Vuex)

export const store = new Vuex.Store({
  state: {
    // baseUrl: 'https://aaaaaaah.de:8667/'
    baseUrl: process.env.VUE_APP_BASE_URL
  } as RootState,
  modules: {
    ...extractVuexModule(ColorStore),
    ...extractVuexModule(CommitComparisonStore),
    ...extractVuexModule(NewsStore),
    ...extractVuexModule(QueueStore),
    ...extractVuexModule(RepoComparisonStore),
    ...extractVuexModule(RepoDetailStore),
    ...extractVuexModule(RepoStore),
    ...extractVuexModule(UserStore)
  },
  plugins: [persistenceLocalStorage.plugin, persistenceSessionStorage.plugin]
})

export const vxm = {
  colorModule: createProxy(store, ColorStore),
  commitComparisonModule: createProxy(store, CommitComparisonStore),
  newsModule: createProxy(store, NewsStore),
  queueModule: createProxy(store, QueueStore),
  repoComparisonModule: createProxy(store, RepoComparisonStore),
  repoModule: createProxy(store, RepoStore),
  userModule: createProxy(store, UserStore),
  repoDetailModule: createProxy(store, RepoDetailStore)
}
