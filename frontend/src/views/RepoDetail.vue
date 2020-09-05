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
          <v-row align="center" no-gutters>
            <v-col>
              <v-btn-toggle
                mandatory
                :value="graphFlavours.indexOf(currentFlavour)"
              >
                <v-btn
                  group
                  tile
                  text
                  v-for="flavour in graphFlavours"
                  v-bind:key="flavour.name"
                  v-on:click="currentFlavour = flavour"
                >
                  {{ flavour.name }}
                </v-btn>
              </v-btn-toggle>
            </v-col>
            <v-spacer></v-spacer>
            <v-col>
              <v-switch
                v-model="dynamicFlavourSwitching"
                label="switch dynamically"
              ></v-switch>
            </v-col>
          </v-row>
          <v-card flat>
            <!-- <component
              v-bind:is="currentFlavour.component"
              :measurements="selectedMeasurements"
              :amount="Number.parseInt(amount)"
              :beginYAtZero="this.yScaleBeginsAtZero"
              @selectionChanged="updateSelection"
            ></component> -->
          </v-card>
        </v-card>
      </v-col>
    </v-row>
    <v-row align="baseline" justify="center">
      <v-col>
        <v-card>
          <v-card-text class="ma-0 pa-0">
            <v-container fluid class="ma-0 px-5 pb-0">
              <v-row align="center" justify="space-between" no-gutters>
                <v-col :md="useMatrixSelector ? '' : '5'" sm="12" cols="12">
                  <v-btn
                    @click="useMatrixSelector = !useMatrixSelector"
                    text
                    color="primary"
                  >
                    <span v-if="useMatrixSelector">Use tree selector</span>
                    <span v-if="!useMatrixSelector">Use matrix selector</span>
                  </v-btn>
                  <matrix-measurement-id-selection
                    v-if="useMatrixSelector"
                    @input="selectedMeasurements = $event"
                    :selectedMeasurements="selectedMeasurements"
                    :repoId="id"
                  ></matrix-measurement-id-selection>
                  <normal-measurement-id-selection
                    v-if="!useMatrixSelector"
                    @input="selectedMeasurements = $event"
                    :selectedMeasurements="selectedMeasurements"
                    :repoId="id"
                  ></normal-measurement-id-selection>
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
                            :rules="[
                              nonEmptyRunAmount,
                              nonNegativeRunAmount,
                              onlyNumericInput,
                              noIntegerOverflow
                            ]"
                            label="number of commits to fetch"
                            class="mr-5"
                          ></v-text-field>
                        </v-col>
                        <v-col>
                          <v-text-field
                            @blur="retrieveRuns"
                            @keyup.enter="retrieveRuns"
                            v-model="skip"
                            :rules="[
                              nonEmptyRunAmount,
                              nonNegativeRunAmount,
                              onlyNumericInput,
                              noIntegerOverflow
                            ]"
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
          <v-card-actions class="mx-5 px-0">
            <v-row no-gutters justify="end">
              <v-col v-show="referenceCommitSelected">
                <v-row no-gutters>
                  <v-col cols="12">
                    <span class="hint">reference line commit</span>
                  </v-col>
                  <v-col>
                    <commit-chip
                      class="mr-2"
                      :to="{
                        name: 'commit-detail',
                        params: { repoID: id, hash: referenceCommit }
                      }"
                      :commitHash="referenceCommit"
                      :copyOnClick="false"
                    ></commit-chip>
                  </v-col>
                </v-row>
              </v-col>
              <v-col cols="auto" v-if="lockedToRelativeCommit">
                <commit-selection
                  :commit="relativeToCommit"
                  @value="
                    relativeToCommit =
                      typeof $event === 'string' ? $event : $event.hash
                  "
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
                      >Lock reference frame</v-btn
                    >
                  </template>
                  Make skip relative to the anchor commit. Good for permanent
                  links, as it never changes!
                </v-tooltip>
                <v-tooltip top v-if="lockedToRelativeCommit">
                  <template #activator="{ on }">
                    <v-btn
                      v-on="on"
                      text
                      color="primary"
                      @click="unlockReferenceFrame"
                      >Unlock reference frame</v-btn
                    >
                  </template>
                  Make skip relative to the latest. Good for staying up to date!
                </v-tooltip>
              </v-col>
              <v-col cols="auto" align-self="end">
                <v-btn text color="primary" @click="toggleYScale()">{{
                  yScaleButtonLabel
                }}</v-btn>
              </v-col>
              <v-col cols="auto" align-self="end" v-if="isAdmin">
                <v-tooltip top>
                  <template #activator="{ on }">
                    <v-btn
                      v-on="on"
                      :color="canDeleteMetric ? 'error' : '#d3d3d3'"
                      text
                      @click="deleteMetric"
                      >Delete metric</v-btn
                    >
                  </template>
                  <span v-if="!canDeleteMetric"
                    >Please only select a single metric.</span
                  >
                  <span v-else>
                    Deletes metric '
                    <span class="font-weight-bold">{{
                      selectedMeasurements[0].benchmark
                    }}</span>
                    —
                    <span class="font-weight-bold">{{
                      selectedMeasurements[0].metric
                    }}</span
                    >'
                  </span>
                </v-tooltip>
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
import { Repo, Dimension, CommitHash } from '@/store/types'
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
import MatrixMeasurementIdSelection from '../components/graphs/MatrixMeasurementIdSelection.vue'
import DytailGraph from '../components/graphs/Dygraph-Detail.vue'
import EchartsGraph from '@/components/graphs/ECharts-Detail.vue'

