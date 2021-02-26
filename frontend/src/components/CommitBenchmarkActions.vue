<template>
  <span>
    <v-tooltip top v-for="item in buttonDescriptions" :key="item.tooltip">
      <template #activator="{ on }">
        <v-btn
          v-on="on"
          small
          icon
          @click="item.handler && item.handler($event)"
          :to="item.to"
          :href="item.href"
          :target="item.external ? '_blank' : ''"
          :rel="item.external ? 'noopener nofollow' : ''"
        >
          <v-icon>{{ item.icon }}</v-icon>
        </v-btn>
      </template>
      <span v-html="item.tooltip"></span>
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
  mdiOpenInNew,
  mdiScaleBalance
} from '@mdi/js'
import { Prop } from 'vue-property-decorator'
import { vxm } from '@/store'
import { CommitDescription, Repo, RunId } from '@/store/types'
import { RawLocation } from 'vue-router'

type ButtonDescription = {
  handler?: (e: MouseEvent) => void
  icon: string
  tooltip: string
  to?: RawLocation
  href?: string
  external?: boolean
  show: boolean
}

@Component
export default class CommitBenchmarkActions extends Vue {
  @Prop({ default: true })
  private hasExistingBenchmark!: boolean

  @Prop()
  private commitDescription!: CommitDescription

  @Prop({ default: null })
  private runId!: RunId | null

  private get buttonDescriptions(): ButtonDescription[] {
    return [
      {
        handler: this.benchmark,
        icon: this.hasExistingBenchmark
          ? this.rebenchmarkIcon
          : this.benchmarkIcon,
        tooltip: this.hasExistingBenchmark
          ? 'Re-run all benchmarks for this commit'
          : 'Run all benchmarks for this commit',
        show: this.isAdmin
      },
      {
        handler: this.benchmarkUpwards,
        icon: this.benchmarkUpwardsIcon,
        tooltip:
          'Benchmark all commits upwards of this commit (this <strong>one</strong> and <strong>up</strong>)',
        show: this.isAdmin
      },
      {
        to: this.compareRunLocation || undefined,
        icon: this.compareIcon,
        tooltip: 'Compare this run with another',
        show: this.runId !== null
      },
      {
        href: this.commitRemoteLink,
        icon: this.commitRemoteIcon,
        tooltip: `View this commit on <strong>${this.getRemoteHostname}</strong>`,
        external: true,
        show: true
      }
    ].filter(it => it.show)
  }

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

  private get compareRunLocation() {
    if (!this.runId) {
      return null
    }
    return {
      name: 'search',
      params: {
        runId: this.runId
      }
    }
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

  private get commitRemoteIcon(): string {
    if (!this.repo) {
      return mdiOpenInNew
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
  private compareIcon = mdiScaleBalance
  // ==============       ==============
}
</script>
