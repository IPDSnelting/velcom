<template>
  <div class="repo-detail-frame">
    <h1>This is the repo detail frame</h1>
    <v-btn :to="repoRoute">navigate to detail page</v-btn>
    <router-view></router-view>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'

@Component
export default class RepoDetailFrame extends Vue {
  private repoRoute = '/repo-detail/0'
}
</script>
