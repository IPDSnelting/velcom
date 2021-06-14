<template>
  <v-card>
    <v-card-title>
      <v-toolbar color="toolbarColor" dark>
        <div
          :class="
            $vuetify.breakpoint.xs
              ? ['d-flex', 'flex-wrap', 'justify-center', 'align-center']
              : ['justify-space-between', 'd-flex', 'align-center']
          "
          style="width: 100%"
        >
          <span>{{ repo.name }}</span>
          <span
            style="flex: 0 0 100%"
            class="my-1"
            v-if="$vuetify.breakpoint.xs"
          ></span>
          <span class="ml-5 subtitle-1" style="margin-right: auto">
            {{ repo.id }}
          </span>
          <span
            style="flex: 0 0 100%"
            class="my-1"
            v-if="$vuetify.breakpoint.xs"
          ></span>
          <v-btn text :to="{ name: 'search', query: { repoId: repo.id } }">
            Search and Compare Runs
            <v-icon right size="22">{{ searchAndCompareIcon }}</v-icon>
          </v-btn>
        </div>
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
          <v-col cols="9" class="d-flex flex-wrap" style="gap: 8px">
            <v-tooltip top v-for="branch in branches" :key="branch.name">
              <template v-slot:activator="{ on }">
                <v-chip
                  :class="{ untracked: !branch.tracked }"
                  outlined
                  label
                  v-on="on"
                  :color="branch.tracked ? 'success' : undefined"
                >
                  {{ branch.name }}
                </v-chip>
              </template>
              {{ branch.tracked ? 'Tracked' : 'Not Tracked' }}
            </v-tooltip>
          </v-col>
        </v-row>
        <v-row v-if="hasActiveBot || isAdmin">
          <v-col cols="3" class="subtitle-2">Github Bot</v-col>
          <v-col cols="9">
            <github-bot-command-chips
              :prs="githubCommands"
              :repo="repo"
              v-if="hasActiveBot"
            ></github-bot-command-chips>
            <span v-if="hasActiveBot && githubCommands.length === 0">
              No commands found yet
            </span>
            <div v-if="!hasActiveBot && isAdmin">
              No Github bot set up. You can do that by following these steps:
              <ol class="mt-1">
                <li>
                  On Github, create a new account for the bot if you want to.
                </li>
                <li>
                  On Github, log in to the account you want the bot to use. Note
                  that the account must have <strong>push</strong> access to the
                  repository.<br />
                  VelCom only allows collaborators to trigger benchmarks and
                  fetching the collaborator status requires push permissions.
                </li>
                <li>
                  On GitHub, go to Settings > Developer settings > Personal
                  access tokens
                </li>
                <li>
                  On Github, generate a new token with the "public_repo" scope
                  (or the "repo" scope if you want to use it on a private
                  repository)
                </li>
                <li>
                  Use the "Update" button to add the access token to the repo.
                </li>
              </ol>
            </div>
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
      >
        Delete Repository
      </v-btn>
    </v-card-actions>
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import { GithubBotCommand, Repo, RepoBranch } from '@/store/types'
import { vxm } from '@/store'
import RepoUpdateDialog from '@/components/repodetail/RepoUpdateDialog.vue'
import { mdiCompassOutline } from '@mdi/js'
import GithubBotCommandChips from '@/components/repodetail/GithubBotCommandChips.vue'

@Component({
  components: {
    GithubBotCommandChips,
    RepoUpdateDialog
  }
})
export default class RepoBaseInformation extends Vue {
  private githubCommands: GithubBotCommand[] = []

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

  private get hasActiveBot() {
    return this.repo.lastGithubUpdate !== undefined
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

  private async mounted() {
    if (this.hasActiveBot) {
      this.githubCommands = await vxm.repoModule.fetchGithubCommands(
        this.repo.id
      )
    }
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
