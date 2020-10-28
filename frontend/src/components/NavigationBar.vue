<template>
  <nav>
    <v-toolbar dark color="primary darken-1">
      <v-app-bar-nav-icon
        class="hidden-md-and-up"
        @click="drawerShown = !drawerShown"
      ></v-app-bar-nav-icon>
      <v-tooltip bottom color="rgba(0,0,0,0)" class="logoTooltip">
        <template #activator="{ on }">
          <img
            v-on="on"
            @click="$emit('navigate', 'home')"
            width="45px"
            height="45px"
            src="@/assets/mini-logo.png"
            alt="logo"
            class="mx-4"
          />
        </template>
        <img src="@/assets/mini-logo.png" alt="logo" class="mx-4" id="logo" />
      </v-tooltip>
      <v-toolbar-title>{{ title }}</v-toolbar-title>

      <v-spacer></v-spacer>

      <!-- Navigation items -->
      <v-btn
        class="hidden-sm-and-down"
        v-for="item in validRoutes"
        :key="item.routeName"
        text
        @click="$emit('navigate', item.routeName)"
      >
        {{ item.label }}
        <v-icon right dark :size="iconFontSize">{{ item.icon }}</v-icon>
      </v-btn>

      <login v-if="!loggedIn">
        <template #activator="{ on }">
          <v-btn v-on="on" text>
            Login
            <v-icon right dark :size="iconFontSize">{{ loginIcon }}</v-icon>
          </v-btn>
        </template>
      </login>
      <v-btn v-if="loggedIn" text @click="logout">
        Logout
        <v-icon right dark :size="iconFontSize">{{ logoutIcon }}</v-icon>
      </v-btn>
    </v-toolbar>

    <!-- Navigation drawer -->
    <v-navigation-drawer
      class="hidden-md-and-up"
      v-model="drawerShown"
      app
      temporary
    >
      <v-toolbar dark color="primary darken-1">
        <v-list>
          <v-list-item>
            <v-list-item-title class="title">Navigation</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-toolbar>

      <v-divider></v-divider>

      <v-list dense class="pt-0">
        <v-list-item
          v-for="item in validRoutes"
          :key="item.routeName"
          :to="{ name: item.routeName }"
        >
          <v-list-item-icon>
            <v-icon>{{ item.icon }}</v-icon>
          </v-list-item-icon>

          <v-list-item-content>
            <v-list-item-title>{{ item.label }}</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
      </v-list>
    </v-navigation-drawer>
  </nav>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { VuetifyIcon } from 'vuetify/types/services/icons'
import VueRouterEx from 'vue-router/types/router'
import router from '../router'
import LoginDialog from '../components/dialogs/LoginDialog.vue'
import { mdiAccountCircleOutline, mdiLogout } from '@mdi/js'
import { vxm } from '@/store'

class NavigationItem {
  readonly routeName: string
  readonly icon: VuetifyIcon
  readonly label: string

  constructor(routeName: string, icon: VuetifyIcon, label: string) {
    this.routeName = routeName
    this.icon = icon
    this.label = label
  }
}

@Component({
  components: {
    login: LoginDialog
  }
})
export default class NavigationBar extends Vue {
  private iconFontSize = 22
  private title = 'VelCom'
  private homeKey: number = 0

  private drawerShown = false

  get validRoutes(): NavigationItem[] {
    return router.routes
      .filter(this.filterRoute)
      .map(
        route =>
          new NavigationItem(route.name!, route.meta.icon, route.meta.label)
      )
  }

  private filterRoute(route: VueRouterEx.RouteConfig): boolean {
    if (
      vxm.repoModule.allRepos.length <= 1 &&
      route.name === 'repo-comparison'
    ) {
      return false
    }
    return route.meta.navigable
  }

  get loggedIn(): boolean {
    return vxm.userModule.loggedIn
  }

  logout(): void {
    vxm.userModule.logOut()
  }

  // ============== ICONS ==============
  private loginIcon = mdiAccountCircleOutline
  private logoutIcon = mdiLogout
  // ==============       ==============
}
</script>

<style>
/*noinspection CssUnusedSymbol*/
.v-tooltip__content {
  opacity: 1 !important;
}
</style>
