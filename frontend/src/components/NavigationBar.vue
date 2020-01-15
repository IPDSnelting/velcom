<template>
  <nav>
    <v-toolbar dark color="primary darken-1">
      <v-app-bar-nav-icon v-if="!actionsHidden" @click="drawer = !drawer"></v-app-bar-nav-icon>
      <v-toolbar-title>{{ title }}</v-toolbar-title>

      <v-spacer></v-spacer>

      <!-- Navigation items -->
      <v-btn
        v-for="item in navigationItems"
        :key="item.routeName"
        text
        :to="{ name: item.routeName }"
      >
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
import {
  mdiInformationOutline,
  mdiHome,
  mdiScaleBalance,
  mdiSourceBranch
} from '@mdi/js'

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
  private navigationItems: NavigationItem[] = [
    new NavigationItem('home', mdiHome, 'Home'),
    new NavigationItem('repo-comparison', mdiScaleBalance, 'Repo Comparison'),
    new NavigationItem('repo-detail', mdiSourceBranch, 'Repo Detail'),
    new NavigationItem('about', mdiInformationOutline, 'About')
  ]
  private iconFontSize = 22
}
</script>

<style scoped>
</style>
