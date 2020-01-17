<template>
  <div class="commit-detail">
    <h1>
      This is the detail page of commit {{ hash() }} in repo {{ repoID() }}
    </h1>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'

@Component
export default class CommitDetail extends Vue {
  repoID() {
    return this.$route.params.repoID
  }

  hash() {
    return this.$route.params.hash
  }
}
</script>
