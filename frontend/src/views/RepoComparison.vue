<template>
  <div class="repo-comparison">
    <h1>This is the repo comparison page</h1>
    <repo-add>
      <template #activator="{ on }">
        <v-btn v-on="on">Test</v-btn>
      </template>
    </repo-add>
    <repo-module-tester></repo-module-tester>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import RepoAddDialog from '../components/RepoAddDialog.vue'
import RepoModuleTester from '../testComponents/RepoModuleTester.vue'

@Component({
  components: {
    'repo-add': RepoAddDialog,
    'repo-module-tester': RepoModuleTester
  }
})
export default class RepoComparison extends Vue {}
</script>
