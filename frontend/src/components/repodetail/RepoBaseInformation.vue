<template>
  <v-card>
    <v-card-title>
      <v-toolbar color="primary darken-1" dark>
        {{ repo.name }}
        <span class="ml-5 subtitle-1">{{ repo.id }}</span>
        <v-spacer></v-spacer>
        <v-btn icon @click="showDetails = !showDetails">
          <v-icon>{{ showDetails ? upIcon : downIcon }}</v-icon>
        </v-btn>
      </v-toolbar>
    </v-card-title>
    <v-expand-transition>
      <div v-show="showDetails">
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
                <v-tooltip top v-for="branch in branches" :key="branch.name">
                  <template v-slot:activator="{ on }">
                    <v-chip
                      :class="{
                        'ma-2': true,
                        untracked: !branch.tracked
                      }"
                      outlined
                      label
                      v-on="on"
                      :color="branch.tracked ? 'success' : undefined"
                      >{{ branch.name }}</v-chip
                    >
                  </template>
                  {{ branch.tracked ? 'Tracked' : 'Not Tracked' }}
                </v-tooltip>
              </v-col>
            </v-row>
          </v-container>
        </v-card-text>
      </div>
    </v-expand-transition>
    <v-card-actions v-if="canEdit">
      <v-spacer></v-spacer>
      <repo-update :repoId="repo.id">
        <template #activator="{ on }">
          <v-btn v-on="on" color="primary">update</v-btn>
        </template>
      </repo-update>
      <v-tooltip bottom>
        <template #activator="{ on }">
          <v-btn
            v-on="on"
            class="mr-5 ml-3"
            outlined
            color="primary"
            text
            @click="refetchRepo"
            v-if="isWebsiteAdmin"
          >
            Refetch repo
          </v-btn>
        </template>
        Executes a `git fetch` updating the benchmark repo as well as this one.
        It should find any new commits and pick up changes to the benchmark
        repo, but might take a few seconds to complete.
      </v-tooltip>
      <v-btn
        color="error"
        class="mr-5 ml-3"
        outlined
        text
        @click="deleteRepository"
        >Delete Repository</v-btn
      >
    </v-card-actions>
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import { Repo, RepoBranch } from '@/store/types'
import { vxm } from '@/store'
import RepoUpdateDialog from '@/components/dialogs/RepoUpdateDialog.vue'
import { mdiChevronUp, mdiChevronDown } from '@mdi/js'

@Component({
  components: {
    'repo-update': RepoUpdateDialog
  }
})
export default class RepoBaseInformation extends Vue {
  @Prop()
  private repo!: Repo

  private showDetails: boolean = false
  private upIcon = mdiChevronUp
  private downIcon = mdiChevronDown

  private get branches() {
    return this.repo.branches
      .slice()
      .sort(
        this.chainComparators(this.comparatorTrackStatus, (a, b) =>
          a.name.localeCompare(b.name)
        )
      )
  }

  private get canEdit() {
    return vxm.userModule.authorized(this.repo.id)
  }

  private get isWebsiteAdmin() {
    return vxm.userModule.isAdmin
  }

  private deleteRepository() {
    const confirmed = window.confirm(
      `Do you really want to delete ${this.repo.name} (${this.repo.id})?`
    )
    if (!confirmed) {
      return
    }
    vxm.repoModule.deleteRepo(this.repo.id).then(() => {
      vxm.detailGraphModule.selectedRepoId = ''
      this.$router.replace({ name: 'repo-detail-frame', params: { id: '' } })
    })
  }

  private async refetchRepo() {
    if (!this.isWebsiteAdmin) {
      return
    }
    await vxm.repoModule.triggerListenerFor(this.repo.id)

    this.$globalSnackbar.setSuccess(
      'listener',
      'Re-fetched repo and updated benchrepo'
    )
  }

  private comparatorTrackStatus(branchA: RepoBranch, branchB: RepoBranch) {
    if (branchA.tracked && branchB.tracked) {
      return 0
    }
    if (branchA.tracked) {
      return -1
    }
    if (branchB.tracked) {
      return 1
    }
    return 0
  }

  private chainComparators<T>(
    a: (a: T, b: T) => number,
    b: (a: T, b: T) => number
  ): (a: T, b: T) => number {
    return (x, y) => {
      if (a(x, y) !== 0) {
        return a(x, y)
      }
      return b(x, y)
    }
  }
}
</script>

<style scoped></style>

<style>
.untracked > span {
  opacity: 0.7;
}
</style>
