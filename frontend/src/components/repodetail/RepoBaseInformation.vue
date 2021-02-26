<template>
  <v-card>
    <v-card-title>
      <v-toolbar color="toolbarColor" dark>
        {{ repo.name }}
        <span class="ml-5 subtitle-1">{{ repo.id }}</span>
        <v-spacer></v-spacer>
        <v-btn
          text
          :to="{ name: 'prepare-run-compare', query: { repoId: repo.id } }"
        >
          Compare Runs
          <v-icon right size="22">{{ compareRunIcon }}</v-icon>
        </v-btn>
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
import { mdiScaleBalance } from '@mdi/js'

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
          a.name.localeCompare(b.name)
        )
      )
  }

  private get canEdit() {
    return vxm.userModule.authorized(this.repo.id)
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

  // ===== ICONS =====
  private compareRunIcon = mdiScaleBalance
}
</script>

<style scoped></style>

<style>
.untracked > span {
  opacity: 0.7;
}
</style>
