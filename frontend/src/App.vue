<template>
  <v-app>
    <v-content>
      <nav-bar></nav-bar>
      <color-module-tester></color-module-tester>
      <repo-module-tester></repo-module-tester>
    </v-content>
  </v-app>
</template>

<script lang="ts">
import Vue from 'vue'
import ColorModuleTester from './testComponents/ColorModuleTester.vue'
import RepoModuleTester from './testComponents/RepoModuleTester.vue'
import Component from 'vue-class-component'
import NavigationBar from './components/NavigationBar.vue'

@Component({
  components: {
    'color-module-tester': ColorModuleTester,
    'repo-module-tester': RepoModuleTester,
    'nav-bar': NavigationBar
  }
})
export default class App extends Vue {}
</script>
