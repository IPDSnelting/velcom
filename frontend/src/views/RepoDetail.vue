<template>
  <v-container v-if="repoExists(id)">
    <v-row>
      <v-col>
        <v-card>
          <v-card-title>
            <v-toolbar color="primary darken-1" dark>
              {{ repo.name }}
              <span class="ml-5 subtitle-1">{{ repo.id }}</span>
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
                <v-col cols="9">
                  <v-tooltip top v-for="branch in branches" :key="branch">
                    <template v-slot:activator="{ on }">
                      <v-chip
                        class="ma-2"
                        outlined
                        label
                        v-on="on"
                        :color="isBranchTracked(branch) ? 'success' : 'error'"
                      >{{ branch }}</v-chip>
                    </template>
                    {{ isBranchTracked(branch) ? 'Tracked' : 'Not Tracked' }}
                  </v-tooltip>
                </v-col>
              </v-row>
            </v-container>
          </v-card-text>
          <v-card-actions v-if="canEdit">
            <v-spacer></v-spacer>
            <repo-update :repoId="id">
              <template #activator="{ on }">
                <v-btn v-on="on" color="primary">update</v-btn>
              </template>
            </repo-update>
            <v-btn
              color="error"
              class="mr-5 ml-3"
              outlined
              text
              @click="deleteRepository"
            >Delete Repository</v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>
    <v-row align="baseline" justify="center">
      <v-col>
        <v-card>
          <v-card-title>
            <v-toolbar color="primary darken-1" dark>Filter Data</v-toolbar>
          </v-card-title>
          <v-card-text>
            <v-container fluid>
              <v-row align="start" justify="space-around">
                <v-col md="5" sm="12" xs="12">
                  <v-row>
                    <v-select
                      class="mr-5"
                      :items="occuringBenchmarks"
                      v-model="selectedBenchmark"
                      label="benchmark"
                    ></v-select>
                    <v-select
                      class="mr-5"
                      :items="metricsForBenchmark(this.selectedBenchmark)"
                      v-model="selectedMetric"
                      label="metric"
                    ></v-select>
                  </v-row>
                </v-col>
                <v-col md="5" sm="12" xs="12">
                  <v-form v-model="formValid" ref="form">
                    <template>
                      <v-row>
                        <v-text-field
                          @blur="retrieveRuns"
                          @keyup.enter="retrieveRuns"
                          v-model="amount"
                          :rules="[nonEmptyRunAmount, nonNegativeRunAmount, onlyNumericInput]"
                          label="number of commits to fetch"
                          class="mr-5"
                        ></v-text-field>
                        <v-text-field
                          @blur="retrieveRuns"
                          @keyup.enter="retrieveRuns"
                          v-model="skip"
                          :rules="[nonEmptyRunAmount, nonNegativeRunAmount, onlyNumericInput]"
                          label="number of commits to skip"
                          class="mr-5"
                        ></v-text-field>
                      </v-row>
                    </template>
                  </v-form>
                </v-col>
              </v-row>
            </v-container>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
    <v-row align="baseline" justify="center">
      <v-col>
        <v-card>
          <v-card-title>
            <v-toolbar color="primary darken-1" dark>Recent commits in this repo</v-toolbar>
          </v-card-title>
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
                        v-for="([commit, commitComparison], index) in props.items"
                        :key="index"
                      >
                        <run-overview v-if="commitComparison.second" :run="commitComparison.second"></run-overview>
                        <commit-overview v-else :commit="commit">
                          <template #avatar>
                            <v-list-item-avatar>
                              <v-tooltip top>
                                <template #activator="{ on }">
                                  <v-icon
                                    v-on="on"
                                    size="32px"
                                    color="gray"
                                  >{{ notBenchmarkedIcon }}</v-icon>
                                </template>
                                This commit was never benchmarked!
                              </v-tooltip>
                            </v-list-item-avatar>
                          </template>
                        </commit-overview>
                      </v-col>
                    </v-row>
                  </template>
                </v-data-iterator>
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
import { Repo } from '@/store/types'
import Component from 'vue-class-component'
import { Watch } from 'vue-property-decorator'
import RepoUpdateDialog from '../components/dialogs/RepoUpdateDialog.vue'
import RunOverview from '../components/overviews/RunOverview.vue'
import { vxm } from '../store/index'
import SmallCommitOverview from '../components/overviews/SmallCommitOverview.vue'
import { mdiHelpCircleOutline } from '@mdi/js'

