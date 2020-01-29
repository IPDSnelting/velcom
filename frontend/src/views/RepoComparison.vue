<template>
  <div class="repo-comparison">
    <v-container fluid>
      <v-row align="baseline" justify="center">
        <h1>Repository Comparison</h1>
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
                          :items="occuringBenchmarks"
                          v-model="selectedBenchmark"
                          label="benchmark"
                          class="mr-5"
                        ></v-select>
                      </v-col>
                      <v-col>
                        <v-select
                          :items="metricsForBenchmark(this.selectedBenchmark)"
                          v-model="selectedMetric"
                          label="metric"
                        ></v-select>
                      </v-col>
                    </v-row>
                  </v-col>
                  <v-col md="5" sm="12" cols="12">
                    <v-row align="center">
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
                              v-model="startTimeString"
                              label="from:"
                              :prepend-icon="dateIcon"
                              :rules="[notAfterToday]"
                              readonly
                              v-on="on"
                            ></v-text-field>
                          </template>
                          <v-date-picker v-model="startTimeString" no-title scrollable>
                            <v-btn
                              text
                              color="primary"
                              @click="$refs.startDateMenu.save(today); retrieveGraphData()"
                            >Today</v-btn>
                            <v-spacer></v-spacer>
                            <v-btn text color="primary" @click="startDateMenuOpen = false">Cancel</v-btn>
                            <v-btn
                              text
                              color="primary"
                              @click="$refs.startDateMenu.save(startTimeString); retrieveGraphData()"
                            >OK</v-btn>
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
                              v-model="stopTimeString"
                              label="to:"
                              :prepend-icon="dateIcon"
                              :rules="[stopAfterStart, notAfterToday]"
                              readonly
                              v-on="on"
                            ></v-text-field>
                          </template>
                          <v-date-picker v-model="stopTimeString" no-title scrollable>
                            <v-btn
                              text
                              color="primary"
                              @click="$refs.stopDateMenu.save(today); retrieveGraphData()"
                            >Today</v-btn>
                            <v-spacer></v-spacer>
                            <v-btn text color="primary" @click="stopDateMenuOpen = false">Cancel</v-btn>
                            <v-btn
                              text
                              color="primary"
                              @click="$refs.stopDateMenu.save(stopTimeString); retrieveGraphData()"
                            >OK</v-btn>
                          </v-date-picker>
                        </v-menu>
                      </v-col>
                      <v-col>
                        <v-btn text color="primary" @click="resetDates()">Reset dates</v-btn>
                        <v-btn
                          :disabled="!selectedBenchmark || !selectedMetric"
                          text
                          color="primary"
                          @click="autoZoom()"
                        >Auto zoom</v-btn>
                      </v-col>
                    </v-row>
                  </v-col>
                </v-row>
              </v-container>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <v-row align="start" justify="start" class="d-flex" no-gutters>
        <v-col cols="auto">
          <repo-selector v-on:selectionChanged="retrieveGraphData()"></repo-selector>
        </v-col>
        <v-col md="9" class="mt-3">
          <comparison-graph :metric="this.selectedMetric"></comparison-graph>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Watch } from 'vue-property-decorator'
import { vxm } from '../store/index'
import RepoAddDialog from '../components/dialogs/RepoAddDialog.vue'
import RepoSelector from '../components/RepoSelector.vue'
import ComparisonGraph from '../components/graphs/ComparisonGraph.vue'
import { Repo, MeasurementID, Datapoint } from '../store/types'
import { mdiCalendar } from '@mdi/js'

@Component({
  components: {
    'repo-add': RepoAddDialog,
    'repo-selector': RepoSelector,
    'comparison-graph': ComparisonGraph
  }
})
export default class RepoComparison extends Vue {
  private selectedBenchmark: string = ''
  private selectedMetric: string = ''

  private today = new Date().toISOString().substr(0, 10)

  private startDateMenuOpen: boolean = false
  private stopDateMenuOpen: boolean = false

  // ============== ICONS ==============
  private dateIcon = mdiCalendar
  // ==============       ==============

  get selectedMeasurement() {
    return new MeasurementID(this.selectedBenchmark, this.selectedMetric)
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

  @Watch('selectedBenchmark')
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

  @Watch('selectedMetric')
  retrieveGraphData() {
    if (this.selectedMetric !== '' && this.stopAfterStart && this.notAfterToday) {
      vxm.repoComparisonModule.fetchComparisonData(this.payload)
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
    vxm.repoComparisonModule
      .fetchComparisonData({
        startTime: null,
        stopTime: null,
        ...this.payload
      })
      .then(data => {
        let { min, max } = Object.values(data)
          .flatMap(it => it)
          .reduce(
            (accumulated, next) => {
              let time = next.commit.authorDate
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
      })
  }
}
</script>
