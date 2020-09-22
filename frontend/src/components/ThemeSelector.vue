<template>
  <footer>
    <v-toolbar bottom dense class="hidden-md-and-up">
      <v-spacer></v-spacer>
      <v-btn text @click="toggleDarkTheme">
        <v-icon left>{{ darkThemeIcon }}</v-icon>
        Use {{ isDarkTheme ? 'light' : 'dark' }} theme
      </v-btn>
    </v-toolbar>
    <v-tooltip left>
      <template #activator="{ on }">
        <v-btn
          v-on="on"
          icon
          @click="toggleDarkTheme"
          class="hidden-sm-and-down floater"
        >
          <v-icon :class="{ icon: true, 'light-icon': !isDarkTheme }">
            {{ darkThemeIcon }}
          </v-icon>
        </v-btn>
      </template>
      Use {{ isDarkTheme ? 'light' : 'dark' }} theme
    </v-tooltip>
  </footer>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { mdiInvertColors } from '@mdi/js'
import { vxm } from '@/store'

@Component
export default class ThemeSelector extends Vue {
  private toggleDarkTheme() {
    this.$emit('useDarkTheme', !this.isDarkTheme)
  }

  private get isDarkTheme(): boolean {
    return vxm.userModule.darkThemeSelected
  }

  // ICONS
  private darkThemeIcon = mdiInvertColors
}
</script>

<style scoped>
.floater {
  position: fixed;
  bottom: 16px;
  right: 16px;
}

.icon {
  transition: transform 0.5s linear;
}

.light-icon {
  transform: matrix(-1, 0, 0, 1, 0, 0);
}
</style>
