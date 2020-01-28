<template>
  <v-container v-if="repoExists(id)">
    <v-row>
      <v-col>
        <repo-base-information :repo="repo"></repo-base-information>
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
                <v-col md="5" sm="12" cols="12">
                  <v-row>
                    <v-col>
                      <v-select
                        class="mr-5"
                        :items="occuringBenchmarks"
                        v-model="selectedBenchmark"
                        label="benchmark"
                      ></v-select>
                    </v-col>
                    <v-col>
                      <v-select
                        class="mr-5"
                        :items="metricsForBenchmark(this.selectedBenchmark)"
                        v-model="selectedMetric"
                        label="metric"
                      ></v-select>
                    </v-col>
                  </v-row>
                </v-col>
                <v-col md="5" sm="12" cols="12">
                  <v-form v-model="formValid" ref="form">
                    <template>
                      <v-row>
                        <v-col>
                          <v-text-field
                            @blur="retrieveRuns"
                            @keyup.enter="retrieveRuns"
                            v-model="amount"
                            :rules="[nonEmptyRunAmount, nonNegativeRunAmount, onlyNumericInput]"
                            label="number of commits to fetch"
                            class="mr-5"
                          ></v-text-field>
                        </v-col>
                        <v-col>
                          <v-text-field
                            @blur="retrieveRuns"
                            @keyup.enter="retrieveRuns"
                            v-model="skip"
                            :rules="[nonEmptyRunAmount, nonNegativeRunAmount, onlyNumericInput]"
                            label="number of commits to skip"
                            class="mr-5"
                          ></v-text-field>
                        </v-col>
                      </v-row>
                    </template>
                  </v-form>
                </v-col>
              </v-row>
            </v-container>
          </v-card-text>
          <v-card-actions>
            <v-spacer></v-spacer>
            <v-btn
              color="error"
              text
              outlined
              :disabled="!selectedBenchmark || !selectedMetric"
              @click="deleteMetric"
            >Delete metric</v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>
    <v-row align="baseline" justify="center">
      <v-col>
        <<<<<<< HEAD
        <repo-commit-overview :repo="repo"></repo-commit-overview>=======
        <detail-graph :benchmark="selectedBenchmark" :metric="selectedMetric" :amount="amount"></detail-graph>
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
        </v-card>>>>>>>> basic detail graph component
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import { Repo, Commit, MeasurementID } from '@/store/types'
import Component from 'vue-class-component'
import { Watch } from 'vue-property-decorator'
import { vxm } from '../store/index'
import RepoBaseInformation from '@/components/repodetail/RepoBaseInformation.vue'
import RepoCommitOverview from '@/components/repodetail/RepoCommitOverview.vue'
import DetailGraph from '@/components/graphs/DetailGraph.vue'

@Component({
  components: {
    'repo-base-information': RepoBaseInformation,
    'repo-commit-overview': RepoCommitOverview,
    'detail-graph': DetailGraph
  }
})
export default class RepoDetail extends Vue {
  private selectedBenchmark: string = ''
  private selectedMetric: string = ''

  private formValid: boolean = true

  private amount: string = '10'
  private skip: string = '0'

  private get id() {
    return this.$route.params.id
  }

  get occuringBenchmarks(): string[] {
    return vxm.repoModule.occuringBenchmarks([this.id])
  }

  get metricsForBenchmark(): (benchmark: string) => string[] {
    return (benchmark: string) => vxm.repoModule.metricsForBenchmark(benchmark)
  }

  private repoExists(id: string): boolean {
    return vxm.repoModule.repoByID(id) !== undefined
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

  private deleteMetric() {
    if (!this.selectedBenchmark || !this.selectedMetric) {
      return
    }
    if (
      window.confirm(
        `Do you really want to delete metric '${this.selectedMetric}' in '${this.selectedBenchmark}'?
         This will also delete all measurements for this metric!`
      )
    ) {
      vxm.repoDetailModule.dispatchDeleteMeasurements({
        measurementId: new MeasurementID(
          this.selectedBenchmark,
          this.selectedMetric
        ),
        repoId: this.id
      })
    }
  }

  @Watch('id')
  @Watch('selectedMetric')
  @Watch('selectedBenchmark')
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
}
</script>
