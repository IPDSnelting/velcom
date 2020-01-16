<template>
  <div class="repo-detail">
    <h1>This is the detail page of repo {{ id() }}</h1>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import { Repo } from '@/store/types'
import Component from 'vue-class-component'

@Component
export default class RepoComparison extends Vue {
  id() {
    return this.$route.params.id
  }
}
</script>
