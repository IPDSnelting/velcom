import { UserStore } from './modules/userStore'
import { ColorStore } from './modules/colorStore'
import { RootState, vxm } from './index'
import VuexPersistence from 'vuex-persist'
import {
  detailGraphStoreFromJson,
  detailGraphStoreToJson,
  repoStoreFromJson,
  repoStoreToJson
} from '@/util/StorePersistenceUtilities'

const STORAGE_VERSION_KEY = 'VELCOM_STORAGE_VERSION'
const STORAGE_VERSION_CURRENT = '1'

/**
 * Deletes old stored data which does not conform to what the store expects to
 * deserialize now.
 */
export function deletedOutdatedLocalData(): void {
  const storedVersion = window.localStorage.getItem(STORAGE_VERSION_KEY)
  if (!storedVersion || storedVersion !== STORAGE_VERSION_CURRENT) {
    window.localStorage.removeItem('vuex')
    window.localStorage.removeItem('vuex-persist')
    window.sessionStorage.removeItem('vuex')
    window.sessionStorage.removeItem('vuex-persist')
  }
}

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

    storage!.setItem(STORAGE_VERSION_KEY, STORAGE_VERSION_CURRENT)
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
      detailGraphModule: detailGraphStoreToJson(state.detailGraphModule)
    }

    storage!.setItem(key, JSON.stringify(persistable))
    storage!.setItem(STORAGE_VERSION_KEY, STORAGE_VERSION_CURRENT)
  },
  restoreState: (key, storage) => {
    const data = storage!.getItem(key)
    if (data === null || data === undefined) {
      return {}
    }

    const parsed = JSON.parse(data) as SessionStoragePersisted

    return {
      detailGraphModule: detailGraphStoreFromJson(parsed.detailGraphModule)
    }
  }
})

export function storeToLocalStorage(): void {
  const data = {
    data: sessionStorage.getItem('vuex'),
    accessTime: new Date().getTime()
  }
  localStorage.setItem('persisted_session_state', JSON.stringify(data))
}

export async function restoreFromPassedSession(): Promise<void> {
  console.log('Trying to restore')

  const rawData = localStorage.getItem('persisted_session_state')

  if (rawData === null) {
    console.log('Raw data really null')
    return
  }

  const { data, accessTime } = JSON.parse(rawData)

  // Older than 10 seconds. Should not happen, but better be safe than sorry.
  if (new Date().getTime() - accessTime > 10 * 1000) {
    console.log('Is too old: ' + accessTime)
    return
  }

  console.log('Used data: ' + data)

  sessionStorage.setItem('persisted_session_state_unwrapped', data)

  const stateWrapped = persistenceSessionStorage.restoreState(
    'persisted_session_state_unwrapped',
    sessionStorage
  )

  let state: Partial<RootState>
  if (stateWrapped instanceof Promise) {
    state = await stateWrapped
  } else {
    state = stateWrapped
  }

  console.log('Used state')
  console.log(state)

  // Detail module
  Object.assign(vxm.detailGraphModule, state.detailGraphModule)

  // Comparison module
  Object.assign(vxm.comparisonGraphModule, state.comparisonGraphModule)

  sessionStorage.removeItem('persisted_session_state_unwrapped')
}
