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
              <v-toolbar color="primary" dark>Filter Data</v-toolbar>
            </v-card-title>
            <v-card-text>
              <v-container fluid>
                <v-row align="start" justify="space-around">
                  <v-col md="5" sm="12" xs="12" class="d-flex">
                    <v-select
                      :items="occuringBenchmarks"
                      v-model="selectedBenchmark"
                      label="benchmark"
                      class="mr-5"
                    ></v-select>
                    <v-select
                      :items="metricsForBenchmark(this.selectedBenchmark)"
                      v-model="selectedMetric"
                      label="metric"
                    ></v-select>
                  </v-col>
                  <v-col md="5" sm="12" xs="12" class="d-flex">
                    <v-menu
                      ref="startDateMenu"
                      v-model="startDateMenuOpen"
                      :close-on-content-click="false"
                      :return-value.sync="startDate"
                      transition="scale-transition"
                      offset-y
                      min-width="290px"
                    >
                      <template v-slot:activator="{ on }" class="mr-5">
                        <v-text-field
                          v-model="startDate"
                          label="from:"
                          :prepend-icon="dateIcon"
                          readonly
                          v-on="on"
                        ></v-text-field>
                      </template>
                      <v-date-picker v-model="startDate" :max="today" no-title scrollable>
                        <v-spacer></v-spacer>
                        <v-btn text color="primary" @click="startDateMenuOpen = false">Cancel</v-btn>
                        <v-btn
                          text
                          color="primary"
                          @click="$refs.startDateMenu.save(startDate); retrieveGraphData()"
                        >OK</v-btn>
                      </v-date-picker>
                    </v-menu>
                    <v-menu
                      ref="stopDateMenu"
                      v-model="stopDateMenuOpen"
                      :close-on-content-click="false"
                      :return-value.sync="stopDate"
                      transition="scale-transition"
                      offset-y
                      min-width="290px"
                    >
                      <template v-slot:activator="{ on }">
                        <v-text-field
                          v-model="stopDate"
                          label="to:"
                          :prepend-icon="dateIcon"
                          readonly
                          v-on="on"
                        ></v-text-field>
                      </template>
                      <v-date-picker
                        v-model="stopDate"
                        :min="startDate"
                        :max="today"
                        no-title
                        scrollable
                      >
                        <v-spacer></v-spacer>
                        <v-btn text color="primary" @click="stopDateMenuOpen = false">Cancel</v-btn>
                        <v-btn
                          text
                          color="primary"
                          @click="$refs.stopDateMenu.save(stopDate); retrieveGraphData()"
                        >OK</v-btn>
                      </v-date-picker>
                    </v-menu>
                  </v-col>
                </v-row>
              </v-container>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <v-row align="start" justify="space-between">
        <v-col>
          <v-row>
            <v-col class="d-flex">
              <repo-selector v-on:selectionChanged="retrieveGraphData()"></repo-selector>
            </v-col>
          </v-row>
          <v-row align="center">
            <v-col>
              <repo-add>
                <template #activator="{ on }">
                  <v-btn v-on="on" v-show="isAdmin">add a new repository</v-btn>
                </template>
              </repo-add>
            </v-col>
          </v-row>
        </v-col>
        <v-col>
          <h2>repos and their selected branches:</h2>
          <p>{{this.branches}}</p>
          <h2>graph data:</h2>
          <p>{{this.graphData}}</p>
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
import { Repo, MeasurementID } from '../store/types'
import { mdiCalendar } from '@mdi/js'

@Component({
  components: {
    'repo-add': RepoAddDialog,
    'repo-selector': RepoSelector
  }
})
export default class RepoComparison extends Vue {
  private selectedBenchmark: string = ''
  private selectedMetric: string = ''

  private today = new Date().toISOString().substr(0, 10)

  private startDateMenuOpen: boolean = false

  // get the date one week ago in a quite clumsy way
  private startDate = new Date(new Date().setDate(new Date().getDate() - 7))
    .toISOString()
    .substr(0, 10)

  private stopDateMenuOpen: boolean = false
  private stopDate = new Date().toISOString().substr(0, 10)

  // ============== ICONS ==============
  private dateIcon = mdiCalendar
  // ==============       ==============

  get allRepos(): Repo[] {
    return vxm.repoModule.allRepos
  }

  get allColors(): string[] {
    return vxm.colorModule.allColors
  }

  get occuringBenchmarks(): string[] {
    var benchmarks: string[] = []
    vxm.repoComparisonModule.selectedRepos.forEach(repoId => {
      let repo: Repo | undefined = vxm.repoModule.repoByID(repoId)
      if (repo) {
        var measurements: MeasurementID[] = repo.measurements
        measurements.forEach(measurement => {
          if (!benchmarks.includes(measurement.benchmark)) {
            benchmarks.push(measurement.benchmark)
          }
        })
      }
    })
    return benchmarks
  }

  get metricsForBenchmark(): (benchmark: string) => string[] {
    return (benchmark: string) => {
      var metrics: string[] = []
      vxm.repoComparisonModule.selectedRepos.forEach(repoId => {
        let repo: Repo | undefined = vxm.repoModule.repoByID(repoId)
        if (repo) {
          var measurements = repo.measurements
          measurements.forEach(measurement => {
            if (
              measurement.benchmark === benchmark &&
              !metrics.includes(measurement.metric)
            ) {
              metrics.push(measurement.metric)
            }
          })
        }
      })
      return metrics
    }
  }

  get isAdmin(): boolean {
    return vxm.userModule.isAdmin
  }

  get startUnixTimestamp(): number {
    return new Date(this.startDate).getTime() / 1000
  }

  get stopUnixTimestamp(): number {
    return new Date(this.stopDate).getTime() / 1000
  }

  get payload(): {
    startTime: number
    stopTime: number
    benchmark: string
    metric: string
    } {
    return {
      startTime: this.startUnixTimestamp,
      stopTime: this.stopUnixTimestamp,
      benchmark: this.selectedBenchmark,
      metric: this.selectedMetric
    }
  }

  @Watch('selectedMetric')
  retrieveGraphData() {
    if (this.selectedMetric !== '') {
      vxm.repoComparisonModule.fetchDatapoints(this.payload)
    }
  }

  get repos() {
    return vxm.repoComparisonModule.selectedRepos
  }

  get branches() {
    return vxm.repoComparisonModule.selectedBranchesByRepoID
  }

  get graphData() {
    return vxm.repoComparisonModule.allRuns
  }
}
</script>
