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
          <detail-graph
            :measurements="selectedMeasurements"
            :amount="Number.parseInt(amount)"
            :beginYAtZero="this.yScaleBeginsAtZero"
            @selectionChanged="updateSelection"
          ></detail-graph>
        </v-card>
      </v-col>
    </v-row>
    <v-row align="baseline" justify="center">
      <v-col>
        <v-card>
          <v-card-text class="ma-0 pa-0">
            <v-container fluid class="ma-0 px-4 pb-0">
              <v-row align="center" justify="space-around" no-gutters>
                <v-col md="5" sm="12" cols="12">
                  <measurement-id-selection v-model="selectedMeasurements" :repoId="id"></measurement-id-selection>
                </v-col>
                <v-col md="5" sm="12" cols="12">
                  <v-form v-model="formValid" ref="form">
                    <template>
                      <v-row no-gutters>
                        <v-col>
                          <v-text-field
                            @blur="retrieveRuns"
                            @keyup.enter="retrieveRuns"
                            v-model="amount"
                            :rules="[nonEmptyRunAmount, nonNegativeRunAmount, onlyNumericInput, noIntegerOverflow]"
                            label="number of commits to fetch"
                            class="mr-5"
                          ></v-text-field>
                        </v-col>
                        <v-col>
                          <v-text-field
                            @blur="retrieveRuns"
                            @keyup.enter="retrieveRuns"
                            v-model="skip"
                            :rules="[nonEmptyRunAmount, nonNegativeRunAmount, onlyNumericInput, noIntegerOverflow]"
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
          <v-card-actions class="mx-2">
            <v-row no-gutters justify="end">
              <v-col v-show="referenceCommitSelected">
                <v-row no-gutters>
                  <v-col cols="12">
                    <span class="hint">reference line commit</span>
                  </v-col>
                  <v-col>
                    <commit-chip
                      class="mr-2"
                      :to="{ name: 'commit-detail', params: { repoID: id, hash: referenceCommit } }"
                      :commitHash="referenceCommit"
                      :copyOnClick="false"
                    ></commit-chip>
                  </v-col>
                </v-row>
              </v-col>
              <v-col cols="auto" v-if="lockedToRelativeCommit">
                <commit-selection
                  :commit="relativeToCommit"
                  @value="relativeToCommit = typeof $event === 'string' ? $event : $event.hash"
                  label="Reference frame anchor"
                  :allCommits="allCommits"
                ></commit-selection>
              </v-col>
              <v-col cols="auto" align-self="end" class="mt-3">
                <v-tooltip top v-if="!lockedToRelativeCommit">
                  <template #activator="{ on }">
                    <v-btn
                      v-on="on"
                      text
                      color="primary"
                      @click="lockReferenceFrame"
                    >Lock reference frame</v-btn>
                  </template>
                  Make skip relative to the anchor commit. Good for permanent links, as it never changes!
                </v-tooltip>
                <v-tooltip top v-if="lockedToRelativeCommit">
                  <template #activator="{ on }">
                    <v-btn
                      v-on="on"
                      text
                      color="primary"
                      @click="unlockReferenceFrame"
                    >Unlock reference frame</v-btn>
                  </template>
                  Make skip relative to the latest. Good for staying up to date!
                </v-tooltip>
              </v-col>
              <v-col cols="auto" align-self="end">
                <v-btn text color="primary" @click="toggleYScale()">{{ yScaleButtonLabel }}</v-btn>
              </v-col>
              <v-col cols="auto" align-self="end">
                <v-btn
                  color="error"
                  text
                  :disabled="!selectedMeasurements"
                  @click="deleteMetric"
                >Delete metric</v-btn>
              </v-col>
            </v-row>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>
    <v-row align="baseline" justify="center">
      <v-col>
        <repo-commit-overview :repo="repo"></repo-commit-overview>
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
import { Route, RawLocation } from 'vue-router'
import { Dictionary } from 'vue-router/types/router'
import CommitChip from '../components/CommitChip.vue'
import CommitSelectionComponent from '../components/CommitSelectionComponent.vue'
import MeasurementIdSelection from '../components/graphs/MeasurementIdSelection.vue'

