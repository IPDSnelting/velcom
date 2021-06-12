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
import { vxm } from '@/store'

@Component({
  components: { StatusComparison, RepoComparison }
})
export default class Comparison extends Vue {
  private tabMapping: Array<'timeline' | 'status'> = ['timeline', 'status']

  private set selectedTab(tabIndex: number) {
    vxm.statusComparisonModule.selectedTab = this.tabMapping[tabIndex]
  }

  private get selectedTab(): number {
    return this.tabMapping.indexOf(vxm.statusComparisonModule.selectedTab)
  }

  private get height() {
    // Full height
    //   - Navigation bar
    //   -  8px container padding
    //   - 25px Tabs
    //   -  8px tab margin
    //   -  4px lift it up a bit
    return 'calc(100vh - 64px - 8px - 25px - 8px - 4px)'
  }

  private async mounted() {
    // They will be fetched on page load anyways, but we *need* to make sure they are already loaded!
    // Otherwise we might not find our selected dimension
    if (vxm.repoModule.allRepos.length === 0) {
      await vxm.repoModule.fetchRepos()
    }

    // No query, so do not adjust to anything
    if (Object.keys(this.$route.query).length === 0) {
      return
    }

    if (!this.$route.query.type || this.$route.query.type === 'timeline') {
      await vxm.comparisonGraphModule.adjustToPermanentLink(this.$route)
      this.selectedTab = this.tabMapping.indexOf('timeline')
    } else {
      await vxm.statusComparisonModule.adjustToPermanentLink(this.$route)
      this.selectedTab = this.tabMapping.indexOf('status')
    }
  }
}
</script>

<style>
.v-window-item {
  height: 100%;
}
</style>