type graphFlavour = {
  name: string
  component: typeof Vue
}

@Component({
  components: {
    'repo-base-information': RepoBaseInformation,
    'repo-commit-overview': RepoCommitOverview,
    /*     'detail-graph': DetailGraph,
    'dytail-graph': DytailGraph,
    'echarts-graph': EchartsGraph, */
    'commit-chip': CommitChip,
    'commit-selection': CommitSelectionComponent,
    'matrix-measurement-id-selection': MatrixMeasurementIdSelection,
    'normal-measurement-id-selection': MeasurementIdSelection
  }
})
export default class RepoDetail extends Vue {
  private formValid: boolean = true

  private useMatrixSelector: boolean = false

  private graphFlavours: graphFlavour[] = [
    {
      name: 'd3',
      component: DetailGraph
    },
    {
      name: 'dygraphs',
      component: DytailGraph
    },
    {
      name: 'echarts',
      component: EchartsGraph
    }
  ]

  private currentFlavour: graphFlavour = this.graphFlavours[0]

  private dynamicFlavourSwitching: boolean = true

  private get referenceCommit(): string {
    return vxm.detailGraphModule.referenceDatapoint
      ? vxm.detailGraphModule.referenceDatapoint.dataPoint.hash
      : ''
  }

  private get referenceCommitSelected(): boolean {
    return vxm.detailGraphModule.referenceDatapoint !== null
  }

  private yScaleBeginsAtZero: boolean = true
  private yScaleButtonLabel:
    | 'begin y-Scale at zero'
    | 'begin y-Scale at minimum Value' = 'begin y-Scale at minimum Value'

  private get selectedDimensions(): Dimension[] {
    return vxm.detailGraphModule.selectedDimensions
  }

  private set selectedMeasurements(selectedMeasurements: Dimension[]) {
    vxm.detailGraphModule.selectedMeasurements = selectedMeasurements
  }

  private get relativeToCommit(): string {
    return vxm.detailGraphModule.relativeToCommit
  }

  private set relativeToCommit(commitHash: string) {
    vxm.detailGraphModule.relativeToCommit = commitHash
    if (this.lockedToRelativeCommit) {
      vxm.detailGraphModule.fetchDetailGraph(this.payload)
    }
  }

  private get amount() {
    return '' // vxm.detailGraphModule.selectedFetchAmount
  }

  private set amount(amount: string) {
    // vxm.detailGraphModule.selectedFetchAmount = amount
  }

  private get skip() {
    return '' // vxm.detailGraphModule.selectedSkipAmount
  }

  private set skip(skip: string) {
    // vxm.detailGraphModule.selectedSkipAmount = skip
  }

  private get lockedToRelativeCommit(): boolean {
    return vxm.detailGraphModule.lockedToRelativeCommit
  }

  private set lockedToRelativeCommit(lockedToRelativeCommit: boolean) {
    vxm.detailGraphModule.lockedToRelativeCommit = lockedToRelativeCommit
  }

  private get allCommits(): CommitHash[] {
    return vxm.detailGraphModule.detailGraph.map(it => it.hash)
  }

  private get id() {
    return this.$route.params.id
  }

  private repoExists(id: string): boolean {
    return vxm.repoModule.repoById(id) !== undefined
  }

