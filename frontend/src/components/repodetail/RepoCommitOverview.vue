<template>
  <v-card>
    <v-card-title>
      <v-toolbar color="primary darken-1" dark>Recent commits in this repo</v-toolbar>
    </v-card-title>
    <v-card class="sticky">
      <v-card-title>Compare:</v-card-title>
      <v-card-text>
        <v-container fluid>
          <v-row align="start" justify="space-around" no-gutters>
            <v-col cols="5">
              <v-autocomplete
                v-model="firstHash"
                :items="allCommits"
                :filter="filterCommits"
                item-value="hash"
                item-text="hash"
                label="first commit"
                placeholder="Search for a hash or message"
                auto-select-first
                hide-no-data
                hide-selected
                clearable
              >
                <template v-slot:item="data">
                  <v-list-item-content>
                    <v-list-item-title>{{ data.item.hash }}</v-list-item-title>
                    <v-list-item-subtitle>{{ data.item.message }}</v-list-item-subtitle>
                  </v-list-item-content>
                </template>
              </v-autocomplete>
            </v-col>
            <v-col cols="5">
              <v-autocomplete
                v-model="secondHash"
                :items="allCommits"
                :disabled="firstHashEntered"
                item-value="hash"
                item-text="hash"
                label="second commit"
                placeholder="Search for a hash or message"
                auto-select-first
                hide-no-data
                hide-selected
                clearable
                @input="navigateToComparison"
              >
                <template v-slot:item="data">
                  <v-list-item-content>
                    <v-list-item-title>{{ data.item.hash }}</v-list-item-title>
                    <v-list-item-subtitle>{{ data.item.message }}</v-list-item-subtitle>
                  </v-list-item-content>
                </template>
              </v-autocomplete>
            </v-col>
          </v-row>
        </v-container>
      </v-card-text>
    </v-card>
    <v-card-text>
      <v-container fluid>
        <v-row align="center">
          <v-data-iterator
            :items="commitHistory"
            :hide-default-footer="commitHistory.length < defaultItemsPerPage"
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
                    :hideActions="!isAdmin"
                  ></run-overview>
                  <commit-overview-base v-else :commit="commit">
                    <template #avatar>
                      <v-list-item-avatar>
                        <v-tooltip top>
                          <template #activator="{ on }">
                            <v-icon v-on="on" size="32px" color="gray">{{ notBenchmarkedIcon }}</v-icon>
                          </template>
                          This commit was never benchmarked!
                        </v-tooltip>
                      </v-list-item-avatar>
                    </template>
                    <template #actions v-if="isAdmin">
                      <commit-benchmark-actions
                        :hasExistingBenchmark="false"
                        @benchmark="benchmark(commit)"
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
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import { Repo, Commit, CommitComparison } from '@/store/types'
import { vxm } from '@/store'
import { mdiHelpCircleOutline } from '@mdi/js'
import CommitOverviewBase from '@/components/overviews/CommitOverviewBase.vue'
import CommitBenchmarkActions from '@/components/CommitBenchmarkActions.vue'
import RunOverview from '@/components/overviews/RunOverview.vue'

@Component({
  components: {
    'commit-overview-base': CommitOverviewBase,
    'run-overview': RunOverview,
    'commit-benchmark-actions': CommitBenchmarkActions
  }
})
export default class RepoCommitOverview extends Vue {
  private itemsPerPageOptions: number[] = [10, 20, 50, 100, 200, -1]
  private defaultItemsPerPage: number = 20

  @Prop()
  private repo!: Repo

  private firstHash: string = ''
  private secondHash: string = ''

  private get firstHashEntered(): boolean {
    return this.firstHash === ''
  }

  private get commitHistory() {
    return vxm.repoDetailModule.historyForRepoId(this.repo.id)
  }

  private get allCommits() {
    return vxm.repoDetailModule
      .historyForRepoId(this.repo.id)
      .map((datapoint: { commit: Commit; comparison: CommitComparison }) => {
        return datapoint.commit
      })
  }

  private get isAdmin() {
    return vxm.userModule.isAdmin
  }

  private benchmark(commit: Commit) {
    vxm.queueModule.dispatchPrioritizeOpenTask(commit)
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
    this.$router.push({
      name: 'commit-comparison',
      params: {
        repoID: this.repo.id,
        hashOne: this.firstHash,
        hashTwo: this.secondHash
      }
    })
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
