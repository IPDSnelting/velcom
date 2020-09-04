import Vuex from 'vuex'
import { extractVuexModule, createProxy } from 'vuex-class-component'
import Vue from 'vue'
import { RepoStore } from './modules/repoStore'
import { UserStore } from './modules/userStore'
import { ColorStore } from './modules/colorStore'
import { CommitDetailComparisonStore } from './modules/commitDetailComparisonStore'
import { NewsStore } from './modules/newsStore'
import { QueueStore } from './modules/queueStore'
import { RepoComparisonStore } from './modules/repoComparisonStore'
import { DetailGraphStore } from './modules/detailGraphStore'
import VuexPersistence from 'vuex-persist'
import { Commit, RepoId } from './types'

interface RootState {
  baseUrl: string
  colorModule: ColorStore
  commitDetailComparisonModule: CommitDetailComparisonStore
  newsModule: NewsStore
  queueModule: QueueStore
  repoComparisonModule: PersistedRepoComparisonStore
  repoModule: PersistedRepoStore
  detailGraphModule: PersistedDetailGraphStore
  userModule: UserStore
}

interface PersistedRepoStore {
  repoIndices: { [repoID: string]: number }
  currentRepoIndex: number
}

interface PersistedRepoComparisonStore {
  _selectedRepos: string[]
  _selectedBranchesByRepoID: { [key: string]: string[] }
  _referenceCommit: Commit | null
  startTime: string
  stopTime: string
  selectedMetric: string
  selectedBenchmark: string
}

interface PersistedDetailGraphStore {
  _selectedRepoId: RepoId

  lockedToRelativeCommit: boolean
  relativeToCommit: string
  _selectedMeasurements: { metric: string; benchmakr: string }[]
  selectedFetchAmount: string
  selectedSkipAmount: string
  _referenceDatapoint: {
    commit: Commit
    comparison: CommitComparison
  } | null
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
        _referenceCommit: state.repoComparisonModule._referenceCommit,
        startTime: state.repoComparisonModule.startTime,
        stopTime: state.repoComparisonModule.stopTime,
        selectedBenchmark: state.repoComparisonModule.selectedBenchmark,
        selectedMetric: state.repoComparisonModule.selectedMetric
      },
      repoDetailModule: {
        _selectedRepoId: state.repoDetailModule._selectedRepoId,
        lockedToRelativeCommit: state.repoDetailModule.lockedToRelativeCommit,
        _selectedMeasurements: state.repoDetailModule._selectedMeasurements,
        relativeToCommit: state.repoDetailModule.relativeToCommit,
        _referenceDatapoint: state.repoDetailModule._referenceDatapoint,
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
    ...extractVuexModule(CommitDetailComparisonStore),
    ...extractVuexModule(NewsStore),
    ...extractVuexModule(QueueStore),
    ...extractVuexModule(RepoComparisonStore),
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
  repoComparisonModule: createProxy(store, RepoComparisonStore),
  repoModule: createProxy(store, RepoStore),
  userModule: createProxy(store, UserStore),
  repoDetailModule: createProxy(store, DetailGraphStore)
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
