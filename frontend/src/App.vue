<template>
  <v-app>
    <v-content>
      <nav-bar></nav-bar>
      <snackbar ref="global-snackbar"></snackbar>
      <repo-update>
        <template #activator="{ on }">
          <v-btn v-on="on">Test update</v-btn>
        </template>
      </repo-update>
      <router-view></router-view>
    </v-content>
  </v-app>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import NavigationBar from './components/NavigationBar.vue'
import Snackbar from './components/Snackbar.vue'
import { Store } from 'vuex'
import { RootState, Worker } from './store/types'
import RepoUpdateDialog from './components/RepoUpdateDialog.vue'

@Component({
  components: {
    'nav-bar': NavigationBar,
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
