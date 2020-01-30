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
  repoComparisonModule: PersistedRepoComparisonStore
  repoModule: PersistedRepoStore
  repoDetailModule: PersisteRepoDetailStore
  userModule: UserStore
}

interface PersistedRepoStore {
  repoIndices: { [repoID: string]: number }
  currentRepoIndex: number
}

interface PersistedRepoComparisonStore {
  _selectedRepos: string[]
  _selectedBranchesByRepoID: { [key: string]: string[] }
  startTime: string
  stopTime: string
}

interface PersisteRepoDetailStore {
  _selectedRepoId: string
}

interface PersistedSessionRootState {
  repoComparisonModule: PersistedRepoComparisonStore
  repoDetailModule: PersisteRepoDetailStore
}

const persistenceLocalStorage = new VuexPersistence<RootState>({
  storage: window.localStorage,
  reducer: state => {
    return {
      userModule: state.userModule,
      colorModule: state.colorModule,
      repoStore: {
        repoIndices: state.repoModule.repoIndices,
        currentRepoIndex: state.repoModule.currentRepoIndex
      }
    }
  }
})

const persistenceSessionStorage = new VuexPersistence<RootState>({
  storage: window.sessionStorage,
  reducer: state => {
    return {
      repoComparisonModule: {
        _selectedRepos: state.repoComparisonModule._selectedRepos,
        _selectedBranchesByRepoID:
          state.repoComparisonModule._selectedBranchesByRepoID,
        startTime: state.repoComparisonModule.startTime,
        stopTime: state.repoComparisonModule.stopTime
      },
      repoDetailModule: {
        _selectedRepoId: state.repoDetailModule._selectedRepoId
      }
    }
  }
})

Vue.use(Vuex)

export const store = new Vuex.Store({
  state: {
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

export function restoreFromPassedSession(state: PersistedSessionRootState) {
  console.log(
    'Restoring state donated by another tab: ' + JSON.stringify(state)
  )

  // Detail module
  vxm.repoDetailModule.selectedRepoId = state.repoDetailModule._selectedRepoId

  // Comparison module
  Object.assign(vxm.repoComparisonModule, state.repoComparisonModule)
}
