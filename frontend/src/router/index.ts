import Vue from 'vue'
import VueRouter, { RouteConfig, RouterOptions } from 'vue-router'
import Home from '../views/Home.vue'
import About from '../views/About.vue'
import {
  mdiHome,
  mdiScaleBalance,
  mdiSourceBranch,
  mdiInformationOutline
} from '@mdi/js'

Vue.use(VueRouter)

const routes = [
  {
    // Redirect / to /home
    redirect: '/home',
    path: '/',
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
    component: About,
    meta: {
      label: 'Repo Comparison',
      navigable: true,
      icon: mdiScaleBalance
    }
  },
  {
    path: '/repo-detail',
    name: 'repo-detail',
    component: About,
    meta: {
      label: 'Repo Detail',
      navigable: true,
      icon: mdiSourceBranch
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

export default router
