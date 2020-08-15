import Vue from 'vue'
import VueRouter, { RouteConfig, RouterOptions } from 'vue-router'
import Home from '../views/Home.vue'
import RepoComparison from '../views/RepoComparison.vue'
import RepoDetailFrame from '../views/RepoDetailFrame.vue'
import RepoDetail from '../views/RepoDetail.vue'
import Queue from '../views/Queue.vue'
import NotFound404 from '../views/NotFound404.vue'
import CommitComparisonDisplay from '../views/CommitComparisonDisplay.vue'
import CommitDetail from '../views/CommitDetail.vue'
import RunDetail from '../components/rundetail/RunDetail.vue'
import NewCommitDetail from '../components/rundetail/CommitDetail.vue'
import {
  mdiHome,
  mdiScaleBalance,
  mdiSourceBranch,
  mdiInformationOutline,
  mdiCircleSlice6
} from '@mdi/js'
import { vxm } from '@/store'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    redirect: '/home',
    meta: {
      navigable: false,
      label: 'Home'
    }
  },
  {
    // Redirect / to /home
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
    path: '/commit-comparison/:repoID/:hashOne/:hashTwo',
    name: 'commit-comparison',
    component: CommitComparisonDisplay,
    meta: {
      navigable: false,
      label: 'Commit Comparison'
    }
  },
  {
    path: '/commit-detail/:repoID/:hash',
    name: 'commit-detail',
    component: CommitDetail,
    meta: {
      navigable: false,
      label: 'Commit Detail'
    }
  },
  {
    path: '/run-detail',
    name: 'run-detail',
    component: RunDetail,
    meta: {
      navigable: true,
      label: 'Run Detail'
    }
  },
  {
    path: '/new-commit-detail',
    name: 'new-commit-detail',
    component: NewCommitDetail,
    meta: {
      navigable: true,
      label: 'Commit Detail'
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
  routes
})

router.afterEach((to, from) => {
  Vue.nextTick(() => {
    document.title = to.meta.label ? 'VelCom - ' + to.meta.label : 'VelCom'
  })
})

router.beforeEach((to, from, next) => {
  if (to.name === 'repo-detail') {
    vxm.repoDetailModule.selectedRepoId = to.params.id
  }
  next()
})

router.beforeEach((to, from, next) => {
  if (to.name === 'repo-detail-frame' && !to.params['id']) {
    let saved = vxm.repoDetailModule.selectedRepoId
    if (saved) {
      next({ name: 'repo-detail', params: { id: saved } })
      return
    }
  }
  next()
})

export default router
