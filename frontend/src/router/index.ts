import Vue from 'vue'
import VueRouter, { RouteConfig, RouterOptions } from 'vue-router'
import Home from '../views/Home.vue'
import RepoComparison from '../views/RepoComparison.vue'
import RepoDetailFrame from '../views/RepoDetailFrame.vue'
import RepoDetail from '../views/NewRepoDetail.vue'
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

Vue.use(VueRouter)

export type RouteName =
  | 'home'
  | 'repo-comparison'
  | 'repo-detail-frame'
  | 'repo-detail'
  | 'queue'
  | 'run-comparison'
  | 'run-detail'
  | 'about'
  | '404'

type RouteInfo = Partial<{
  name: RouteName
  meta: Partial<{
    navigable: boolean
    label: string
    icon: string
  }>
}> &
  RouteConfig

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
    component: RepoComparison,
    meta: {
      label: 'Repo Comparison',
      navigable: true,
      icon: mdiScaleBalance
    }
  },
  {
    path: '/repo-detail',
    name: 'repo-detail-frame',
    component: RepoDetailFrame,
    children: [
      {
        path: ':id',
        name: 'repo-detail',
        component: RepoDetail
      }
    ],
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
    path: '/run-detail/:first/:second?',
    name: 'run-detail',
    component: RunCommitDetailView,
    meta: {
      navigable: false,
      label: 'Detail'
    }
  },
  {
    path: '/about',
    name: 'about',
    // route level code-splitting
    // this generates a separate chunk (about.[hash].js) for this route
    // which is lazy-loaded when the route is visited.
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
  matcher: any

  public routes: RouteConfig[] = []

  constructor(options: RouterOptions) {
    super(options)
    const { addRoutes } = this.matcher
    const { routes } = options

    this.routes = routes!

    this.matcher.addRoutes = (newRoutes: RouteConfig[]) => {
      this.routes.push(...newRoutes)
      addRoutes(newRoutes)
    }
  }
}

Vue.use(VueRouterEx)

const router = new VueRouterEx({
  mode: 'history',
  base: process.env.BASE_URL,
  routes: routes
})

router.afterEach(to => {
  Vue.nextTick(() => {
    document.title = to.meta.label ? 'VelCom - ' + to.meta.label : 'VelCom'
  })
})

router.beforeEach((to, from, next) => {
  if (to.name === 'repo-detail') {
    vxm.detailGraphModule.selectedRepoId = to.params.id
  }
  next()
})

router.beforeEach((to, from, next) => {
  if (to.name === 'repo-detail-frame' && !to.params['id']) {
    const saved = vxm.detailGraphModule.selectedRepoId
    if (saved) {
      next({ name: 'repo-detail', params: { id: saved } })
      return
    }
  }
  next()
})

export default router
