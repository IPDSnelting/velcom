<template>
  <v-container fluid>
    <v-row>
      <v-col>
        <v-card>
          <v-card-text class="pa-1">
            <v-container fluid class="ma-0 pa-4">
              <v-row align="center" justify="space-around" no-gutters>
                <v-col md="5" cols="12">
                  <v-row no-gutters>
                    <v-col>
                      <v-select
                        :items="occuringBenchmarks"
                        v-model="selectedBenchmark"
                        label="benchmark"
                        class="mr-5"
                        hide-details="auto"
                      ></v-select>
                    </v-col>
                    <v-col>
                      <v-select
                        :items="metricsForBenchmark(this.selectedBenchmark)"
                        v-model="selectedMetric"
                        label="metric"
                        hide-details="auto"
                      ></v-select>
                    </v-col>
                  </v-row>
                </v-col>
                <v-col md="5" cols="12">
                  <v-row align="center" no-gutters>
                    <v-col>
                      <v-menu
                        ref="startDateMenu"
                        v-model="startDateMenuOpen"
                        :close-on-content-click="false"
                        :return-value.sync="startTimeString"
                        transition="scale-transition"
                        offset-y
                        min-width="290px"
                      >
                        <template v-slot:activator="{ on }" class="mr-5">
                          <v-text-field
                            hide-details="auto"
                            v-model="startTimeString"
                            label="from:"
                            :prepend-icon="dateIcon"
                            :rules="[notAfterToday]"
                            readonly
                            v-on="on"
                          ></v-text-field>
                        </template>
                        <v-date-picker
                          hide-details="auto"
                          v-model="startTimeString"
                          no-title
                          scrollable
                        >
                          <v-btn
                            text
                            color="primary"
                            @click="
                              $refs.startDateMenu.save(today)
                              retrieveGraphData()
                            "
                            >Today</v-btn
                          >
                          <v-spacer></v-spacer>
                          <v-btn
                            text
                            color="primary"
                            @click="startDateMenuOpen = false"
                            >Cancel</v-btn
                          >
                          <v-btn
                            text
                            color="primary"
                            @click="
                              $refs.startDateMenu.save(startTimeString)
                              retrieveGraphData()
                            "
                            >OK</v-btn
                          >
                        </v-date-picker>
                      </v-menu>
                    </v-col>
                    <v-col>
                      <v-menu
                        ref="stopDateMenu"
                        v-model="stopDateMenuOpen"
                        :close-on-content-click="false"
                        :return-value.sync="stopTimeString"
                        transition="scale-transition"
                        offset-y
                        min-width="290px"
                      >
                        <template v-slot:activator="{ on }">
                          <v-text-field
                            hide-details="auto"
                            v-model="stopTimeString"
                            label="to:"
                            :prepend-icon="dateIcon"
                            :rules="[stopAfterStart, notAfterToday]"
                            readonly
                            v-on="on"
                          ></v-text-field>
                        </template>
                        <v-date-picker
                          v-model="stopTimeString"
                          no-title
                          scrollable
                          hide-details="auto"
                        >
                          <v-btn
                            text
                            color="primary"
                            @click="
                              $refs.stopDateMenu.save(today)
                              retrieveGraphData()
                            "
                            >Today</v-btn
                          >
                          <v-spacer></v-spacer>
                          <v-btn
                            text
                            color="primary"
                            @click="stopDateMenuOpen = false"
                            >Cancel</v-btn
                          >
                          <v-btn
                            text
                            color="primary"
                            @click="
                              $refs.stopDateMenu.save(stopTimeString)
                              retrieveGraphData()
                            "
                            >OK</v-btn
                          >
                        </v-date-picker>
                      </v-menu>
                    </v-col>
                  </v-row>
                </v-col>
              </v-row>
            </v-container>
          </v-card-text>
          <v-card-actions>
            <v-row no-gutters justify="center">
              <v-col cols="auto">
                <v-btn text color="primary" @click="resetDates()"
                  >Reset dates</v-btn
                >
              </v-col>
              <v-col cols="auto">
                <v-btn
                  :disabled="!selectedBenchmark || !selectedMetric"
                  text
                  color="primary"
                  @click="autoZoom()"
                  >Auto zoom</v-btn
                >
              </v-col>
              <v-col>
                <v-btn text color="primary" @click="zoomToBrush()"
                  >Zoom to brushed area</v-btn
                >
              </v-col>
              <v-col cols="auto">
                <v-btn text color="primary" @click="toggleYScale()">{{
                  yScaleButtonLabel
                }}</v-btn>
              </v-col>
            </v-row>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>

    <v-row align="start" justify="start" class="d-flex">
      <v-col>
        <v-row no-gutters>
          <v-card>
            <v-col v-show="referenceCommitSelected">
              <v-row no-gutters>
                <v-col cols="12" class="ma-0 pa-0">
                  <span class="hint pa-0">reference line commit</span>
                </v-col>
                <v-col class="pa-0">
                  <commit-chip
                    class="mr-2"
                    :to="{
                      name: 'commit-detail',
                      params: { repoID: referenceRepoID, hash: referenceHash }
                    }"
                    :commitHash="referenceHash"
                    :copyOnClick="false"
                  ></commit-chip>
                </v-col>
              </v-row>
            </v-col>
            <v-col class="ma-0 pa-0">
              <repo-selector
                v-on:selectionChanged="retrieveGraphData()"
              ></repo-selector>
            </v-col>
          </v-card>
        </v-row>
      </v-col>
      <v-col style="flex: 1 1 50%; min-width: 600px">
        <v-card>
          <comparison-graph
            ref="graph"
            :beginYAtZero="this.yScaleBeginsAtZero"
          ></comparison-graph>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Watch } from 'vue-property-decorator'
