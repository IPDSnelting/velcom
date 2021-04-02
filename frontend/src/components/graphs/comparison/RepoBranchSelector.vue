<template>
  <v-expansion-panels multiple accordion>
    <v-expansion-panel
      v-for="repo in sortedRepos"
      :key="repo.id"
      :disabled="repo.trackedBranches.length === 0"
    >
      <v-expansion-panel-header>
        <v-checkbox
          :input-value="allBranchesSelected(repo)"
          :indeterminate="onlySomeBranchesSelected(repo)"
          :style="{ color: colorForRepo(repo.id) }"
          :color="colorForRepo(repo.id)"
          dense
          hide-details
          class="ma-0 pa-0 flex-grow-0 colored-checkbox"
          @change="toggleRepo(repo)"
          @click.native.stop="() => {}"
        ></v-checkbox>
        <span v-if="!hasConflictingNames">{{ repo.name }}</span>

        <v-container fluid class="ma-0 pa-0" v-if="hasConflictingNames">
          <v-row no-gutters class="mt-0">
            <v-col>
              <span>{{ repo.name }}</span>
            </v-col>
          </v-row>
          <v-row no-gutters class="mt-0">
            <v-col>
              <span class="caption text--disabled">{{ repo.id }}</span>
            </v-col>
          </v-row>
        </v-container>

        <span v-if="repo.trackedBranches.length === 0" class="text-end ml-3">
          No tracked branches
        </span>
      </v-expansion-panel-header>
      <v-expansion-panel-content>
        <v-list dense class="pt-0">
          <v-list-item v-for="branch in repo.trackedBranches" :key="branch">
            <v-list-item-content class="ma-0 pa-0">
              <v-checkbox
                :input-value="selectedBranchesForRepo(repo.id)"
                :value="branch"
                :label="branch"
                :color="colorForRepo(repo.id)"
                :style="{ color: colorForRepo(repo.id) }"
                class="ma-0 pa-0 colored-checkbox"
                hide-details
                dense
                @change="toggleBranch(repo, branch)"
              ></v-checkbox>
            </v-list-item-content>
          </v-list-item>
        </v-list>
      </v-expansion-panel-content>
    </v-expansion-panel>
  </v-expansion-panels>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Repo, RepoId } from '@/store/types'
import { vxm } from '@/store'

@Component
export default class RepoBranchSelector extends Vue {
  private selectedBranchesForRepo(repoId: RepoId) {
    return vxm.comparisonGraphModule.selectedBranchesForRepo(repoId)
  }

  private get sortedRepos() {
    return vxm.repoModule.allRepos.slice().sort((a, b) => {
      if (a.trackedBranches.length === 0 && b.trackedBranches.length === 0) {
        return a.name.localeCompare(b.name)
      }
      if (a.trackedBranches.length === 0) {
        return 1
      }
      if (b.trackedBranches.length === 0) {
        return -1
      }
      return a.name.localeCompare(b.name)
    })
  }

  private get hasConflictingNames() {
    const distinct = new Set(this.sortedRepos.map(it => it.name.trim()))
    return distinct.size !== this.sortedRepos.length
  }

  private colorForRepo(repoId: RepoId) {
    return vxm.colorModule.colorByIndex(
      vxm.repoModule.allRepos.findIndex(repo => repo.id === repoId) || 0
    )
  }

  private onlySomeBranchesSelected(repo: Repo) {
    const selectedLength = this.selectedBranchesForRepo(repo.id).length
    return selectedLength < repo.trackedBranches.length && selectedLength > 0
  }

  private allBranchesSelected(repo: Repo) {
    if (repo.trackedBranches.length === 0) {
      return false
    }
    const selectedLength = this.selectedBranchesForRepo(repo.id).length
    return selectedLength === repo.trackedBranches.length
  }

  private toggleRepo(repo: Repo) {
    if (this.allBranchesSelected(repo)) {
      vxm.comparisonGraphModule.setSelectedBranchesForRepo({
        repoId: repo.id,
        branches: []
      })
    } else {
      vxm.comparisonGraphModule.setSelectedBranchesForRepo({
        repoId: repo.id,
        branches: repo.trackedBranches
      })
    }
  }

  private toggleBranch(repo: Repo, branch: string) {
    vxm.comparisonGraphModule.toggleRepoBranch({
      repoId: repo.id,
      branch: branch
    })
  }
}
</script>

<style>
.colored-checkbox .v-icon {
  color: inherit !important;
}
</style>
