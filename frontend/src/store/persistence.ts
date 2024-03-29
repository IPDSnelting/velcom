import { UserStore } from './modules/userStore'
import { ColorStore } from './modules/colorStore'
import { RootState, vxm } from './index'
import VuexPersistence from 'vuex-persist'
import { fromJson, toJson } from '@/util/Serialisation'
import { RepoStore } from '@/store/modules/repoStore'
import { DetailGraphStore } from '@/store/modules/detailGraphStore'
import { ComparisonGraphStore } from '@/store/modules/comparisonGraphStore'
import { StatusComparisonStore } from '@/store/modules/statusComparisonStore'

const STORAGE_VERSION_KEY = 'VELCOM_STORAGE_VERSION'
const STORAGE_VERSION_CURRENT = '3'

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

export const persistenceLocalStorage = new VuexPersistence<Partial<RootState>>({
  storage: window.localStorage,
  saveState: (key, rawState, storage) => {
    const state = rawState as RootState

    const persistable = {
      userModule: UserStore.toPlainObject(state.userModule),
      colorModule: ColorStore.toPlainObject(state.colorModule),
      repoModule: RepoStore.toPlainObject(state.repoModule)
    }

    try {
      storage!.setItem(key, toJson(persistable))
      storage!.setItem(STORAGE_VERSION_KEY, STORAGE_VERSION_CURRENT)
    } catch (e) {
      // eslint-disable-next-line no-console
      console.warn('Unable to save session data', e)
      // Ignore the store on error. This is nicer than displaying a white
      // page and rendering VelCom unusable.
    }
  },
  restoreState: (key, storage) => {
    const data = storage!.getItem(key)

    if (data === null || data === undefined) {
      return {}
    }

    try {
      return fromJson(data)
    } catch (e) {
      // eslint-disable-next-line no-console
      console.warn('Unable to restore local data', e)
      // Discard local state on error. This is nicer than displaying a white
      // page and rendering VelCom unusable.
      return {}
    }
  }
})

export const persistenceSessionStorage = new VuexPersistence<
  Partial<RootState>
>({
  storage: window.sessionStorage,
  saveState: (key, rawState, storage) => {
    const state = rawState as RootState

    const persistable = {
      detailGraphModule: DetailGraphStore.toPlainObject(
        state.detailGraphModule
      ),
      comparisonGraphModule: ComparisonGraphStore.toPlainObject(
        state.comparisonGraphModule
      ),
      statusComparisonModule: StatusComparisonStore.toPlainObject(
        state.statusComparisonModule
      )
    }

    try {
      storage!.setItem(key, toJson(persistable))
      storage!.setItem(STORAGE_VERSION_KEY, STORAGE_VERSION_CURRENT)
    } catch (e) {
      // eslint-disable-next-line no-console
      console.warn('Unable to save session data', e)
      // Ignore the store on error. This is nicer than displaying a white
      // page and rendering VelCom unusable.
    }
  },
  restoreState: (key, storage) => {
    const data = storage!.getItem(key)
    if (data === null || data === undefined) {
      return {}
    }

    try {
      return fromJson(data)
    } catch (e) {
      // eslint-disable-next-line no-console
      console.warn('Unable to restore session data', e)
      // Discard local state on error. This is nicer than displaying a white
      // page and rendering VelCom unusable.
      return {}
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
  // eslint-disable-next-line no-console
  console.info('Trying to restore tab state')

  const rawData = localStorage.getItem('persisted_session_state')

  if (rawData === null) {
    // eslint-disable-next-line no-console
    console.info("Couldn't find any past state. Assuming I am a new tab")
    return
  }

  const { data, accessTime } = JSON.parse(rawData)

  // Older than 10 seconds. Should not happen, but better be safe than sorry.
  if (new Date().getTime() - accessTime > 10 * 1000) {
    // eslint-disable-next-line no-console
    console.info('Found state is too old: ' + accessTime)
    return
  }

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

  // eslint-disable-next-line no-console
  console.info('Restored from saved tab state')

  // Detail module
  Object.assign(vxm.detailGraphModule, state.detailGraphModule)

  // Comparison module
  Object.assign(vxm.comparisonGraphModule, state.comparisonGraphModule)

  sessionStorage.removeItem('persisted_session_state_unwrapped')
}
