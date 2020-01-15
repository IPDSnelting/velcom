<template>
  <nav>
    <v-toolbar dark color="primary darken-1">
      <v-app-bar-nav-icon></v-app-bar-nav-icon>
      <v-toolbar-title>{{ title }}</v-toolbar-title>

      <v-spacer></v-spacer>

      <!-- Navigation items -->
      <v-btn v-for="item in validRoutes" :key="item.routeName" text :to="{ name: item.routeName }">
        {{ item.label }}
        <v-icon right dark :size="iconFontSize">{{ item.icon }}</v-icon>
      </v-btn>
    </v-toolbar>
  </nav>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { VuetifyIcon } from 'vuetify/types/services/icons'
import { VueRouterEx, RouteConfig } from 'vue-router/types/router'

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

@Component
export default class NavigationBar extends Vue {
  private iconFontSize = 22
  private title = 'VelCom'

  get validRoutes() {
    return this.$router.routes
      .filter(route => route.meta.navigable)
      .map(
        route =>
          new NavigationItem(route.name!, route.meta.icon, route.meta.label)
      )
  }
}
</script>

<style scoped>
</style>