@Component({
  components: {
    'repo-update': RepoUpdateDialog,
    'run-overview': RunOverview,
    'commit-overview': SmallCommitOverview
  }
})
export default class RepoDetail extends Vue {
  private selectedBenchmark: string = ''
  private selectedMetric: string = ''

  private formValid: boolean = true

  private amount: string = '10'
  private skip: string = '0'

  private itemsPerPageOptions: number[] = [10, 20, 50, 100, 200, -1]
  private defaultItemsPerPage: number = 20

  private get id() {
    return this.$route.params.id
  }

  get occuringBenchmarks(): string[] {
    return vxm.repoModule.occuringBenchmarks
  }

  get metricsForBenchmark(): (benchmark: string) => string[] {
    return (benchmark: string) => vxm.repoModule.metricsForBenchmark(benchmark)
  }

  private get canEdit() {
    return vxm.userModule.authorized(this.id)
  }

  private repoExists(id: string): boolean {
    return vxm.repoModule.repoByID(id) !== undefined
  }

  private isBranchTracked(branch: string): boolean {
    return this.repo.trackedBranches.indexOf(branch) >= 0
  }

  private deleteRepository() {
    let confirmed = window.confirm(
      `Do you really want to delete ${this.repo.name} (${this.id})?`
    )
    if (!confirmed) {
      return
    }
    vxm.repoModule
      .deleteRepo(this.id)
      .then(() => this.$router.push({ name: 'repo-detail-frame' }))
  }

  private get branches() {
    return this.repo.branches
      .slice()
      .sort(
        this.chainComparators(this.comparatorTrackStatus, (a, b) =>
          a.localeCompare(b)
        )
      )
  }

  private get commitHistory() {
    return vxm.repoDetailModule.historyForRepoId(this.id)
  }

  private comparatorTrackStatus(branchA: string, branchB: string) {
    const aTracked = this.isBranchTracked(branchA)
    const bTracked = this.isBranchTracked(branchB)
    if (aTracked && bTracked) {
      return 0
    }
    if (aTracked) {
      return -1
    }
    if (bTracked) {
      return 1
    }
    return 0
  }

  private chainComparators(
    a: (a: string, b: string) => number,
    b: (a: string, b: string) => number
  ): (a: string, b: string) => number {
    return (x, y) => {
      if (a(x, y) !== 0) {
        return a(x, y)
      }
      return b(x, y)
    }
  }

  private get payload(): { repoId: string; amount: number; skip: number } {
    return {
      repoId: this.id,
      amount: Number(this.amount),
      skip: Number(this.skip)
    }
  }

  private nonEmptyRunAmount(input: string): boolean | string {
    return input.length > 0 ? true : 'This field must not be empty!'
  }

  private nonNegativeRunAmount(input: string): boolean | string {
    return Number(input) >= 0 ? true : 'Input must be a non negative number!'
  }

  private onlyNumericInput(input: string): boolean | string {
    return !isNaN(Number(input)) ? true : 'Input must be a number!'
  }

  @Watch('id')
  retrieveRuns() {
    if (this.$refs.form && (this.$refs.form as any).validate()) {
      vxm.repoDetailModule.fetchHistoryForRepo(this.payload)
    }
  }

  private get repo(): Repo {
    return vxm.repoModule.repoByID(this.id)!
  }

  created() {
    vxm.repoDetailModule.fetchHistoryForRepo(this.payload)
  }

  // ============== ICONS ==============
  private notBenchmarkedIcon = mdiHelpCircleOutline
  // ==============       ==============
}
</script>