import { vxm } from '../store/index'
import RepoAddDialog from '../components/dialogs/RepoAddDialog.vue'
import RepoSelector from '../components/RepoSelector.vue'
import { Repo, Commit, Dimension } from '../store/types'
import { mdiCalendar } from '@mdi/js'
import { Route, RawLocation } from 'vue-router'
import { Dictionary } from 'vue-router/types/router'
import ComparisonGraph from '../components/graphs/ComparisonGraph.vue'
import CommitChip from '../components/CommitChip.vue'

@Component({
  components: {
    'repo-add': RepoAddDialog,
    'repo-selector': RepoSelector,
    'comparison-graph': ComparisonGraph,
    'commit-chip': CommitChip
  }
})
export default class RepoComparison extends Vue {
  private today = new Date().toISOString().substr(0, 10)

  private startDateMenuOpen: boolean = false
  private stopDateMenuOpen: boolean = false

  private yScaleBeginsAtZero: boolean = true
  private yScaleButtonLabel:
    | 'begin y-Scale at zero'
    | 'begin y-Scale at minimum Value' = 'begin y-Scale at minimum Value'

  // ============== ICONS ==============
  private dateIcon = mdiCalendar
  // ==============       ==============

  get selectedMeasurement() {
    return new Dimension(
      this.selectedBenchmark,
      this.selectedMetric,
      '',
      'NEUTRAL'
    )
  }

  get selectedBenchmark() {
    return vxm.repoComparisonModule.selectedBenchmark
  }

  set selectedBenchmark(newBenchmark: string) {
    if (vxm.repoComparisonModule.selectedBenchmark !== newBenchmark) {
      let newMetrics = this.metricsForBenchmark(newBenchmark)
      if (!newMetrics.includes(this.selectedMetric)) {
        if (newMetrics) {
          this.selectedMetric = newMetrics[0]
        }
      }
    }

    vxm.repoComparisonModule.selectedBenchmark = newBenchmark
  }

  get selectedMetric() {
    return vxm.repoComparisonModule.selectedMetric
  }

  set selectedMetric(metric: string) {
    vxm.repoComparisonModule.selectedMetric = metric
  }

  get allRepos(): Repo[] {
    return vxm.repoModule.allRepos
  }

  get allColors(): string[] {
    return vxm.colorModule.allColors
  }

  get occuringBenchmarks(): string[] {
    return vxm.repoModule.occuringBenchmarks(
      vxm.repoComparisonModule.selectedRepos
    )
  }

  get metricsForBenchmark(): (benchmark: string) => string[] {
    return (benchmark: string) => vxm.repoModule.metricsForBenchmark(benchmark)
  }

  get isAdmin(): boolean {
    return vxm.userModule.isAdmin
  }

  get payload(): { benchmark: string; metric: string } {
    return {
      benchmark: this.selectedBenchmark,
      metric: this.selectedMetric
    }
  }

  get startTimeString() {
    return vxm.repoComparisonModule.startDate.toISOString().substring(0, 10)
  }

  set startTimeString(value: string) {
    vxm.repoComparisonModule.startDate = new Date(value)
  }

  get stopTimeString() {
    return vxm.repoComparisonModule.stopDate.toISOString().substring(0, 10)
  }

  set stopTimeString(value: string) {
    vxm.repoComparisonModule.stopDate = new Date(value)
  }

  private stopAfterStart(): boolean | string {
    return vxm.repoComparisonModule.startDate.getTime() <=
      vxm.repoComparisonModule.stopDate.getTime()
      ? true
      : 'This date must not be be before the first!'
  }

  private notAfterToday(input: string): boolean | string {
    let inputDate = new Date(input)
    let inputTime = inputDate.getTime()
    let today: Date = new Date()
    return inputTime <= today.getTime()
      ? true
      : 'This date must not be after today!'
  }

  private get referenceHash(): string {
    return vxm.repoComparisonModule.referenceDatapoint !== undefined
      ? vxm.repoComparisonModule.referenceDatapoint.hash
      : ''
  }

  private get referenceRepoID(): string {
    return vxm.repoComparisonModule.referenceDatapoint !== undefined
      ? vxm.repoComparisonModule.referenceDatapoint.hash
      : ''
  }

  private get referenceCommitSelected(): boolean {
    return vxm.repoComparisonModule.referenceDatapoint !== undefined
  }

