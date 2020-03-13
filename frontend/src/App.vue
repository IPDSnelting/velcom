<template>
  <v-app>
    <v-content>
      <nav-bar></nav-bar>
      <snackbar ref="global-snackbar"></snackbar>
      <router-view />
      <theme-selector @useDarkTheme="setDarkTheme"></theme-selector>
    </v-content>
  </v-app>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import NavigationBar from './components/NavigationBar.vue'
import Snackbar from './components/Snackbar.vue'
import { Store } from 'vuex'
import { vxm, storeToLocalStorage } from './store'
import { Watch } from 'vue-property-decorator'
import ThemeSelector from './components/ThemeSelector.vue'

@Component({
  components: {
    'nav-bar': NavigationBar,
    snackbar: Snackbar,
    'theme-selector': ThemeSelector
  }
})
export default class App extends Vue {
  private clickHandler: any = this.checkClick

  private checkClick(event: Event) {
    if (!event.srcElement) {
      return
    }
    if (!(event.srcElement instanceof HTMLElement)) {
      return
    }

    let tmpElement: HTMLElement | null = event.srcElement
    while (tmpElement && tmpElement.tagName.toLowerCase() !== 'a') {
      tmpElement = tmpElement.parentElement
    }

    if (!tmpElement) {
      return
    }
    storeToLocalStorage()
  }

  private setDarkTheme(darkTheme: boolean) {
    vxm.userModule.darkThemeSelected = darkTheme
  }

  @Watch('isDarkTheme')
  private onDarkThemeChanged() {
    this.$vuetify.theme.dark = this.isDarkTheme
  }

  private get isDarkTheme() {
    return vxm.userModule.darkThemeSelected
  }

  created() {
    this.$vuetify.theme.dark = this.isDarkTheme
    document.addEventListener('click', this.clickHandler)
    document.addEventListener('mousedown', this.clickHandler)

    vxm.repoModule.fetchRepos()
    this.$router.afterEach((from, to) => {
      vxm.repoModule.fetchRepos()
    })
  }

  beforeDestroy() {
    document.removeEventListener('click', this.clickHandler)
    document.removeEventListener('mousedown', this.clickHandler)
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
