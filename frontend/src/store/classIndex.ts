import Vuex from 'vuex'
import { extractVuexModule, createProxy } from 'vuex-class-component'
import Vue from 'vue'
import { RepoStore } from '../store/modules/repoModule/repoModuleClass'
import { UserStore } from '../store/modules/userModule/userModuleClass'
import { ColorModuleStore } from './modules/colorModule/colorModuleClass'
import { CommitComparisonModuleStore } from './modules/commitComparisonModule/commitComparisonModuleClass'
import { NewsModuleStore } from './modules/newsModule/newsModuleClass'
import { QueueModuleStore } from './modules/queueModule/queueModuleClass'
import { RepoComparisonStore } from './modules/repoComparisonModule/repoComparisonModuleClass'
import { RepoDetailStore } from './modules/repoDetailModule/repoDetailModuleClass'

Vue.use(Vuex)

export const store = new Vuex.Store({
  modules: {
    ...extractVuexModule(ColorModuleStore),
    ...extractVuexModule(CommitComparisonModuleStore),
    ...extractVuexModule(NewsModuleStore),
    ...extractVuexModule(QueueModuleStore),
    ...extractVuexModule(RepoComparisonStore),
    ...extractVuexModule(RepoDetailStore),
    ...extractVuexModule(RepoStore),
    ...extractVuexModule(UserStore)
  }
})

export const vxm = {
  colorModule: createProxy(store, ColorModuleStore),
  commitComparisonModule: createProxy(store, CommitComparisonModuleStore),
  newsModule: createProxy(store, NewsModuleStore),
  queueModule: createProxy(store, QueueModuleStore),
  repoComparisonModule: createProxy(store, RepoComparisonStore),
  repoModule: createProxy(store, RepoStore),
  userModule: createProxy(store, UserStore)
}
