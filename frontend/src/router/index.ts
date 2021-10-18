import Vue from 'vue'
import VueRouter, { RouteConfig, RouterOptions } from 'vue-router'
import Home from '../views/Home.vue'
import RepoDetailFrame from '../views/RepoDetailFrame.vue'
import Queue from '../views/Queue.vue'
import NotFound404 from '../views/NotFound404.vue'
import RunCommitDetailView from '../views/RunCommitDetailView.vue'
import RunComparison from '../views/RunComparison.vue'
import {
  mdiHome,
  mdiScaleBalance,
  mdiSourceBranch,
  mdiInformationOutline,
  mdiCircleSlice6
} from '@mdi/js'
import { vxm } from '@/store'
import TaskDetailView from '@/views/TaskDetailView.vue'
import Search from '@/views/Search.vue'
import Comparison from '@/views/RepoComparison.vue'

Vue.use(VueRouter)

export type RouteName =
  | 'home'
  | 'repo-comparison'
  | 'repo-detail'
  | 'queue'
  | 'search'
  | 'run-comparison'
  | 'run-detail'
  | 'task-detail'
  | 'about'
  | '404'

type RouteInfo = {
  name?: RouteName
  meta: {
    navigable?: boolean
    label?: string
    icon?: string
  }
} & RouteConfig

const routes: RouteInfo[] = [
  {
    // Redirect / to /home
    path: '/',
    redirect: '/home',
    meta: {
      navigable: false,
      label: 'Home'
    }
  },
  {
    path: '/home',
    name: 'home',
    component: Home,
    meta: {
      label: 'Home',
      navigable: true,
      icon: mdiHome
    }
  },
  {
    path: '/repo-comparison',
    name: 'repo-comparison',
    component: Comparison,
    meta: {
      label: 'Comparison',
      navigable: true,
      icon: mdiScaleBalance
    }
  },
  {
    path: '/repo-detail/:id?',
    name: 'repo-detail',
    component: RepoDetailFrame,
    meta: {
      label: 'Repo Detail',
      navigable: true,
      icon: mdiSourceBranch
    }
  },
  {
    path: '/queue',
    name: 'queue',
    component: Queue,
    meta: {
      label: 'Queue',
      navigable: true,
      icon: mdiCircleSlice6
    }
  },
  {
    path: '/compare/:first/to/:second',
    name: 'run-comparison',
    component: RunComparison,
    meta: {
      navigable: false,
      label: 'Run Comparison'
    }
  },
  {
    path: '/search/:runId?',
    name: 'search',
    component: Search,
    meta: {
      navigable: false,
      label: 'Voogle - Search'
    }
  },
  {
    path: '/run-detail/:first/:second?',
    name: 'run-detail',
    component: RunCommitDetailView,
    meta: {
      navigable: false,
      label: 'Detail'
    }
  },
  {
    path: '/task-detail/:taskId',
    name: 'task-detail',
    component: TaskDetailView,
    meta: {
      navigable: false,
      label: 'Task-Detail'
    }
  },
  {
    path: '/about',
    name: 'about',
    component: () =>
      import(/* webpackChunkName: "about" */ '../views/About.vue'),
    meta: {
      label: 'About',
      navigable: true,
      icon: mdiInformationOutline
    }
  },
  {
    path: '*',
    name: '404',
    component: NotFound404,
    meta: {
      label: 'Not found',
      navigable: false
    }
  }
]

class VueRouterEx extends VueRouter {
  public routes: RouteConfig[] = []

  constructor(options: RouterOptions) {
    super(options)
    const { routes } = options

    this.routes = routes!
  }

  addRoutes(routes: RouteConfig[]) {
    super.addRoutes(routes)
    // Track route additions
    this.routes.push(...routes)
  }
}

Vue.use(VueRouterEx)

export let routerBaseUrl = ''

if (process.env.VUE_APP_LOCATION_INDEPENDENT_IMAGE) {
  routerBaseUrl = computeRuntimeBaseUrl()
} else {
  routerBaseUrl = process.env.BASE_URL
}

const router = new VueRouterEx({
  mode: 'history',
  base: routerBaseUrl,
  routes: routes
})

router.afterEach(to => {
  Vue.nextTick(() => {
    document.title = to.meta!.label ? 'VelCom - ' + to.meta!.label : 'VelCom'
  })
})

router.beforeEach((to, from, next) => {
  if (to.name === 'repo-detail' && to.params.id) {
    vxm.detailGraphModule.selectedRepoId = to.params.id
  }
  next()
})

router.beforeEach((to, from, next) => {
  if (to.name === 'repo-detail' && !to.params['id']) {
    const saved = vxm.detailGraphModule.selectedRepoId
    if (saved) {
      next({ name: 'repo-detail', params: { id: saved } })
      return
    }
  }
  next()
})

export default router

function computeRuntimeBaseUrl() {
  let path = window.location.pathname

  const routeFirstPathComponents = routes
    .map(it => it.path)
    .filter(it => it.length > 1 && it.startsWith('/'))
    .map(it => it.replace(/\/(.+?)\/.*/, '$1'))

  for (const component of routeFirstPathComponents) {
    path = path.replace(new RegExp(component + '(/.*)?', 'g'), '')
  }

  return path
}