@Component({
  components: {
    'repo-base-information': RepoBaseInformation,
    'repo-commit-overview': RepoCommitOverview,
    'detail-graph': DetailGraph,
    'commit-chip': CommitChip,
    'commit-selection': CommitSelectionComponent,
    'measurement-id-selection': MeasurementIdSelection
  }
})
export default class RepoDetail extends Vue {
  private formValid: boolean = true

  private get referenceCommit(): string {
    return vxm.repoDetailModule.referenceDatapoint
      ? vxm.repoDetailModule.referenceDatapoint.commit.hash
      : ''
  }

  private get referenceCommitSelected(): boolean {
    return vxm.repoDetailModule.referenceDatapoint !== null
  }

  private yScaleBeginsAtZero: boolean = true
  private yScaleButtonLabel:
    | 'begin y-Scale at zero'
    | 'begin y-Scale at minimum Value' = 'begin y-Scale at minimum Value'

  private get selectedMeasurement(): MeasurementID {
    return this.selectedMeasurements[0]
      ? this.selectedMeasurements[0]
      : new MeasurementID('', '')
  }

  private get selectedMeasurements(): MeasurementID[] {
    return vxm.repoDetailModule.selectedMeasurements
  }

  private set selectedMeasurements(selectedMeasurements: MeasurementID[]) {
    vxm.repoDetailModule.selectedMeasurements = selectedMeasurements.slice()
  }

  private get relativeToCommit(): string {
    return vxm.repoDetailModule.relativeToCommit
  }

  private set relativeToCommit(commitHash: string) {
    vxm.repoDetailModule.relativeToCommit = commitHash
    if (this.lockedToRelativeCommit) {
      vxm.repoDetailModule.fetchHistoryForRepo(this.payload)
    }
  }

  private get amount() {
    return vxm.repoDetailModule.selectedFetchAmount
  }

  private set amount(amount: string) {
    vxm.repoDetailModule.selectedFetchAmount = amount
  }

  private get skip() {
    return vxm.repoDetailModule.selectedSkipAmount
  }

  private set skip(skip: string) {
    vxm.repoDetailModule.selectedSkipAmount = skip
  }

  private get lockedToRelativeCommit(): boolean {
    return vxm.repoDetailModule.lockedToRelativeCommit
  }

  private set lockedToRelativeCommit(lockedToRelativeCommit: boolean) {
    vxm.repoDetailModule.lockedToRelativeCommit = lockedToRelativeCommit
  }

  private get allCommits(): Commit[] {
    return vxm.repoDetailModule.repoHistory.map(it => it.commit)
  }

