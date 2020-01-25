<template>
  <div class="repo-selector">
    <v-container fluid>
      <v-card flat outlined>
        <v-card-title>
          <v-toolbar color="primary darken-1" dark>Repositories</v-toolbar>
        </v-card-title>
        <v-card-text>
          <v-list>
            <v-list-group v-for="repo in allRepos" :key="repo.index">
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
                      <v-list-item-title v-on="on">{{ repo.name }}</v-list-item-title>
                      <v-list-item-subtitle>{{ repo.id }}</v-list-item-subtitle>
                    </router-link>
                  </template>
                  <span>go to detail page of {{ repo.name }}</span>
                </v-tooltip>
              </template>
              <v-list-item v-for="(branch, index) in repo.trackedBranches" :key="index">
                <v-list-item-title>
                  <v-checkbox
                    multiple
                    class="ml-5"
                    :input-value="selectedBranchesByRepo(repo.id)"
                    :label="branch"
                    :value="branch"
                    :disabled="!repoSelected(repo.id)"
                    :color="colorById(repo.id)"
                    @change="updateSelectedBranchesForRepo(repo.id, branch, $event)"
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
import { Prop, Model, Watch } from 'vue-property-decorator'
import { vxm } from '../store/index'
import { Repo } from '../store/types'

@Component
export default class RepoSelector extends Vue {
  @Prop({})
  private repoID!: string

  @Prop({})
  private index!: number

  private notifyTimeout: number | undefined

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

  get colorById(): (repoID: string) => string {
    return (repoID: string) => {
      let repoIndex = vxm.repoModule.repoIndex(repoID)
      return vxm.colorModule.colorByIndex(repoIndex)
    }
  }

  updateSelectedRepos() {
    vxm.repoComparisonModule.selectedRepos = this.selectedRepos
    this.debounce(this.notifySelectionChanged, 1000)()
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
    this.debounce(this.notifySelectionChanged, 1000)()
  }

  debounce(func: Function, wait: number) {
    return () => {
      if (this.notifyTimeout) {
        return
      }
      let context = this
      let args = arguments
      clearTimeout(this.notifyTimeout)
      this.notifyTimeout = setTimeout(() => {
        this.notifyTimeout = undefined
        func.apply(context, args)
      }, wait)
    }
  }

  notifySelectionChanged() {
    this.$emit('selectionChanged')
  }

  @Watch('allRepos')
  addMissingColors() {
    if (this.allColors.length < this.allRepos.length) {
      let diff = this.allRepos.length - this.allColors.length
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