  private get payload(): {
    repoId: string
    startTime: string
    endTime: string
    duration: number
    dimensions: Dimension[]
  } {
    return {
      repoId: this.id,
      startTime: '',
      endTime: '',
      duration: 0,
      dimensions: []
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
    /* this.relativeToCommit = vxm.detailGraphModule.detailGraph[0].hash
    let { index } = await vxm.detailGraphModule.fetchIndexOfCommit({
      repoId: this.repo.id,
      commitHash: this.relativeToCommit
    })
    this.lockedToRelativeCommit = true
    this.skip = '0'
    vxm.detailGraphModule.fetchHistoryForRepo(this.payload) */
  }

  private async unlockReferenceFrame() {
    /* vxm.detailGraphModule
      .fetchIndexOfCommit({
        repoId: this.repo.id,
        commitHash: this.relativeToCommit
      })
      .then(({ index }) => {
        this.skip = '' + (parseInt(this.skip) + index)
      })
      .finally(() => {
        this.lockedToRelativeCommit = false
        vxm.detailGraphModule.fetchHistoryForRepo(this.payload)
      }) */
  }

  private toggleYScale() {
    this.yScaleBeginsAtZero = !this.yScaleBeginsAtZero
    if (this.yScaleButtonLabel === 'begin y-Scale at zero') {
      this.yScaleButtonLabel = 'begin y-Scale at minimum Value'
    } else {
      this.yScaleButtonLabel = 'begin y-Scale at zero'
    }
  }

  private get isAdmin(): boolean {
    return vxm.userModule.isAdmin
  }

  private get canDeleteMetric(): boolean {
    return (
      this.selectedMeasurements &&
      this.selectedMeasurements.length === 1 &&
      this.isAdmin
    )
  }

  private deleteMetric() {
    /* if (!this.canDeleteMetric) {
      return
    }
    let measurement = this.selectedMeasurements[0]
    if (
      window.confirm(
        `Do you really want to delete metric '${measurement.metric}' in '${measurement.benchmark}'?
         This will also delete all measurements for this metric!`
      )
    ) {
      vxm.detailGraphModule.dispatchDeleteMeasurements({
        measurementId: measurement,
        repoId: this.id
      })
    } */
  }

  @Watch('id')
  retrieveRuns(): void {
    if (this.$refs.form && (this.$refs.form as any).validate()) {
      vxm.detailGraphModule.fetchDetailGraph(this.payload)
    }
  }

  @Watch('selectedMeasurements')
  @Watch('skip')
  @Watch('amount')
  @Watch('lockedToRelativeCommit')
  @Watch('relativeToCommit')
  @Watch('yScaleBeginsAtZero')
  updateUrl(): void {
    let newQuery: { [param: string]: string } = {
      selectedMeasurements: JSON.stringify(this.selectedMeasurements),
      skip: this.skip,
      fetchAmount: this.amount,
      relativeToCommit: this.relativeToCommit,
      lockedToRelativeCommit: this.lockedToRelativeCommit ? 'true' : 'false',
      yScaleBeginsAtZero: this.yScaleBeginsAtZero ? 'true' : 'false'
    }

    let url: string = process.env.BASE_URL
    if (url.endsWith('/')) {
      url = url.substring(0, url.length - 1)
    }
    url += this.$route.path

    history.replaceState(
      {},
      document.title,
      url +
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

  @Watch('selectedMeasurements')
  @Watch('skip')
  @Watch('amount')
  private switchGraphFlavour() {
    if (this.dynamicFlavourSwitching) {
      if (Number(this.amount) * this.selectedMeasurements.length >= 20000) {
        this.currentFlavour = this.graphFlavours[1]
      } else {
        this.currentFlavour = this.graphFlavours[2]
      }
    }
  }

  beforeRouteEnter(
    to: Route,
    from: Route,
    next: (to?: RawLocation | false | ((vm: Vue) => any) | void) => void
  ): void {
    next(component => {
      let vm = component as RepoDetail
      vm.updateToUrl(to.query)

      vxm.detailGraphModule.fetchDetailGraph(vm.payload)
      vm.updateUrl()
    })
  }

  updateSelection(newAmount: number, additionalSkip: number): void {
    this.amount = newAmount.toString()
    this.skip = (Number.parseInt(this.skip) + additionalSkip).toString()
    this.retrieveRuns()
  }

  private get repo(): Repo {
    return vxm.repoModule.repoById(this.id)!
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
        ({ metric, benchmark }) =>
          new Dimension(benchmark, metric, 's', 'NEUTRAL')
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

  mounted(): void {
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
  opacity: 0.7;
}
</style>
