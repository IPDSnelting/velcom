<template>
  <v-card>
    <v-card-title>
      <v-toolbar color="toolbarColor" dark>
        {{ repo.name }}
        <span class="ml-5 subtitle-1">{{ repo.id }}</span>
        <v-spacer></v-spacer>
        <v-btn text :to="{ name: 'search', query: { repoId: repo.id } }">
          Search and Compare Runs
          <v-icon right size="22">{{ searchAndCompareIcon }}</v-icon>
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
            <v-tooltip
              top
              v-for="(branch, index) in branches"
              :key="branch.name"
            >
              <template v-slot:activator="{ on }">
                <v-chip
                  :class="{
                    'ml-2': index > 0,
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
        <v-row>
          <v-col cols="3" class="subtitle-2">Github Bot</v-col>
          <v-col cols="9">
            <github-bot-pr-chips :prs="prs"></github-bot-pr-chips>
          </v-col>
        </v-row>
      </v-container>
    </v-card-text>
    <v-card-actions v-if="isAdmin">
      <v-spacer></v-spacer>
      <repo-update-dialog :repoId="repo.id">
        <template #activator="{ on }">
          <v-btn v-on="on" color="primary">update</v-btn>
        </template>
      </repo-update-dialog>
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
import { GithubBotPr, Repo, RepoBranch } from '@/store/types'
import { vxm } from '@/store'
import RepoUpdateDialog from '@/components/repodetail/RepoUpdateDialog.vue'
import { mdiCompassOutline } from '@mdi/js'
import GithubBotPrChips from '@/components/repodetail/GithubBotPrChips.vue'

@Component({
  components: {
    GithubBotPrChips,
    RepoUpdateDialog
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

  private get isAdmin() {
    return vxm.userModule.isAdmin
  }

  private get prs(): GithubBotPr[] {
    return [
      new GithubBotPr('seen', 2021, 2),
      new GithubBotPr('reacted', 2022, 2),
      new GithubBotPr('queued', 2023, 2)
    ]
  }

  private async deleteRepository() {
    const confirmed = window.confirm(
      `Do you really want to delete ${this.repo.name} (${this.repo.id})?`
    )
    if (!confirmed) {
      return
    }
    await vxm.repoModule.deleteRepo(this.repo.id)
    vxm.detailGraphModule.selectedRepoId = ''
    await this.$router.replace({ name: 'repo-detail', params: { id: '' } })
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
  private searchAndCompareIcon = mdiCompassOutline
}
</script>

<style scoped></style>

<style>
.untracked > span {
  opacity: 0.7;
}
</style>