  @Watch('selectedBenchmark')
  @Watch('selectedMetric')
  clearMetricOnBenchmarkSelection() {
    if (
      this.metricsForBenchmark(this.selectedBenchmark).includes(
        this.selectedMetric
      )
    ) {
      this.retrieveGraphData()
    } else {
      this.selectedMetric = ''
    }
  }

  private get selectedRepos() {
    return vxm.repoComparisonModule.selectedBranchesByRepoId
  }

  retrieveGraphData() {
    if (
      this.selectedBenchmark &&
      this.selectedMetric &&
      this.stopAfterStart &&
      this.notAfterToday
    ) {
      vxm.repoComparisonModule.fetchComparisonGraph(this.payload)
    }
  }

  updateTimeframe(newMin: Date, newMax: Date) {
    vxm.repoComparisonModule.startDate = newMin
    vxm.repoComparisonModule.stopDate = newMax
    this.retrieveGraphData()
  }

  private toggleYScale() {
    this.yScaleBeginsAtZero = !this.yScaleBeginsAtZero
    if (this.yScaleButtonLabel === 'begin y-Scale at zero') {
      this.yScaleButtonLabel = 'begin y-Scale at minimum Value'
    } else {
      this.yScaleButtonLabel = 'begin y-Scale at zero'
    }
  }

  private resetDates() {
    this.startTimeString = vxm.repoComparisonModule.defaultStartTime
    this.stopTimeString = vxm.repoComparisonModule.defaultStopTime

    if (this.selectedBenchmark && this.selectedMetric) {
      this.retrieveGraphData()
    }
  }

  private autoZoom() {
    /*    vxm.repoComparisonModule
      .fetchComparisonGraph({
        startTime: null,
        endTime: null,
        ...this.payload
      })
      .then(data => {
        let { min, max } = Object.values(data)
          .flatMap(it => it)
          .reduce(
            (accumulated, next) => {
              let time = next.authorDate
              if (time && time < accumulated.min) {
                accumulated.min = time
              }
              if (time && time > accumulated.max) {
                accumulated.max = time
              }
              return accumulated
            },
            { min: 1e200, max: 0 }
          )

        vxm.repoComparisonModule.startDate = new Date(min * 1000)
        vxm.repoComparisonModule.stopDate = new Date(max * 1000)
      }) */
  }

  private zoomToBrush() {
    let brushedArea: number[] = (this.$refs
      .graph as ComparisonGraph).brushedArea()
    this.updateTimeframe(
      new Date(brushedArea[0]),
      new Date(brushedArea[1] - 60 * 60 * 24)
    )
  }

  @Watch('selectedMetric')
  @Watch('selectedBenchmark')
  @Watch('startTimeString')
  @Watch('stopTimeString')
  @Watch('selectedRepos')
  updateUrl() {
    let repos: { [repoId: string]: string[] } = {}
    vxm.repoComparisonModule.selectedReposWithBranches.forEach(
      ({ repo_id: repoId, branches }) => {
        let repo = vxm.repoModule.repoById(repoId)
        if (!repo) {
          return
        }
        if (branches.length === repo.branches.length) {
          repos[repoId] = []
        } else {
          repos[repoId] = branches
        }
      }
    )
    let newQuery: { [param: string]: string } = {
      metric: this.selectedMetric,
      benchmark: this.selectedBenchmark,
      repos: JSON.stringify(repos),
      start: this.startTimeString,
      stop: this.stopTimeString
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

  beforeRouteEnter(
    to: Route,
    from: Route,
    next: (to?: RawLocation | false | ((vm: Vue) => any) | void) => void
  ) {
    next(component => {
      let vm = component as RepoComparison
      vxm.repoModule.fetchRepos().then(() => {
        vm.updateToUrl(to.query)

        vm.updateUrl()
      })
    })
  }

  private updateToUrl(query: Dictionary<string | (string | null)[]>) {
    if (query.repos) {
      let branchesByRepoId: { [key: string]: string[] } = JSON.parse(
        query.repos as string
      )
      vxm.repoComparisonModule.selectedRepos = Object.keys(branchesByRepoId)
      Object.keys(branchesByRepoId).forEach(repoId => {
        let branches = branchesByRepoId[repoId]
        if (branches.length === 0) {
          let repo = vxm.repoModule.repoById(repoId)
          if (!repo) {
            return
          }
          branches = repo.branches.slice().map(it => it.name)
        }
        vxm.repoComparisonModule.setSelectedBranchesForRepo({
          repoID: repoId,
          selectedBranches: branches
        })
      })
    }
    if (query.benchmark) {
      vxm.repoComparisonModule.selectedBenchmark = query.benchmark as string
    }
    if (query.metric) {
      vxm.repoComparisonModule.selectedMetric = query.metric as string
    }
    if (query.start) {
      vxm.repoComparisonModule.startDate = new Date(query.start as string)
    }
    if (query.stop) {
      vxm.repoComparisonModule.stopDate = new Date(query.stop as string)
    }
    this.retrieveGraphData()
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
