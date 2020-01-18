import Vuex from 'vuex'
import { extractVuexModule, createProxy } from 'vuex-class-component'
import Vue from 'vue'
import { RepoStore } from '../store/modules/repoModule/repoModuleClass'
import { UserStore } from '../store/modules/userModule/userModuleClass'
<<<<<<< HEAD
import { ColorStore } from './modules/colorModule/colorModuleClass'
import { CommitComparisonStore } from './modules/commitComparisonModule/commitComparisonModuleClass'
import { NewsStore } from './modules/newsModule/newsModuleClass'
import { QueueStore } from './modules/queueModule/queueModuleClass'
import { RepoComparisonStore } from './modules/repoComparisonModule/repoComparisonModuleClass'
import { RepoDetailStore } from './modules/repoDetailModule/repoDetailModuleClass'
=======
import { ColorStore } from '../store/modules/colorModule/colorModuleClass'
>>>>>>> repoSelector component as card

Vue.use(Vuex)

export const store = new Vuex.Store({
  modules: {
    ...extractVuexModule(ColorStore),
    ...extractVuexModule(CommitComparisonStore),
    ...extractVuexModule(NewsStore),
    ...extractVuexModule(QueueStore),
    ...extractVuexModule(RepoComparisonStore),
    ...extractVuexModule(RepoDetailStore),
    ...extractVuexModule(RepoStore),
    ...extractVuexModule(UserStore)
  }
})

export const vxm = {
  colorModule: createProxy(store, ColorStore),
  commitComparisonModule: createProxy(store, CommitComparisonStore),
  newsModule: createProxy(store, NewsStore),
  queueModule: createProxy(store, QueueStore),
  repoComparisonModule: createProxy(store, RepoComparisonStore),
  repoModule: createProxy(store, RepoStore),
  userModule: createProxy(store, UserStore)
}
