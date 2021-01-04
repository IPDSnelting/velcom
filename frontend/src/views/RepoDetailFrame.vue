<template>
  <div class="repo-detail-frame">
    <v-container class="mt-0 pt-0">
      <v-row align="center" justify="center">
        <v-col cols="auto">
          <repo-select v-model="selectedRepoId" :repos="allRepos"></repo-select>
        </v-col>
        <v-spacer></v-spacer>
        <v-col cols="auto">
          <v-tabs v-model="selectedTab">
            <v-tab>Repo Information</v-tab>
            <v-tab>Graph</v-tab>
            <v-tab>Commit search</v-tab>
          </v-tabs>
        </v-col>
        <v-spacer></v-spacer>
        <v-col cols="auto" v-if="isAdmin">
          <repo-add>
            <template #activator="{ on }">
              <v-btn color="success" :outlined="isDarkMode" v-on="on">
                <v-icon left>{{ plusIcon }}</v-icon>
                Add Repo
              </v-btn>
            </template>
          </repo-add>
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="12">
          <v-tabs-items v-model="selectedTab">
            <v-tab-item>
              <repo-base-information
                v-if="repo"
                :repo="repo"
              ></repo-base-information>
            </v-tab-item>
            <v-tab-item>
              <repo-graph-view></repo-graph-view>
            </v-tab-item>
            <v-tab-item>Hello world 3</v-tab-item>
          </v-tabs-items>
        </v-col>
      </v-row>
      <page-404
        v-if="!repoSelected"
        title="kenós"
        subtitle="No repository selected"
      ></page-404>
    </v-container>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import { Component } from 'vue-property-decorator'
import { vxm } from '@/store'
import { Repo } from '@/store/types'
import RepoSelectionComponent from '../components/RepoSelectionComponent.vue'
import { mdiPlusCircleOutline } from '@mdi/js'
import RepoAddDialog from '../components/dialogs/RepoAddDialog.vue'
import NotFound404 from './NotFound404.vue'
import RepoBaseInformation from '@/components/repodetail/RepoBaseInformation.vue'
import RepoGraphView from '@/views/RepoGraphView.vue'

@Component({
  components: {
    'repo-graph-view': RepoGraphView,
    'repo-base-information': RepoBaseInformation,
    'repo-select': RepoSelectionComponent,
    'repo-add': RepoAddDialog,
    'page-404': NotFound404
  }
})
export default class RepoDetailFrame extends Vue {
  private selectedTab = 1

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

  private get repo() {
    return vxm.repoModule.repoById(this.selectedRepoId)
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
    vxm.detailGraphModule.adjustToPermanentLink(this.$route)
  }

  // ============== ICONS ==============
  private plusIcon = mdiPlusCircleOutline
  // ==============       ==============
}
</script>
