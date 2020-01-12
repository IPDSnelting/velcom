import Vue from 'vue'
import Vuex, { StoreOptions } from 'vuex'
import { RootState } from './types'
import { colorModule } from './modules/colorModule'
import { commitComparisonModule } from './modules/commitComparisonModule'
import { newsModule } from './modules/newsModule'
import { queueModule } from './modules/queueModule'
import { repoComparisonModule } from './modules/repoComparisonModule'
import { repoDetailModule } from './modules/repoDetailModule'
import { repoModule } from './modules/repoModule'
import { userModule } from './modules/userModule'

Vue.use(Vuex)

const storeOptions: StoreOptions<RootState> = {
  state: {
    apiBaseURL: 'https://aaaaaaah.de:8667'
  } as RootState,
  mutations: {},
  actions: {
    hello () {
      console.log('hello from base')
    }
  },
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
