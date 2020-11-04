<template>
  <div class="repo-selector">
    <v-container fluid class="ma-0 pa-0">
      <v-card flat>
        <v-card-title>
          <v-toolbar color="toolbarColor" dark>Repositories</v-toolbar>
        </v-card-title>
        <v-card-text>
          <v-list>
            <v-list-group v-for="repo in allRepos" :key="repo.id">
              <template v-slot:activator>
                <v-checkbox
                  v-model="selectedRepos"
                  :value="repo.id"
                  :color="colorById(repo.id)"
                  @click.native.stop
                  @change="updateSelectedRepos()"
                ></v-checkbox>
                <v-tooltip bottom>
                  <template #activator="{ on }">
                    <router-link
                      class="ml-3 mx-auto"
                      :to="{ name: 'repo-detail', params: { id: repo.id } }"
                      tag="button"
                    >
                      <v-list-item-title v-on="on">{{
                        repo.name
                      }}</v-list-item-title>
                      <v-list-item-subtitle v-on="on">{{
                        repo.id
                      }}</v-list-item-subtitle>
                    </router-link>
                  </template>
                  <span>go to detail page of {{ repo.name }}</span>
                </v-tooltip>
              </template>
              <v-list-item
                v-for="(branch, index) in repo.trackedBranches"
                :key="index"
              >
                <v-list-item-title>
                  <v-checkbox
                    multiple
                    class="ml-5"
                    :input-value="selectedBranchesByRepo(repo.id)"
                    :label="branch"
                    :value="branch"
                    :disabled="!repoSelected(repo.id)"
                    :color="colorById(repo.id)"
                    @change="
                      updateSelectedBranchesForRepo(repo.id, branch, $event)
                    "
                  />
                </v-list-item-title>
              </v-list-item>
            </v-list-group>
          </v-list>
        </v-card-text>
      </v-card>
    </v-container>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Watch } from 'vue-property-decorator'
import { vxm } from '../store/index'
import { Repo, RepoId } from '../store/types'

@Component
export default class RepoSelector extends Vue {
  private notifyTimeout: number | undefined

  get selectedRepos(): RepoId[] {
    return vxm.comparisonGraphModule.selectedRepos
  }

  set selectedRepos(repos: RepoId[]) {
    vxm.comparisonGraphModule.selectedRepos = repos
  }

  get allRepos(): Repo[] {
    return vxm.repoModule.allRepos
  }

  get repoSelected(): (repoId: string) => boolean {
    return (repoId: string) => this.selectedRepos.indexOf(repoId) > -1
  }

  get selectedBranchesByRepo(): (repoId: string) => string[] {
    return (repoId: string) =>
      vxm.comparisonGraphModule.selectedBranchesByRepoId[repoId]
  }

  get allColors(): string[] {
    return vxm.colorModule.allColors
  }

  get colorById(): (repoId: string) => string {
    return (repoId: string) => {
      const repoIndex = vxm.repoModule.repoIndex(repoId)
      return vxm.colorModule.colorByIndex(repoIndex)
    }
  }

  updateSelectedRepos(): void {
    this.debounce(this.notifySelectionChanged, 1000)()
  }

  updateSelectedBranchesForRepo(
    repoId: string,
    branch: string,
    checkedValues: string[]
  ): void {
    vxm.comparisonGraphModule.setSelectedBranchesForRepo({
      repoId: repoId,
      selectedBranches: checkedValues
    })
    this.debounce(this.notifySelectionChanged, 1000)()
  }

  debounce(func: (...args: any[]) => void, wait: number): () => void {
    return () => {
      if (this.notifyTimeout) {
        return
      }
      // eslint-disable-next-line @typescript-eslint/no-this-alias
      const context = this
      const args = arguments
      clearTimeout(this.notifyTimeout)
      this.notifyTimeout = setTimeout(() => {
        this.notifyTimeout = undefined
        func.apply(context, Array.from(args))
      }, wait)
    }
  }

  notifySelectionChanged(): void {
    this.$emit('selection-changed')
  }

  @Watch('allRepos')
  addMissingColors(): void {
    if (this.allColors.length < this.allRepos.length) {
      const diff = this.allRepos.length - this.allColors.length
      vxm.colorModule.addColors(diff)
    }
  }
}
</script>

<style scoped>
.panel-header {
  font-size: large;
}
</style>
