<template>
  <v-app>
    <v-content>
      <nav-bar></nav-bar>
      <snackbar ref="global-snackbar"></snackbar>
      <repo-add>
        <template #activator="{ on }">
          <v-btn v-on="on">Test add</v-btn>
        </template>
      </repo-add>
      <repo-update>
        <template #activator="{ on }">
          <v-btn v-on="on">Test update</v-btn>
        </template>
      </repo-update>
      <worker-overview></worker-overview>
      <router-view></router-view>
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
import RepoAddDialog from './components/RepoAddDialog.vue'
import WorkerOverview from './components/WorkerOverview.vue'
import Snackbar from './components/Snackbar.vue'
import { Store } from 'vuex'
import { RootState, Worker } from './store/types'
import RepoUpdateDialog from './components/RepoUpdateDialog.vue'

@Component({
  components: {
    'color-module-tester': ColorModuleTester,
    'repo-module-tester': RepoModuleTester,
    'nav-bar': NavigationBar,
    'repo-add': RepoAddDialog,
    'worker-overview': WorkerOverview,
    snackbar: Snackbar,
    'repo-update': RepoUpdateDialog
  }
})
export default class App extends Vue {
  get store() {
    return this.$store as Store<RootState>
  }

  get workers(): Worker[] {
    return this.store.state.queueModule.workers
  }
}
</script>
