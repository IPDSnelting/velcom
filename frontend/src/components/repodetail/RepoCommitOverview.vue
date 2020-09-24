<template>
  <v-card>
    <v-card-title>
      <v-toolbar color="primary darken-1" dark
        >Search and Compare
        <v-spacer></v-spacer>
        <v-btn icon @click="showList = !showList">
          <v-icon>{{ showList ? upIcon : downIcon }}</v-icon>
        </v-btn>
      </v-toolbar>
    </v-card-title>
    <v-expand-transition>
      <div v-if="showList">
        <v-card class="sticky" dense>
          <v-container fluid>
            <v-row align="start" justify="space-around" no-gutters>
              <v-col cols="5">
                <commit-selection
                  label="First Commit"
                  v-model="firstCommit"
                  :allCommits="allCommitDescriptions"
                ></commit-selection>
              </v-col>
              <v-col cols="5">
                <commit-selection
                  label="Second Commit"
                  v-model="secondCommit"
                  :allCommits="secondCommitCandidates"
                  @value="navigateToComparison"
                  :disabled="!firstCommit"
                ></commit-selection>
              </v-col>
            </v-row>
          </v-container>
        </v-card>
        <v-card-text>
          <v-container fluid>
            <v-row align="center">
              <v-data-iterator
                :items="allCommitDescriptions"
                :hide-default-footer="
                  allCommitDescriptions.length < defaultItemsPerPage
                "
                :items-per-page="defaultItemsPerPage"
                :footer-props="{ itemsPerPageOptions: itemsPerPageOptions }"
                style="width: 100%"
              >
                <template v-slot:default="props">
                  <v-row>
                    <v-col
                      cols="12"
                      class="my-1 py-0"
                      v-for="(item, index) in props.items"
                      :key="index"
                    >
                      <commit-overview-base :commit="item">
                        <template #avatar>
                          <v-list-item-avatar>
                            <v-tooltip top v-if="!item.failed">
                              <template #activator="{ on }">
                                <v-icon v-on="on" size="32px" color="success">{{
                                  successIcon
                                }}</v-icon>
                              </template>
                              There is a successful run for this commit :)
                            </v-tooltip>
                            <v-tooltip top v-else>
                              <template #activator="{ on }">
                                <v-icon
                                  :color="'error'"
                                  v-on="on"
                                  size="32px"
                                  >{{ errorIcon }}</v-icon
                                >
                              </template>
                              <span
                                >There is no successful run for this commit
                                :(</span
                              >
                            </v-tooltip>
                          </v-list-item-avatar>
                          <!--
                          <v-list-item-avatar>
                            <v-tooltip top>
                              <template #activator="{ on }">
                                <v-icon v-on="on" size="32px" color="gray">{{
                                  notBenchmarkedIcon
                                }}</v-icon>
                              </template>
                              This commit was never benchmarked!
                            </v-tooltip>
                          </v-list-item-avatar>
-->
                        </template>
                        <template #actions>
                          <commit-benchmark-actions
                            :hasExistingBenchmark="false"
                            :commit-description="item"
                          ></commit-benchmark-actions>
                        </template>
                      </commit-overview-base>
                    </v-col>
                  </v-row>
                </template>
              </v-data-iterator>
            </v-row>
          </v-container>
        </v-card-text>
      </div>
    </v-expand-transition>
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import {
  Commit,
  CommitDescription,
  DetailDataPoint,
  RepoId
} from '@/store/types'
import { vxm } from '@/store'
import {
  mdiChevronDown,
  mdiChevronUp,
  mdiHelpCircleOutline,
  mdiCheckCircleOutline,
  mdiCloseCircleOutline
} from '@mdi/js'
import CommitOverviewBase from '@/components/overviews/CommitOverviewBase.vue'
import CommitBenchmarkActions from '@/components/CommitBenchmarkActions.vue'
import RunOverview from '@/components/overviews/RunOverview.vue'
import CommitSelectionComponent from '../CommitSelectionComponent.vue'

@Component({
  components: {
    'commit-overview-base': CommitOverviewBase,
    'run-overview': RunOverview,
    'commit-benchmark-actions': CommitBenchmarkActions,
    'commit-selection': CommitSelectionComponent
  }
})
export default class RepoCommitOverview extends Vue {
  private itemsPerPageOptions: number[] = [10, 20, 50, 100, 200, -1]
  private defaultItemsPerPage: number = 20

  @Prop()
  private repo!: RepoId

  private showList: boolean = false
  private upIcon = mdiChevronUp
  private downIcon = mdiChevronDown

  private firstCommit: CommitDescription | string | null | undefined = null
  private secondCommit: CommitDescription | string | null | undefined = null

  private get firstCommitHash(): string | null {
    if (!this.firstCommit) {
      return null
    }
    if (typeof this.firstCommit === 'string') {
      return this.firstCommit
    }
    return this.firstCommit.hash
  }

  private get secondCommitHash(): string | null {
    if (!this.secondCommit) {
      return null
    }
    if (typeof this.secondCommit === 'string') {
      return this.secondCommit
    }
    return this.secondCommit.hash
  }

  private get firstChosen(): boolean {
    return this.firstCommit !== null && this.firstCommit !== undefined
  }

  private get allCommitDescriptions(): CommitDescription[] {
    return vxm.detailGraphModule.detailGraph.map(
      (datapoint: DetailDataPoint) => {
        return new CommitDescription(
          this.repo,
          datapoint.hash,
          datapoint.author,
          datapoint.authorDate,
          datapoint.summary
        )
      }
    )
  }

  private get secondCommitCandidates(): CommitDescription[] {
    return this.allCommitDescriptions.filter((commit: CommitDescription) => {
      const first = this.firstCommit
      if (first !== null && first !== undefined) {
        return commit.hash !== this.firstCommitHash
      }
    })
  }

  private get isAdmin(): boolean {
    return vxm.userModule.isAdmin
  }

  private filterCommits(item: Commit, queryText: any): boolean | '' {
    return (
      item.hash.toLocaleLowerCase().indexOf(queryText.toLocaleLowerCase()) >
        -1 ||
      (item.message &&
        item.message
          .toLocaleLowerCase()
          .indexOf(queryText.toLocaleLowerCase()) > -1)
    )
  }

  private navigateToComparison() {
    if (this.firstCommitHash && this.secondCommitHash) {
      this.$router.push({
        name: 'run-comparison',
        params: { first: this.repo, second: this.repo },
        query: { hash1: this.firstCommitHash, hash2: this.secondCommitHash }
      })
    }
  }

  // ============== ICONS ==============
  private notBenchmarkedIcon = mdiHelpCircleOutline
  private successIcon = mdiCheckCircleOutline
  private errorIcon = mdiCloseCircleOutline
  // ==============       ==============
}
</script>

<style scoped>
.sticky {
  position: sticky;
  top: 0;
  z-index: 2;
}
</style>
