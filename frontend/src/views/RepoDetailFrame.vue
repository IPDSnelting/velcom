<template>
  <div class="repo-detail-frame">
    <v-container>
      <v-row align="baseline" justify="center">
        <h1>Repository details</h1>
      </v-row>
      <v-row align="center" justify="center">
        <v-col class="d-flex">
          <repo-select v-model="selectedRepoId" :repos="allRepos"></repo-select>
        </v-col>
        <v-col cols="auto">
          <repo-add v-if="isAdmin">
            <template #activator="{ on }">
              <v-btn color="success" v-on="on">
                <v-icon left>{{ plusIcon }}</v-icon>Add Repo
              </v-btn>
            </template>
          </repo-add>
        </v-col>
      </v-row>
      <router-view></router-view>
    </v-container>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import { Component, Watch } from 'vue-property-decorator'
import { vxm } from '../store/index'
import { Repo } from '../store/types'
import RepoSelectionComponent from '../components/RepoSelectionComponent.vue'
import { mdiPlusCircleOutline } from '@mdi/js'
import RepoAddDialog from '../components/dialogs/RepoAddDialog.vue'

@Component({
  components: {
    'repo-select': RepoSelectionComponent,
    'repo-add': RepoAddDialog
  }
})
export default class RepoDetailFrame extends Vue {
  get selectedRepoId(): string {
    return this.$route.params.id || ''
  }

  set selectedRepoId(repoId: string) {
    this.$router.push({ name: 'repo-detail', params: { id: repoId } })
  }

  get selectedRepo() {
    return this.selectedRepoId == null
      ? null
      : vxm.repoModule.repoByID(this.selectedRepoId)!
  }

  get allRepos(): Repo[] {
    return vxm.repoModule.allRepos
  }

  get route(): string | null {
    return this.selectedRepo === null
      ? null
      : '/repo-detail/' + this.selectedRepo.id
  }

  get isAdmin() {
    return vxm.userModule.isAdmin
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

  // ============== ICONS ==============
  private plusIcon = mdiPlusCircleOutline
  // ==============       ==============
}
</script>
