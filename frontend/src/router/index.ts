import Vue from 'vue'
import VueRouter from 'vue-router'
import Home from '../views/Home.vue'
import About from '../views/About.vue'

Vue.use(VueRouter)

const routes = [
  {
    // Redirect / to /home
    redirect: '/home',
    path: '/',
    name: 'home',
    component: Home
  },
  {
    path: '/repo-comparison',
    name: 'repo-comparison',
    component: About
  },
  {
    path: '/repo-detail',
    name: 'repo-detail',
    component: About
  },
  {
    path: '/about',
    name: 'about',
    // route level code-splitting
    // this generates a separate chunk (about.[hash].js) for this route
    // which is lazy-loaded when the route is visited.
    component: () =>
      import(/* webpackChunkName: "about" */ '../views/About.vue')
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

export default router
