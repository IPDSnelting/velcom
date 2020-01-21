<template>
  <v-app>
    <v-content>
      <nav-bar></nav-bar>
      <snackbar ref="global-snackbar"></snackbar>
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
import { vxm } from './store'

@Component({
  components: {
    'nav-bar': NavigationBar,
    snackbar: Snackbar
  }
})
export default class App extends Vue {
  created() {
    vxm.repoModule.fetchRepos()
  }
}
</script>
