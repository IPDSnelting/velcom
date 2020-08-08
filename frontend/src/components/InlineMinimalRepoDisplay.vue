<template>
  <router-link class="concealed-link" :to="{ name: 'repo-detail', params: { id: repo.id } }">
    <span class="repo-name" :title="'Repo-ID: ' + repo.id">{{ repo.name }}</span>
  </router-link>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import { vxm } from '../store/index'
import { Repo } from '../store/types'

@Component
export default class InlineMinimalRepoNameDisplay extends Vue {
  @Prop()
  private repoId!: string

  get repo() {
    return (
      vxm.repoModule.repoByID(this.repoId) ||
      new Repo('Placeholder', 'Not loaded yet :/', [], [], '', false)
    )
  }
}
</script>

<style scoped>
.repo-name {
  font-weight: bold;
}
.repo-id {
  font-family: monospace;
}
</style>
