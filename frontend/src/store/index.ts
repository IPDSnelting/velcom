import Vue from 'vue'
import Vuex, { StoreOptions } from 'vuex'
import { RootState } from './types'
import { colorModule } from './modules/ColorModule'
import { commitComparisonModule } from './modules/CommitComparisonModule'
import { newsModule } from './modules/NewsModule'
import { queueModule } from './modules/QueueModule'
import { repoComparisonModule } from './modules/RepoComparisonModule'
import { repoDetailModule } from './modules/RepoDetailModule'
import { repoModule } from './modules/RepoModule'
import { userModule } from './modules/UserModule'

Vue.use(Vuex)

const storeOptions: StoreOptions<RootState> = {
  state: {
    apiBaseURL: 'https://aaaaaaah.de:8667'
  } as RootState,
  mutations: {},
  actions: {},
  modules: {
    colorModule,
    commitComparisonModule,
    newsModule,
    queueModule,
    repoComparisonModule,
    repoDetailModule,
    repoModule,
    userModule
  }

}

export default new Vuex.Store(storeOptions)
