<template>
  <span>
    <v-tooltip top v-if="isAdmin">
      <template #activator="{ on }">
        <v-btn v-on="on" icon @click="benchmark">
          <v-icon class="rocket">
            {{ hasExistingBenchmark ? rebenchmarkIcon : benchmarkIcon }}
          </v-icon>
        </v-btn>
      </template>
      <span v-if="hasExistingBenchmark">
        Re-runs all benchmarks for this commit
      </span>
      <span v-else>Runs all benchmarks for this commit</span>
    </v-tooltip>
    <v-tooltip top v-if="isAdmin">
      <template #activator="{ on }">
        <v-btn icon v-on="on" @click="benchmarkUpwards">
          <v-icon>{{ benchmarkUpwardsIcon }}</v-icon>
        </v-btn>
      </template>
      Benchmarks all commits upwards of this commit (this
      <strong>one</strong> and <strong>up</strong>)
    </v-tooltip>
    <v-tooltip top v-if="commitRemoteLink && commitRemoteIcon">
      <template #activator="{ on }">
        <v-btn
          icon
          v-on="on"
          :href="commitRemoteLink"
          target="_blank"
          rel="noopener nofollow"
        >
          <v-icon>{{ commitRemoteIcon }}</v-icon>
        </v-btn>
      </template>
      View this commit on <strong>{{ getRemoteHostname }}</strong>
    </v-tooltip>
  </span>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import {
  mdiBitbucket,
  mdiFlash,
  mdiGithub,
  mdiGitlab,
  mdiHistory,
  mdiOneUp,
  mdiOpenInNew
} from '@mdi/js'
import { Prop } from 'vue-property-decorator'
import { vxm } from '@/store'
import { CommitDescription, Repo } from '@/store/types'

@Component
export default class CommitBenchmarkActions extends Vue {
  @Prop({ default: true })
  private hasExistingBenchmark!: boolean

  @Prop()
  private commitDescription!: CommitDescription

  private get isAdmin(): boolean {
    return vxm.userModule.isAdmin
  }

  private benchmark() {
    vxm.queueModule.startManualTask({
      repoId: this.commitDescription.repoId,
      hash: this.commitDescription.hash
    })
  }

  private async benchmarkUpwards() {
    const taskCount = await vxm.queueModule.dispatchQueueUpwardsOf(
      this.commitDescription
    )
    this.$globalSnackbar.setSuccess('one-up', `One upped ${taskCount} commits!`)
  }

  private get repo(): Repo | undefined {
    return vxm.repoModule.repoById(this.commitDescription.repoId)
  }

  private get commitRemoteLink(): string | undefined {
    if (!this.repo) {
      return undefined
    }
    const match = /^(https:\/\/(.+)\/|git@(.+):)(.+)\/(.+?)(\.git)?$/.exec(
      this.repo.remoteURL
    )
    if (!match) {
      return undefined
    }
    const domain = match[2] || match[3]
    const account = match[4]
    const projectName = match[5]

    const commitMarker = this.repo.remoteURL.includes('bitbucket')
      ? 'commits'
      : 'commit'

    return `https://${domain}/${account}/${projectName}/${commitMarker}/${this.commitDescription.hash}`
  }

  private get getRemoteHostname(): string | undefined {
    if (!this.repo) {
      return undefined
    }
    const match = /^(https:\/\/(.+)\/|git@(.+):)(.+)\/(.+?)(\.git)?$/.exec(
      this.repo.remoteURL
    )
    if (!match) {
      return undefined
    }
    return match[2] || match[3]
  }

  private get commitRemoteIcon(): string | undefined {
    if (!this.repo) {
      return undefined
    }
    if (this.repo.remoteURL.includes('github.com')) {
      return mdiGithub
    }
    if (this.repo.remoteURL.includes('gitlab.com')) {
      return mdiGitlab
    }
    if (this.repo.remoteURL.includes('bitbucket')) {
      return mdiBitbucket
    }
    return mdiOpenInNew
  }

  // ============== ICONS ==============
  private rebenchmarkIcon = mdiHistory
  private benchmarkIcon = mdiFlash
  private benchmarkUpwardsIcon = mdiOneUp
  // ==============       ==============
}
</script>
