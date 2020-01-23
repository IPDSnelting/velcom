<template>
  <v-container>
    <v-row no-gutters>
      <v-col>
        <v-card>
          <v-card-title>
            <v-toolbar dark color="primary darken-1" class="wrapping-toolbar">
              <v-container fluid>
                <v-row no-gutters align="center" justify="space-between">
                  <v-col style="flex: 1 1 60%;">
                    <span class="mx-2 message font-weight-regular">{{ commitSummary }}</span>
                  </v-col>
                  <v-col cols="auto">
                    <v-spacer></v-spacer>
                    <inline-repo-display class="px-2" :repoId="commit.repoID"></inline-repo-display>—
                    <span class="mx-2 hash">{{ commit.hash }}</span>
                  </v-col>
                </v-row>
              </v-container>
            </v-toolbar>
            <v-card-text class="body-1">
              <span class="font-weight-bold">{{ commit.author }}</span> authored at
              <span
                :title="formatDateUTC(commit.authorDate)"
              >{{ formatDate(commit.authorDate) }}</span>
              <br />
              <span class="font-weight-bold">{{ commit.committer }}</span> committed at
              <span
                :title="formatDateUTC(commit.committerDate)"
              >{{ formatDate(commit.committerDate) }}</span>
              <br />
              <div class="mt-5 commit-detail-message">{{ restOfCommitMessage.trim() }}</div>

              <div class="mt-5 mb-2 overline">Parents:</div>
              <v-tooltip v-for="parent in commit.parents" :key="parent" right>
                <template #activator="{ on }">
                  <commit-chip
                    :on="on"
                    :to="{ name: 'commit-detail', params: { repoID: commit.repoID, hash: parent } }"
                    :commit="commit"
                    :copyOnClick="false"
                  ></commit-chip>
                </template>
                Clicking me will navigate to the commit
              </v-tooltip>
            </v-card-text>
          </v-card-title>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Commit } from '../store/types'
import { Prop } from 'vue-property-decorator'
import InlineMinimalRepoNameDisplay from './InlineMinimalRepoDisplay.vue'
import CommitChip from './CommitChip.vue'

@Component({
  components: {
    'inline-repo-display': InlineMinimalRepoNameDisplay,
    'commit-chip': CommitChip
  }
})
export default class CommitInformation extends Vue {
  @Prop()
  private commit!: Commit

  private formatDate(date: number): string {
    let myDate = this.getDate(date)

    return myDate.toLocaleString()
  }

  private formatDateUTC(date: number): string {
    let myDate = this.getDate(date)

    return myDate.toUTCString()
  }

  private getDate(date: number): Date {
    let myDate = new Date()
    // TODO: remove clamping
    myDate.setTime((Math.abs(date) % 1.8934156e9) * 1000)
    return myDate
  }

  private get commitSummary(): string {
    if (!this.commit.message) {
      return 'Commit has no message :('
    }
    return this.commit.message.substring(0, this.commitSummaryEndIndex)
  }

  private get commitSummaryEndIndex(): number {
    if (!this.commit.message) {
      return 0
    }
    let firstNewline = this.commit.message.indexOf('\n')
    return firstNewline < 0 ? this.commit.message.length : firstNewline
  }

  private get restOfCommitMessage() {
    if (!this.commit.message) {
      return ''
    }
    if (this.commitSummaryEndIndex >= this.commit.message.length) {
      return ''
    }
    return this.commit.message.substring(this.commitSummaryEndIndex)
  }
}
</script>

<style scoped>
.hash,
.commit-detail-message {
  font-family: monospace;
}
.commit-detail-message {
  white-space: pre-wrap;
}
.wrapping-toolbar {
  height: auto !important;
  min-height: 64px;
}
</style>

<style>
.wrapping-toolbar > .v-toolbar__content {
  height: auto !important;
  min-height: 64px;
}
</style>
