<template>
  <v-app>
    <v-content>
      <color-module-tester></color-module-tester>
      <repo-module-tester></repo-module-tester>
    </v-content>
  </v-app>
</template>

<script lang="ts">
import Vue from 'vue'
import ColorModuleTester from './testComponents/ColorModuleTester.vue'
import RepoModuleTester from './testComponents/RepoModuleTester.vue'

export default Vue.extend({
  name: 'App',

  components: {
    ColorModuleTester,
    RepoModuleTester
  },

  data: () => ({
    //
  })
})
</script>
