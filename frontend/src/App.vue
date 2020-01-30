<template>
  <v-app>
    <v-content>
      <nav-bar></nav-bar>
      <snackbar ref="global-snackbar"></snackbar>
      <router-view />
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
    this.$router.afterEach((from, to) => {
      vxm.repoModule.fetchRepos()
    })
  }
}
</script>

<style>
.v-toolbar > .v-toolbar__content {
  height: auto !important;
  min-height: 64px;
}
.v-toolbar {
  height: auto !important;
  min-height: 64px;
}

.theme--light.v-application a.concealed-link {
  color: rgba(0, 0, 0, 0.87) !important;
}

.v-application .theme--dark a.concealed-link {
  color: #ffffff !important;
}

.concealed-link {
  text-decoration: none;
  cursor: pointer;
}

.concealed-link:hover {
  text-decoration: underline;
}
</style>
