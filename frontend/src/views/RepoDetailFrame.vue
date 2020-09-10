<template>
  <div class="repo-detail-frame">
    <v-container>
      <v-row align="center" justify="center">
        <v-col class="d-flex">
          <repo-select v-model="selectedRepoId" :repos="allRepos"></repo-select>
        </v-col>
        <v-col cols="auto" v-if="isAdmin">
          <repo-add>
            <template #activator="{ on }">
              <v-btn color="success" :outlined="isDarkMode" v-on="on">
                <v-icon left>{{ plusIcon }}</v-icon
                >Add Repo
              </v-btn>
            </template>
          </repo-add>
        </v-col>
      </v-row>
      <page-404
        v-if="!repoSelected"
        title="kenós"
        subtitle="No repository selected"
      ></page-404>
      <router-view></router-view>
    </v-container>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import { Component } from 'vue-property-decorator'
import { vxm } from '../store/index'
import { Repo } from '../store/types'
import RepoSelectionComponent from '../components/RepoSelectionComponent.vue'
import { mdiPlusCircleOutline } from '@mdi/js'
import RepoAddDialog from '../components/dialogs/RepoAddDialog.vue'
import NotFound404 from './NotFound404.vue'

@Component({
  components: {
    'repo-select': RepoSelectionComponent,
    'repo-add': RepoAddDialog,
    'page-404': NotFound404
  }
})
export default class RepoDetailFrame extends Vue {
  get selectedRepoId(): string {
    return this.$route.params.id
  }

  set selectedRepoId(repoId: string) {
    if (this.selectedRepoId === repoId) {
      return
    }
    this.$router.replace({ name: 'repo-detail', params: { id: repoId } })
    vxm.detailGraphModule.selectedRepoId = repoId
  }

  get selectedRepo(): Repo | null {
    return this.selectedRepoId == null
      ? null
      : vxm.repoModule.repoById(this.selectedRepoId)!
  }

  get allRepos(): Repo[] {
    return vxm.repoModule.allRepos
  }

  get isAdmin(): boolean {
    return vxm.userModule.isAdmin
  }

  get isDarkMode(): boolean {
    return vxm.userModule.darkThemeSelected
  }

  get repoSelected(): boolean {
    return this.selectedRepo !== null
  }

  mounted(): void {
    if (!this.selectedRepoId && vxm.detailGraphModule.selectedRepoId) {
      this.selectedRepoId = vxm.detailGraphModule.selectedRepoId
    }
    if (vxm.repoModule.allRepos.length === 1) {
      this.selectedRepoId = vxm.repoModule.allRepos[0].id
    }
  }

  // ============== ICONS ==============
  private plusIcon = mdiPlusCircleOutline
  // ==============       ==============
}
</script>
