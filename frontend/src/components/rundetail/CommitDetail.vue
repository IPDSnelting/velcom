<template>
  <v-container fluid class="pa-0 ma-0">
    <v-row>
      <v-col>
        <v-card>
          <v-card-title>
            <v-toolbar dark color="primary" class="wrapping-toolbar">
              <v-container fluid>
                <v-row no-gutters align="center" justify="space-between">
                  <v-col style="flex: 1 1 60%;">
                    <span class="mx-2 message font-weight-regular">
                      {{ commit.summary }}
                    </span>
                  </v-col>
                  <v-col cols="4" class="ml-3 body-1">
                    <v-spacer></v-spacer>
                    <inline-repo-display
                      :repoId="commit.repoId"
                    ></inline-repo-display>
                    <br />
                    <span class="mr-2 hash">{{ commit.hash }}</span>
                  </v-col>
                  <v-col cols="auto">
                    <commit-benchmark-actions
                      :commit="commit"
                      :hasExistingBenchmark="commit.runs.length > 0"
                    ></commit-benchmark-actions>
                  </v-col>
                </v-row>
              </v-container>
            </v-toolbar>
          </v-card-title>
          <v-card-text class="body-1 mx-2" style="color: inherit;">
            <v-container fluid class="my-0 py-0">
              <v-row justify="space-between" align="center" no-gutters>
                <v-col>
                  <span class="font-weight-bold">{{ commit.author }}</span>
                  authored at
                  <span :title="formatDateUTC(commit.authorDate)">
                    {{ formatDate(commit.authorDate) }}
                  </span>
                  <br />
                  <span class="font-weight-bold">{{ commit.committer }}</span>
                  committed at
                  <span :title="formatDateUTC(commit.committerDate)">
                    {{ formatDate(commit.committerDate) }}
                  </span>
                  <br />
                </v-col>
              </v-row>
              <v-row no-gutters v-if="commit.message.trim()">
                <v-col cols="12">
                  <div class="mt-5 commit-detail-message">
                    {{ commit.message.trim() }}
                  </div>
                </v-col>
              </v-row>
            </v-container>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import { Commit, RunDescription } from '@/store/types'
import CommitInformation from '../CommitInformation.vue'
import InlineMinimalRepoNameDisplay from '../InlineMinimalRepoDisplay.vue'
import CommitBenchmarkActions from '../CommitBenchmarkActions.vue'
import { formatDateUTC, formatDate } from '@/util/TimeUtil'
import RunTimeline from './RunTimeline.vue'

// FIXME: Navigation to parent / child

@Component({
  components: {
    'commit-benchmark-actions': CommitBenchmarkActions,
    'inline-repo-display': InlineMinimalRepoNameDisplay,
    'run-timeline': RunTimeline
  }
})
export default class CommitDetail extends Vue {
  @Prop()
  private commit!: Commit

  private formatDate(date: Date) {
    return formatDate(date)
  }

  private formatDateUTC(date: Date) {
    return formatDateUTC(date)
  }

  private get myRunDummy() {
    return [
      new RunDescription(
        'ID 1',
        new Date(new Date().getTime() - 1000 * 40 * 60),
        'FAILURE'
      ),
      new RunDescription(
        'ID 2',
        new Date(new Date().getTime() - 1000 * 20 * 60),
        'PARTIAL_SUCCESS'
      ),
      new RunDescription(
        'ID 3',
        new Date(new Date().getTime() - 1000 * 0 * 60),
        'SUCCESS'
      )
    ]
  }

  created() {
    // this.commit = new Commit(
    //   'Repo id',
    //   'commit hash',
    //   'I Al Istannen',
    //   new Date(),
    //   'I Al Istannen',
    //   new Date(),
    //   'This is my cool message\nWith a newline!',
    //   'This is my summary. It is long but not too long!!!',
    //   [new RunDescription('this is a run id', new Date(), 'SUCCESS')],
    //   ['parent hash one', 'Parent hash two']
    // )
  }
}
</script>

<style scoped>
.hash,
.commit-detail-message {
  font-family: monospace;
}
.commit-detail-message {
  white-space: pre-line;
}
</style>
