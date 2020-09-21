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
                      :commitDescription="commit"
                      :hasExistingBenchmark="commit.runs.length > 0"
                    ></commit-benchmark-actions>
                  </v-col>
                </v-row>
              </v-container>
            </v-toolbar>
          </v-card-title>
          <v-card-text class="body-1" style="color: inherit;">
            <v-container fluid class="my-0 py-0">
              <v-row
                v-for="({ parent, child }, index) in navigationTargets"
                :key="index"
                justify="space-between"
                no-gutters
              >
                <v-col cols="6" class="pr-4">
                  <div v-if="parent" class="d-flex justify-start">
                    <commit-navigation-button
                      :commitDescription="parent"
                      type="PARENT"
                    ></commit-navigation-button>
                  </div>
                  <v-spacer v-else></v-spacer>
                </v-col>
                <v-col cols="6">
                  <div v-if="child" class="d-flex justify-end">
                    <commit-navigation-button
                      :commitDescription="child"
                      type="CHILD"
                    ></commit-navigation-button>
                  </div>
                  <v-spacer v-else></v-spacer>
                </v-col>
              </v-row>
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
import { Commit, CommitDescription } from '@/store/types'
import { formatDateUTC, formatDate } from '@/util/TimeUtil'
import InlineMinimalRepoNameDisplay from '../InlineMinimalRepoDisplay.vue'
import CommitBenchmarkActions from '../CommitBenchmarkActions.vue'
import { mdiArrowLeft, mdiArrowRight } from '@mdi/js'
import CommitNavigationButton from './CommitNavigationButton.vue'

class NavigationTarget {
  readonly parent: CommitDescription | null
  readonly child: CommitDescription | null

  constructor(
    parent: CommitDescription | null,
    child: CommitDescription | null
  ) {
    this.parent = parent
    this.child = child
  }
}

@Component({
  components: {
    'commit-benchmark-actions': CommitBenchmarkActions,
    'inline-repo-display': InlineMinimalRepoNameDisplay,
    'commit-navigation-button': CommitNavigationButton
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

  private get navigationTargets(): NavigationTarget[] {
    const targets: NavigationTarget[] = []
    let mutualItems = Math.min(
      this.commit.parents.length,
      this.commit.children.length
    )
    let i = 0
    for (; i < mutualItems; i++) {
      targets.push(
        new NavigationTarget(this.commit.parents[i], this.commit.children[i])
      )
    }

    if (i === this.commit.parents.length) {
      for (; i < this.commit.children.length; i++) {
        targets.push(new NavigationTarget(null, this.commit.children[i]))
      }
    } else {
      for (; i < this.commit.parents.length; i++) {
        targets.push(new NavigationTarget(this.commit.parents[i], null))
      }
    }

    return targets
  }

  // Icons
  private parentCommitIcon = mdiArrowLeft
  private childCommitIcon = mdiArrowRight
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