  private get id() {
    return this.$route.params.id
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

  private noIntegerOverflow(input: string): boolean | string {
    let value = Math.abs(Number(input))

    if (value >= Math.pow(2, 31) - 1) {
      return 'Input is too large!'
    }
    return true
  }

  private async lockReferenceFrame() {
    this.relativeToCommit = vxm.repoDetailModule.repoHistory[0].commit.hash
    let { index } = await vxm.repoDetailModule.fetchIndexOfCommit({
      repoId: this.repo.id,
      commitHash: this.relativeToCommit
    })
    this.lockedToRelativeCommit = true
    this.skip = '0'
    vxm.repoDetailModule.fetchHistoryForRepo(this.payload)
  }

  private async unlockReferenceFrame() {
    vxm.repoDetailModule
      .fetchIndexOfCommit({
        repoId: this.repo.id,
        commitHash: this.relativeToCommit
      })
      .then(({ index }) => {
        this.skip = '' + (parseInt(this.skip) + index)
      })
      .finally(() => {
        this.lockedToRelativeCommit = false
        vxm.repoDetailModule.fetchHistoryForRepo(this.payload)
      })
  }

  private toggleYScale() {
    this.yScaleBeginsAtZero = !this.yScaleBeginsAtZero
    if (this.yScaleButtonLabel === 'begin y-Scale at zero') {
      this.yScaleButtonLabel = 'begin y-Scale at minimum Value'
    } else {
      this.yScaleButtonLabel = 'begin y-Scale at zero'
    }
  }

  private deleteMetric() {
    if (!this.selectedMeasurements) {
      return
    }
    let measurement = this.selectedMeasurements[0]
    if (
      window.confirm(
        `Do you really want to delete metric '${measurement.metric}' in '${measurement.benchmark}'?
         This will also delete all measurements for this metric!`
      )
    ) {
      vxm.repoDetailModule.dispatchDeleteMeasurements({
        measurementId: measurement,
        repoId: this.id
      })
    }
  }

  @Watch('id')
  @Watch('selectedMeasurements')
  retrieveRuns() {
    if (this.$refs.form && (this.$refs.form as any).validate()) {
      vxm.repoDetailModule.fetchHistoryForRepo(this.payload)
    }
  }

  @Watch('selectedMeasurements')
  @Watch('skip')
  @Watch('amount')
  @Watch('lockedToRelativeCommit')
  @Watch('relativeToCommit')
  @Watch('yScaleBeginsAtZero')
  updateUrl() {
    let newQuery: { [param: string]: string } = {
      selectedMeasurements: JSON.stringify(this.selectedMeasurements),
      skip: this.skip,
      fetchAmount: this.amount,
      relativeToCommit: this.relativeToCommit,
      lockedToRelativeCommit: this.lockedToRelativeCommit ? 'true' : 'false',
      yScaleBeginsAtZero: this.yScaleBeginsAtZero ? 'true' : 'false'
    }

    history.replaceState(
      {},
      document.title,
      this.$route.path +
        '?' +
        Object.keys(newQuery)
          .map(key => {
            return (
              encodeURIComponent(key) + '=' + encodeURIComponent(newQuery[key])
            )
          })
          .join('&')
    )
  }

  beforeRouteEnter(
    to: Route,
    from: Route,
    next: (to?: RawLocation | false | ((vm: Vue) => any) | void) => void
  ) {
    next(component => {
      let vm = component as RepoDetail
      vm.updateToUrl(to.query)

      vxm.repoDetailModule.fetchHistoryForRepo(vm.payload)
      vm.updateUrl()
    })
  }

  updateSelection(newAmount: number, additionalSkip: number) {
    this.amount = newAmount.toString()
    this.skip = (Number.parseInt(this.skip) + additionalSkip).toString()
    this.retrieveRuns()
  }

  private get repo(): Repo {
    return vxm.repoModule.repoByID(this.id)!
  }

  private updateToUrl(query: Dictionary<string | (string | null)[]>) {
    if (query.skip) {
      this.skip = query.skip as string
    }
    if (query.fetchAmount) {
      this.amount = query.fetchAmount as string
    }
    if (query.selectedMeasurements) {
      let jsonified = JSON.parse(query.selectedMeasurements as string) as {
        metric: string
        benchmark: string
      }[]

      this.selectedMeasurements = jsonified.map(
        ({ metric, benchmark }) => new MeasurementID(benchmark, metric)
      )
    }
    if (query.relativeToCommit) {
      this.relativeToCommit = query.relativeToCommit as string
    }
    if (query.lockedToRelativeCommit) {
      this.lockedToRelativeCommit = query.lockedToRelativeCommit === 'true'
    }
    if (query.yScaleBeginsAtZero) {
      this.yScaleBeginsAtZero = query.yScaleBeginsAtZero === 'true'
    }
  }

  mounted() {
    if (!this.$route.query) {
      this.updateUrl()
    }
  }
}
</script>

<style scoped>
.hint {
  font-size: 14px;
  font-weight: 400;
  color: rgba(0, 0, 0, 0.6);
}
</style>
