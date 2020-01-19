<template>
  <div class="repo-comparison">
    <v-container fluid>
      <v-row align="baseline" justify="center">
        <h1>Repository Comparison</h1>
      </v-row>
      <v-row align="start" justify="space-around">
        <v-col md="5" sm="12" xs="12" class="d-flex">
          <v-select :items="occuringBenchmarks" v-model="selectedBenchmark" label="benchmark"></v-select>
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
            <template v-slot:activator="{ on }">
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
              <v-btn text color="primary" @click="$refs.startDateMenu.save(startDate)">OK</v-btn>
            </v-date-picker>
          </v-menu>
          <v-menu
            ref="endDateMenu"
            v-model="endDateMenuOpen"
            :close-on-content-click="false"
            :return-value.sync="endDate"
            transition="scale-transition"
            offset-y
            min-width="290px"
          >
            <template v-slot:activator="{ on }">
              <v-text-field
                v-model="endDate"
                label="to:"
                :prepend-icon="dateIcon"
                readonly
                v-on="on"
              ></v-text-field>
            </template>
            <v-date-picker v-model="endDate" :min="startDate" :max="today" no-title scrollable>
              <v-spacer></v-spacer>
              <v-btn text color="primary" @click="endDateMenuOpen = false">Cancel</v-btn>
              <v-btn text color="primary" @click="$refs.endDateMenu.save(endDate)">OK</v-btn>
            </v-date-picker>
          </v-menu>
        </v-col>
      </v-row>
      <v-row align="start" justify="space-between">
        <v-col>
          <v-row>
            <v-col class="d-flex">
              <v-card flat outlined max-width="300">
                <v-expansion-panels
                  v-for="(repo, index) in allRepos"
                  :key="index"
                  multiple
                  accordion
                  flat
                >
                  <repo-selector
                    :repoID="repo.id"
                    :index="index"
                    @updateSelect="updateSelectedRepos"
                  ></repo-selector>
                </v-expansion-panels>
              </v-card>
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
          <p>{{this.payloadBranchesByRepo}}</p>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Watch } from 'vue-property-decorator'
import { vxm } from '../store/classIndex'
import RepoAddDialog from '../components/RepoAddDialog.vue'
import RepoSelector from '../components/RepoSelector.vue'
import { Repo } from '../store/types'
import { mdiCalendar } from '@mdi/js'

@Component({
  components: {
    'repo-add': RepoAddDialog,
    'repo-selector': RepoSelector
  }
})
export default class RepoComparison extends Vue {
  private reposSelected: { [key: string]: boolean } = {}
  private selectedBranchesByRepo: { [key: string]: string[] } = {}
  private payloadBranchesByRepo: { [key: string]: string[] } = {}

  private selectedBenchmark: string = ''
  private selectedMetric: string = ''

  private today = new Date().toISOString().substr(0, 10)

  private startDateMenuOpen: boolean = false

  // get the date one week ago in a quite clumsy way
  private startDate = new Date(new Date().setDate(new Date().getDate() - 7))
    .toISOString()
    .substr(0, 10)

  private endDateMenuOpen: boolean = false
  private endDate = new Date().toISOString().substr(0, 10)

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
    return vxm.repoModule.occuringBenchmarks
  }

  get metricsForBenchmark(): (benchmark: string) => string[] {
    return (benchmark: string) => vxm.repoModule.metricsForBenchmark(benchmark)
  }

  get isAdmin(): boolean {
    return vxm.userModule.isAdmin
  }

  get startUnixTimestamp(): number {
    return new Date(this.startDate).getTime() / 1000
  }

  get endUnixTimestamp(): number {
    return new Date(this.endDate).getTime() / 1000
  }

  get payload(): {
    repos: { [key: string]: string[] }
    startTime: number
    endTime: number
    benchmark: string
    metric: string
    } {
    return {
      repos: this.payloadBranchesByRepo,
      startTime: this.startUnixTimestamp,
      endTime: this.endUnixTimestamp,
      benchmark: this.selectedBenchmark,
      metric: this.selectedMetric
    }
  }

  updateSelectedRepos(
    repoID: string,
    selected: boolean,
    selectedBranches: string[]
  ) {
    this.reposSelected[repoID] = selected
    if (selected) {
      this.selectedBranchesByRepo[repoID] = selectedBranches
    }
    this.upadtePayloadBranches()
    vxm.repoComparisonModule.fetchDatapoints(this.payload)
  }

  upadtePayloadBranches() {
    var payloadBranches: { [key: string]: string[] } = {}
    this.allRepos.forEach(repo => {
      if (
        this.reposSelected[repo.id] &&
        this.selectedBranchesByRepo[repo.id].length > 0
      ) {
        payloadBranches[repo.id] = this.selectedBranchesByRepo[repo.id]
      }
    })
    this.payloadBranchesByRepo = payloadBranches
  }

  @Watch('allRepos')
  addMissingColors() {
    if (this.allColors.length < this.allRepos.length) {
      let diff = this.allRepos.length - this.allColors.length
      vxm.colorModule.addColors(diff)
    }
  }

  mounted() {
    vxm.repoModule.fetchRepos()
  }
}
</script>
