<template>
  <v-container fluid class="pb-0">
    <v-tabs v-model="selectedTab" height="25" centered class="mb-2">
      <v-tab>Over Time</v-tab>
      <v-tab>Status Comparison</v-tab>
    </v-tabs>

    <v-tabs-items v-model="selectedTab">
      <v-tab-item>
        <repo-comparison></repo-comparison>
      </v-tab-item>
      <v-tab-item>
        <status-comparison :suggested-height="height"></status-comparison>
      </v-tab-item>
    </v-tabs-items>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import RepoComparison from '@/views/RepoComparison.vue'
import StatusComparison from '@/views/StatusComparison.vue'

@Component({
  components: { StatusComparison, RepoComparison }
})
export default class Comparison extends Vue {
  private selectedTab = 0

  private get height() {
    // Full height
    //   - Navigation bar
    //   -  8px container padding
    //   - 25px Tabs
    //   -  8px tab margin
    //   -  4px lift it up a bit
    return 'calc(100vh - 64px - 8px - 25px - 8px - 4px)'
  }
}
</script>

<style>
.v-window-item {
  height: 100%;
}
</style>
