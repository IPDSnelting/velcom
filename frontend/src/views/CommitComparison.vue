<template>
  <div class="commit-comparison">
    <h1>
      This is the comparison page of the commits {{ firstHash() }} and
      {{ secondHash() }} in repo {{ repoID() }}
    </h1>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'

@Component
export default class CommitComparison extends Vue {
  repoID() {
    return this.$route.params.repoID
  }

  firstHash() {
    return this.$route.params.hashOne
  }

  secondHash() {
    return this.$route.params.hashTwo
  }
}
</script>
