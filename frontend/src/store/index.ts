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
  selectedMetric: string
  selectedBenchmark: string
}

interface PersisteRepoDetailStore {
  _selectedRepoId: string

  selectedMetric: string
  selectedBenchmark: string
  selectedFetchAmount: string
  selectedSkipAmount: string
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
        stopTime: state.repoComparisonModule.stopTime,
        selectedBenchmark: state.repoComparisonModule.selectedBenchmark,
        selectedMetric: state.repoComparisonModule.selectedMetric
      },
      repoDetailModule: {
        _selectedRepoId: state.repoDetailModule._selectedRepoId,
        selectedMetric: state.repoDetailModule.selectedMetric,
        selectedBenchmark: state.repoDetailModule.selectedBenchmark,
        selectedFetchAmount: state.repoDetailModule.selectedFetchAmount,
        selectedSkipAmount: state.repoDetailModule.selectedSkipAmount
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

export function storeToLocalStorage() {
  let data = {
    data: JSON.stringify(sessionStorage),
    accessTime: new Date().getTime()
  }
  localStorage.setItem('persisted_session_state', JSON.stringify(data))
}

export function restoreFromPassedSession(rawData: string | null) {
  console.log('Trying to restore')

  if (rawData === null) {
    rawData = localStorage.getItem('persisted_session_state')

    if (rawData === null) {
      console.log('Raw data really null')
      return
    }
  }

  let { data, accessTime } = JSON.parse(rawData)

  // Older than 10 seconds. Should not happen, but better be safe than sorry.
  if (new Date().getTime() - accessTime > 10 * 1000) {
    console.log('Is too old: ' + accessTime)
    return
  }

  console.log('Used data: ' + data)

  let state: PersistedSessionRootState = JSON.parse(JSON.parse(data)['vuex'])

  console.log('Used state')
  console.log(state)

  // Detail module
  Object.assign(vxm.repoDetailModule, state.repoDetailModule)

  // Comparison module
  Object.assign(vxm.repoComparisonModule, state.repoComparisonModule)
}
