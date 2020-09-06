import { UserStore } from './modules/userStore'
import { ColorStore } from './modules/colorStore'
import { RootState, vxm } from './index'
import VuexPersistence from 'vuex-persist'
import { repoStoreToJson, repoStoreFromJson } from './modules/repoStore'
import {
  comparisonGraphStoreFromJson,
  comparisonGraphStoreToJson
} from './modules/comparisonGraphStore'
import {
  detailGraphStoreToJson,
  detailGraphStoreFromJson
} from './modules/detailGraphStore'

interface LocalStoragePersisted {
  userModule?: UserStore
  colorModule?: ColorStore
  repoModule?: string
}

export const persistenceLocalStorage = new VuexPersistence<Partial<RootState>>({
  storage: window.localStorage,
  saveState: (key, rawState, storage) => {
    const state = rawState as RootState

    const persistable: LocalStoragePersisted = {
      userModule: state.userModule,
      colorModule: state.colorModule,
      repoModule: repoStoreToJson(state.repoModule)
    }
    storage!.setItem(key, JSON.stringify(persistable))
  },
  restoreState: (key, storage) => {
    const data = storage!.getItem(key)

    if (data === null || data === undefined) {
      return {}
    }

    const parsed = JSON.parse(data) as LocalStoragePersisted

    return {
      userModule: parsed.userModule,
      colorModule: parsed.colorModule,
      repoModule: repoStoreFromJson(parsed.repoModule)
    }
  }
})

interface SessionStoragePersisted {
  comparisonGraphModule?: string
  detailGraphModule?: string
}

export const persistenceSessionStorage = new VuexPersistence<
  Partial<RootState>
>({
  storage: window.sessionStorage,
  saveState: (key, rawState, storage) => {
    const state = rawState as RootState

    const persistable: SessionStoragePersisted = {
      comparisonGraphModule: comparisonGraphStoreToJson(
        state.comparisonGraphModule
      ),
      detailGraphModule: detailGraphStoreToJson(state.detailGraphModule)
    }

    storage!.setItem(key, JSON.stringify(persistable))
  },
  restoreState: (key, storage) => {
    const data = storage!.getItem(key)
    if (data === null || data === undefined) {
      return {}
    }

    const parsed = JSON.parse(data) as SessionStoragePersisted

    return {
      comparisonGraphModule: comparisonGraphStoreFromJson(
        parsed.comparisonGraphModule
      ),
      detailGraphModule: detailGraphStoreFromJson(parsed.detailGraphModule)
    }
  }
})

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

  let state: RootState = JSON.parse(JSON.parse(data)['vuex'])

  console.log('Used state')
  console.log(state)

  // Detail module
  Object.assign(vxm.detailGraphModule, state.detailGraphModule)

  // Comparison module
  Object.assign(vxm.comparisonGraphModule, state.comparisonGraphModule)
}
