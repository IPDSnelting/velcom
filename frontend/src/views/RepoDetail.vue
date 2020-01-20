<template>
  <div class="repo-detail">
    <repo-update :repoId="id">
      <template #activator="{ on }">
        <v-btn v-on="on">update</v-btn>
      </template>
    </repo-update>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import { Repo } from '@/store/types'
import Component from 'vue-class-component'
import RepoUpdateDialog from '../components/RepoUpdateDialog.vue'

@Component({
  components: {
    'repo-update': RepoUpdateDialog
  }
})
export default class RepoComparison extends Vue {
  get id() {
    return this.$route.params.id
  }
}
</script>
