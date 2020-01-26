<template>
  <v-container>
    <v-row no-gutters>
      <v-col>
        <v-card>
          <v-card-title>
            <v-toolbar dark color="primary" class="wrapping-toolbar">
              <v-container fluid>
                <v-row no-gutters align="center" justify="space-between">
                  <v-col style="flex: 1 1 60%;">
                    <span class="mx-2 message font-weight-regular">{{ commitSummary }}</span>
                  </v-col>
                  <v-col cols="4" class="ml-3 body-1">
                    <v-spacer></v-spacer>
                    <inline-repo-display :repoId="commit.repoID"></inline-repo-display>
                    <br />
                    <span class="mr-2 hash">{{ commit.hash }}</span>
                  </v-col>
                </v-row>
              </v-container>
            </v-toolbar>
            <v-card-text class="body-1">
              <span class="font-weight-bold">{{ commit.author }}</span> authored at
              <span
                :title="formattedDateUTC(commit.authorDate)"
              >{{ formattedDate(commit.authorDate) }}</span>
              <br />
              <span class="font-weight-bold">{{ commit.committer }}</span> committed at
              <span
                :title="formattedDateUTC(commit.committerDate)"
              >{{ formattedDate(commit.committerDate) }}</span>
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
import { Commit, Run } from '../store/types'
import { Prop } from 'vue-property-decorator'
import InlineMinimalRepoNameDisplay from './InlineMinimalRepoDisplay.vue'
import CommitChip from './CommitChip.vue'
import { formatDate, formatDateUTC } from '@/util/TimeUtil'

@Component({
  components: {
    'inline-repo-display': InlineMinimalRepoNameDisplay,
    'commit-chip': CommitChip
  }
})
export default class CommitInformation extends Vue {
  @Prop()
  private run!: Run

  private formattedDate(date: number) {
    return formatDate(date)
  }

  private formattedDateUTC(date: number) {
    return formatDateUTC(date)
  }

  private get commit(): Commit {
    return this.run.commit
  }

  private get commitSummary(): string {
    if (!this.commit.summary) {
      return 'Commit has no message :('
    }
    return this.commit.summary
  }

  private get restOfCommitMessage() {
    if (!this.commit.message) {
      return ''
    }
    return this.commit.bodyWithoutSummary
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
</style>
