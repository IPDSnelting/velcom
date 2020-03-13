<template>
  <v-card>
    <v-card-title>
      <v-toolbar color="primary darken-1" dark>
        {{ repo.name }}
        <span class="ml-5 subtitle-1">{{ repo.id }}</span>
      </v-toolbar>
    </v-card-title>
    <v-card-text>
      <v-container fluid>
        <v-row align="center">
          <v-col cols="3" class="subtitle-2">Remote-URL:</v-col>
          <v-col cols="9">
            <a :href="repo.remoteURL">{{ repo.remoteURL }}</a>
          </v-col>
        </v-row>
        <v-row align="center">
          <v-col cols="3" class="subtitle-2">ID:</v-col>
          <v-col cols="9">{{ repo.id }}</v-col>
        </v-row>
        <v-row align="center">
          <v-col cols="3" class="subtitle-2">Branches:</v-col>
          <v-col cols="9">
            <v-tooltip top v-for="(branch, index) in branches" :key="branch + index">
              <template v-slot:activator="{ on }">
                <v-chip
                  :class="{ 'ma-2': true, 'untracked': !isBranchTracked(branch) }"
                  outlined
                  label
                  v-on="on"
                  :color="isBranchTracked(branch) ? 'success' : undefined"
                >{{ branch }}</v-chip>
              </template>
              {{ isBranchTracked(branch) ? 'Tracked' : 'Not Tracked' }}
            </v-tooltip>
          </v-col>
        </v-row>
      </v-container>
    </v-card-text>
    <v-card-actions v-if="canEdit">
      <v-spacer></v-spacer>
      <repo-update :repoId="repo.id">
        <template #activator="{ on }">
          <v-btn v-on="on" color="primary">update</v-btn>
        </template>
      </repo-update>
      <v-btn
        color="error"
        class="mr-5 ml-3"
        outlined
        text
        @click="deleteRepository"
      >Delete Repository</v-btn>
    </v-card-actions>
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import { Repo } from '@/store/types'
import { vxm } from '@/store'
import RepoUpdateDialog from '@/components/dialogs/RepoUpdateDialog.vue'

@Component({
  components: {
    'repo-update': RepoUpdateDialog
  }
})
export default class RepoBaseInformation extends Vue {
  @Prop()
  private repo!: Repo

  private get branches() {
    return this.repo.branches
      .slice()
      .sort(
        this.chainComparators(this.comparatorTrackStatus, (a, b) =>
          a.localeCompare(b)
        )
      )
  }

  private get canEdit() {
    return vxm.userModule.authorized(this.repo.id)
  }

  private deleteRepository() {
    let confirmed = window.confirm(
      `Do you really want to delete ${this.repo.name} (${this.repo.id})?`
    )
    if (!confirmed) {
      return
    }
    vxm.repoModule.deleteRepo(this.repo.id).then(() => {
      vxm.repoDetailModule.selectedRepoId = ''
      this.$router.replace({ name: 'repo-detail-frame', params: { id: '' } })
    })
  }

  private isBranchTracked(branch: string): boolean {
    return this.repo.trackedBranches.includes(branch)
  }

  private comparatorTrackStatus(branchA: string, branchB: string) {
    const aTracked = this.isBranchTracked(branchA)
    const bTracked = this.isBranchTracked(branchB)
    if (aTracked && bTracked) {
      return 0
    }
    if (aTracked) {
      return -1
    }
    if (bTracked) {
      return 1
    }
    return 0
  }

  private chainComparators(
    a: (a: string, b: string) => number,
    b: (a: string, b: string) => number
  ): (a: string, b: string) => number {
    return (x, y) => {
      if (a(x, y) !== 0) {
        return a(x, y)
      }
      return b(x, y)
    }
  }
}
</script>

<style scoped>
</style>

<style>
.untracked > span {
  opacity: 0.7;
}
</style>
