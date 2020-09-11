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
                  :allCommits="allCommits"
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
                :items="commitHistory"
                :hide-default-footer="
                  commitHistory.length < defaultItemsPerPage
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
                      v-for="({ commit, comparison }, index) in props.items"
                      :key="index"
                    >
                      <run-overview
                        v-if="comparison.second"
                        :run="comparison.second"
                        :commit="commit"
                      ></run-overview>
                      <commit-overview-base v-else :commit="commit">
                        <template #avatar>
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
                        </template>
                        <template #actions v-if="isAdmin">
                          <commit-benchmark-actions
                            :hasExistingBenchmark="false"
                            :commit="commit"
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
import { Repo, Commit } from '@/store/types'
import { vxm } from '@/store'
import { mdiHelpCircleOutline, mdiChevronUp, mdiChevronDown } from '@mdi/js'
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
  private repo!: Repo

  private showList: boolean = false
  private upIcon = mdiChevronUp
  private downIcon = mdiChevronDown

  private firstCommit: Commit | string | null | undefined = null
  private secondCommit: Commit | string | null | undefined = null

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
    return this.firstCommit != null && this.firstCommit !== undefined
  }

  private get commitHistory() {
    // return vxm.detailGraphModule.repoHistory
    return []
  }

  private get allCommits(): Commit[] {
    return []
    // return vxm.detailGraphModule.repoHistory.map(
    //   (datapoint: { commit: Commit; comparison: CommitComparison }) => {
    //     return datapoint.commit
    //   }
    // )
  }

  private get secondCommitCandidates(): Commit[] {
    return this.allCommits.filter((commit: Commit) => {
      let first = this.firstCommit
      if (first !== null && first !== undefined) {
        return commit.hash !== this.firstCommitHash
      }
    })
  }

  private get isAdmin() {
    return vxm.userModule.isAdmin
  }

  filterCommits(item: Commit, queryText: any, itemText: any) {
    return (
      item.hash.toLocaleLowerCase().indexOf(queryText.toLocaleLowerCase()) >
        -1 ||
      (item.message &&
        item.message
          .toLocaleLowerCase()
          .indexOf(queryText.toLocaleLowerCase()) > -1)
    )
  }

  navigateToComparison() {
    if (this.firstCommitHash && this.secondCommitHash) {
      this.$router.push({
        name: 'commit-comparison',
        params: {
          repoID: this.repo.id,
          hashOne: this.firstCommitHash,
          hashTwo: this.secondCommitHash
        }
      })
    }
  }

  // ============== ICONS ==============
  private notBenchmarkedIcon = mdiHelpCircleOutline
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
