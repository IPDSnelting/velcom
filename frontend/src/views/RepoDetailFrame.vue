<template>
  <div class="repo-detail-frame">
    <v-container>
      <v-row align="baseline" justify="center">
        <h1>Repository details</h1>
      </v-row>
      <v-row align="baseline" justify="center">
        <v-col class="d-flex">
          <v-select
            v-model="selectedRepo"
            :items="allRepos"
            item-text="name"
            label="repository"
            return-object
          ></v-select>
        </v-col>
      </v-row>
      <router-view></router-view>
    </v-container>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import { Component, Watch } from 'vue-property-decorator'
import { vxm } from '../store/classIndex'
import { Repo } from '../store/types'

@Component
export default class RepoDetailFrame extends Vue {
  private selectedRepo: Repo | null = null

  get allRepos(): Repo[] {
    return vxm.repoModule.allRepos
  }

  get route(): string | null {
    return this.selectedRepo === null
      ? null
      : '/repo-detail/' + this.selectedRepo.id
  }

  get repoSelected(): boolean {
    return this.selectedRepo !== null
  }

  @Watch('selectedRepo')
  selectedRepoChanged() {
    this.$router.push({
      name: 'repo-detail',
      params: { id: this.selectedRepo ? this.selectedRepo.id : '' }
    })
  }

  mounted() {
    vxm.repoModule.fetchRepos()
  }
}
</script>
