<template>
  <v-container v-if="repoExists(id)">
    <v-row>
      <v-col>
        <v-card>
          <v-card-title>
            <v-toolbar color="primary" dark>
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
                  <v-tooltip top v-for="branch in branches" :key="branch">
                    <template v-slot:activator="{ on }">
                      <v-chip
                        class="ma-2"
                        outlined
                        label
                        v-on="on"
                        :color="isBranchTracked(branch) ? 'accent' : 'error'"
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
            <repo-update :repoId="id">
              <template #activator="{ on }">
                <v-btn v-on="on">update</v-btn>
              </template>
            </repo-update>
            <v-spacer></v-spacer>
            <v-btn color="error" class="mr-5 mb-2" @click="deleteRepository">Delete Repository</v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import { Repo } from '@/store/types'
import Component from 'vue-class-component'
import RepoUpdateDialog from '../components/RepoUpdateDialog.vue'
import { vxm } from '../store/classIndex'

@Component({
  components: {
    'repo-update': RepoUpdateDialog
  }
})
export default class RepoDetail extends Vue {
  private get id() {
    return this.$route.params.id
  }

  private get canEdit() {
    return vxm.userModule.authorized(this.id)
  }

  private repoExists(id: string): boolean {
    return vxm.repoModule.repoByID(id) !== undefined
  }

  private isBranchTracked(branch: string): boolean {
    return this.repo.trackedBranches.indexOf(branch) >= 0
  }

  private deleteRepository() {
    let confirmed = window.confirm(
      `Do you really want to delete ${this.repo.name} (${this.id})?`
    )
    if (!confirmed) {
      return
    }
    vxm.repoModule
      .deleteRepo(this.id)
      .then(() => this.$router.push({ name: 'repo-detail-frame' }))
  }

  private get branches() {
    return this.repo.branches
      .slice()
      .sort(
        this.chainComparators(this.comparatorTrackStatus, (a, b) =>
          a.localeCompare(b)
        )
      )
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

  private get repo(): Repo {
    let repo = vxm.repoModule.repoByID(this.id)!
    if (repo.branches.length < 15) {
      let branchNumber: number = repo.branches.length
      repo.branches.push('Hello world ' + branchNumber)
      if (Math.random() < 0.5) {
        repo.trackedBranches.push('Hello world ' + branchNumber)
      }
    }
    return repo
  }
}
</script>
