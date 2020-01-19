<template>
  <div class="repo-comparison">
    <v-container fluid>
      <v-col>
        <v-row align="baseline" justify="center">
          <h1>Repository Comparison</h1>
        </v-row>
      </v-col>
      <v-col>
        <v-row align="start" justify="start">
          <v-card max-width="400">
            <v-expansion-panels v-for="(repo, index) in allRepos" :key="index">
              <repo-selector :repoID="repo.id" :index="index" @updateSelect="updateSelectedRepos"></repo-selector>
            </v-expansion-panels>
            <v-spacer></v-spacer>
            <repo-add>
              <template #activator="{ on }">
                <v-btn v-on="on">add a new repository</v-btn>
              </template>
            </repo-add>
          </v-card>
        </v-row>
      </v-col>
    </v-container>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Watch } from 'vue-property-decorator'
import { vxm } from '../store/classIndex'
import RepoAddDialog from '../components/RepoAddDialog.vue'
import RepoSelector from '../components/RepoSelector.vue'
import { Repo } from '../store/types'

@Component({
  components: {
    'repo-add': RepoAddDialog,
    'repo-selector': RepoSelector
  }
})
export default class RepoComparison extends Vue {
  private selectedRepos: string[] = []
  private selectedBranchesByRepo: {[key: string]: string[]} = {}

  get allRepos(): Repo[] {
    return vxm.repoModule.allRepos
  }

  get allColors(): string[] {
    return vxm.colorModule.allColors
  }

  updateSelectedRepos(repoID: string, selected: boolean, selectedBranches: string[]) {
    if (selected) {
      this.selectedRepos.push(repoID)
      this.selectedBranchesByRepo[repoID] = selectedBranches
    } else {
      const index: number = this.selectedRepos.indexOf(repoID)
      this.selectedRepos.splice(index, 1)
    }
  }

  @Watch('allRepos')
  addMissingColors() {
    if (this.allColors.length < this.allRepos.length) {
      let diff = this.allRepos.length - this.allColors.length
      vxm.colorModule.addColors(diff)
    }
  }

  mounted() {
    vxm.repoModule.fetchRepos()
  }
}
</script>
