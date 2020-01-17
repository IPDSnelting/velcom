<template>
  <nav>
    <v-toolbar dark color="primary darken-1">
      <v-app-bar-nav-icon class="hidden-md-and-up" @click="drawerShown = !drawerShown"></v-app-bar-nav-icon>
      <v-toolbar-title>{{ title }}</v-toolbar-title>

      <v-spacer></v-spacer>

      <!-- Navigation items -->
      <v-btn
        class="hidden-sm-and-down"
        v-for="item in validRoutes"
        :key="item.routeName"
        text
        :to="{ name: item.routeName }"
      >
        {{ item.label }}
        <v-icon right dark :size="iconFontSize">{{ item.icon }}</v-icon>
      </v-btn>

      <login>
        <template #activator="{ on }">
          <v-btn v-on="on" text>
            Login
            <v-icon right dark :size="iconFontSize">{{ loginIcon }}</v-icon>
          </v-btn>
        </template>
      </login>
    </v-toolbar>

    <!-- Navigation drawer -->
    <v-navigation-drawer class="hidden-md-and-up" v-model="drawerShown" app temporary>
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
import VueRouterEx, { RouteConfig } from 'vue-router/types/router'
import router from '../router'
import LoginDialog from '../components/LoginDialog.vue'
import { mdiAccountCircleOutline } from '@mdi/js'

class NavigationItem {
  readonly routeName: String
  readonly icon: VuetifyIcon
  readonly label: String

  constructor(routeName: String, icon: VuetifyIcon, label: String) {
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

  private drawerShown = false

  get validRoutes() {
    return router.routes
      .filter(route => route.meta.navigable)
      .map(
        route =>
          new NavigationItem(route.name!, route.meta.icon, route.meta.label)
      )
  }

  // ============== ICONS ==============
  private loginIcon = mdiAccountCircleOutline
  // ==============       ==============
}
</script>

<style scoped></style>
