<template>
  <div class="repo-selector">
    <v-container fluid>
      <v-card flat outlined max-width="380">
        <v-card-title>
          <v-toolbar color="primary" dark>Repositories</v-toolbar>
        </v-card-title>
        <v-card-text>
          <v-expansion-panels v-for="(repo, i) in allRepos" :key="i" multiple accordion flat>
            <v-expansion-panel>
              <v-expansion-panel-header>
                <v-checkbox
                  class="shrink mt-0"
                  hide-details
                  v-model="selectedRepos"
                  :value="repo.id"
                  :color="colorByIndex(i)"
                  @click.native.stop
                  @change="updateSelectedRepos()"
                ></v-checkbox>
                <router-link
                  class="ml-3 mx-auto"
                  :to="{ name: 'repo-detail', params: { id: repo.id } }"
                  tag="button"
                >
                  <span class="panel-header">{{ repo.name }}</span>
                </router-link>
              </v-expansion-panel-header>
              <v-expansion-panel-content>
                <div v-for="(branch, j) in repo.trackedBranches" :key="j">
                  <v-checkbox
                    multiple
                    hide-details
                    class="shrink mt-0 ml-5"
                    :input-value="selectedBranchesByRepo(repo.id)"
                    :label="branch"
                    :value="branch"
                    :disabled="!repoSelected(repo.id)"
                    :color="colorByIndex(i)"
                    @change="updateSelectedBranchesForRepo(repo.id, branch, $event)"
                  />
                </div>
              </v-expansion-panel-content>
            </v-expansion-panel>
          </v-expansion-panels>
        </v-card-text>
      </v-card>
    </v-container>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop, Model, Watch } from 'vue-property-decorator'
import { Store } from 'vuex'

import { vxm } from '../store/classIndex'
import { Repo } from '../store/types'

@Component
export default class RepoSelector extends Vue {
  @Prop({})
  private repoID!: string

  @Prop({})
  private index!: number

  private selectedRepos: string[] = vxm.repoComparisonModule.selectedRepos

  get allRepos(): Repo[] {
    return vxm.repoModule.allRepos
  }

  get repoSelected(): (repoID: string) => boolean {
    return (repoID: string) => this.selectedRepos.indexOf(repoID) > -1
  }

  get selectedBranchesByRepo(): (repoId: string) => string[] {
    return (repoId: string) =>
      vxm.repoComparisonModule.selectedBranchesByRepoID[repoId]
  }

  get allColors() {
    return vxm.colorModule.allColors
  }

  get colorByIndex(): (index: number) => string {
    return (index: number) => vxm.colorModule.colorByIndex(index)
  }

  updateSelectedRepos() {
    vxm.repoComparisonModule.selectedRepos = this.selectedRepos
  }

  updateSelectedBranchesForRepo(
    repoID: string,
    branch: string,
    checkedValues: string[]
  ) {
    vxm.repoComparisonModule.setSelectedBranchesForRepo({
      repoID: repoID,
      selectedBranches: checkedValues
    })
  }

  @Watch('allRepos')
  addMissingColors() {
    if (this.allColors.length < this.allRepos.length) {
      let diff = this.allRepos.length - this.allColors.length
      vxm.colorModule.addColors(diff)
    }
  }

  async created() {
    await vxm.repoModule.fetchRepos()
  }
}
</script>

<style scoped>
.panel-header {
  font-size: large;
}
</style>
