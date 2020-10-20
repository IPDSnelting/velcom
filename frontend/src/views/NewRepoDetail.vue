<template>
  <v-container v-if="repoExists(id)">
    <v-row>
      <v-col>
        <repo-base-information :repo="repo"></repo-base-information>
      </v-col>
    </v-row>
    <v-row align="baseline" justify="center" no-gutters>
      <v-col class="ma-0 pa-0">
        <v-card>
          <v-card-title class="mb-0 pb-0">
            <v-row no-gutters align="center" justify="space-between">
              <v-col class="ma-0 pa-0">
                <v-btn-toggle
                  :value="selectedGraphComponent"
                  @change="setSelectedGraphComponent"
                  mandatory
                >
                  <v-btn
                    v-for="{ component, name } in availableGraphComponents"
                    :key="name"
                    :value="component"
                  >
                    {{ name }}
                  </v-btn>
                </v-btn-toggle>
              </v-col>
              <v-col cols="auto">
                <share-graph-link-dialog />
              </v-col>
            </v-row>
          </v-card-title>
          <v-card-text>
            <component
              :is="selectedGraphComponent"
              :dimensions="selectedDimensions"
              :beginYAtZero="yStartsAtZero"
              :dayEquidistant="dayEquidistantGraphSelected"
            ></component>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
    <v-row align="baseline" justify="center">
      <v-col>
        <v-card>
          <v-card-text>
            <v-container fluid class="ma-0 px-5 pb-0">
              <v-row align="start" justify="space-between" no-gutters>
                <v-col :md="useMatrixSelector ? '' : '5'" sm="12" cols="12">
                  <v-btn
                    @click="useMatrixSelector = !useMatrixSelector"
                    text
                    color="primary"
                  >
                    <span v-if="useMatrixSelector">Use tree selector</span>
                    <span v-if="!useMatrixSelector">Use matrix selector</span>
                  </v-btn>
                  <matrix-dimension-selection
                    v-if="useMatrixSelector"
                    :selectedDimensions="selectedDimensions"
                    :repoId="id"
                  ></matrix-dimension-selection>
                  <normal-dimension-selection
                    v-if="!useMatrixSelector"
                    :selectedDimensions="selectedDimensions"
                    :repoId="id"
                  >
                  </normal-dimension-selection>
                </v-col>
                <v-col class="d-flex justify-end">
                  <v-btn
                    v-if="graphSupportsDayEquidistantDisplay"
                    color="primary"
                    outlined
                    class="mr-4"
                    @click="
                      dayEquidistantGraphSelected = !dayEquidistantGraphSelected
                    "
                  >
                    <span v-if="dayEquidistantGraphSelected">
                      Disable Day-Equistant Graph
                    </span>
                    <span v-else>Enable Day-Equistant Graph</span>
                  </v-btn>
                  <v-btn
                    @click="yStartsAtZero = !yStartsAtZero"
                    color="primary"
                    outlined
                  >
                    <span v-if="yStartsAtZero">
                      Begin Y-Axis at minimum value
                    </span>
                    <span v-else>Begin Y-Axis at zero</span>
                  </v-btn>
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
          <v-card-text class="ma-0 pa-0">
            <v-container fluid class="ma-0 px-5">
              <v-row align="center" justify="space-between" no-gutters>
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
                    <template #activator="{ on }">
                      <v-text-field
                        class="mr-5 mb-5"
                        hide-details="auto"
                        v-model="startTimeString"
                        :disabled="dateLocked === 'start'"
                        label="from:"
                        :prepend-icon="dateIcon"
                        readonly
                        v-on="on"
                      >
                        <v-icon
                          slot="append"
                          @click="lockDates('start')"
                          class="lock-button"
                        >
                          {{ dateLocked === 'start' ? lock : openLock }}
                        </v-icon>
                      </v-text-field>
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
                        @click="saveStartDateMenu(today)"
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
                        @click="saveStartDateMenu(startTimeString)"
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
                    :return-value.sync="endTimeString"
                    transition="scale-transition"
                    offset-y
                    min-width="290px"
                  >
                    <template v-slot:activator="{ on }">
                      <v-text-field
                        class="mr-5 mb-5"
                        hide-details="auto"
                        v-model="endTimeString"
                        :disabled="dateLocked === 'end'"
                        label="to:"
                        :prepend-icon="dateIcon"
                        :rules="[stopAfterStart]"
                        readonly
                        v-on="on"
                      >
                        <v-icon
                          slot="append"
                          @click="lockDates('end')"
                          class="lock-button"
                        >
                          {{ dateLocked === 'end' ? lock : openLock }}
                        </v-icon>
                      </v-text-field>
                    </template>
                    <v-date-picker
                      v-model="endTimeString"
                      no-title
                      scrollable
                      hide-details="auto"
                    >
                      <v-btn
                        text
                        color="primary"
                        @click="saveStopDateMenu(today)"
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
                        @click="saveStopDateMenu(endTimeString)"
                        >OK</v-btn
                      >
                    </v-date-picker>
                  </v-menu>
                </v-col>
                <v-col>
                  <v-form>
                    <v-text-field
                      @blur="saveDuration"
                      :value="duration"
                      :disabled="dateLocked === 'neither'"
                      label="number of days to fetch:"
                      class="mr-5"
                      :rules="[ruleIsNumber]"
                    ></v-text-field>
                  </v-form>
                </v-col>
              </v-row>
            </v-container>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
    <v-row>
      <v-col>
        <commit-overview :repo="repo.id"></commit-overview>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { mdiCalendar, mdiLock, mdiLockOpenVariant } from '@mdi/js'
import { vxm } from '@/store'
import RepoBaseInformation from '@/components/repodetail/RepoBaseInformation.vue'
import DimensionSelection from '../components/graphs/DimensionSelection.vue'
import MatrixDimensionSelection from '../components/graphs/MatrixDimensionSelection.vue'
import RepoCommitOverview from '@/components/repodetail/RepoCommitOverview.vue'
import { Dimension, Repo } from '@/store/types'
import EchartsDetailGraph from '@/components/graphs/EchartsDetailGraph.vue'
import DytailGraph from '@/components/graphs/NewDytailGraph.vue'
import { Watch } from 'vue-property-decorator'
import ShareGraphLinkDialog from '@/views/ShareGraphLinkDialog.vue'
import GraphPlaceholder from '@/components/graphs/GraphPlaceholder.vue'

@Component({
  components: {
    'share-graph-link-dialog': ShareGraphLinkDialog,
    'repo-base-information': RepoBaseInformation,
    'matrix-dimension-selection': MatrixDimensionSelection,
    'normal-dimension-selection': DimensionSelection,
    'new-echarts-detail': EchartsDetailGraph,
    'dygraph-detail': DytailGraph,
    'commit-overview': RepoCommitOverview,
    'graph-placeholder': GraphPlaceholder
  }
})
export default class RepoDetail extends Vue {
  // ============== ICONS ==============
  private dateIcon = mdiCalendar
  private openLock = mdiLockOpenVariant
  private lock = mdiLock
  // ==============       ==============

  private today = new Date().toISOString().substr(0, 10)

  private useMatrixSelector: boolean = false

  private startDateMenuOpen: boolean = false
  private stopDateMenuOpen: boolean = false
  private dateLocked: 'start' | 'end' | 'neither' = 'end'
  private availableGraphComponents = [
    {
      predicate: () => {
        return vxm.detailGraphModule.visiblePoints < 30_000
      },
      component: EchartsDetailGraph,
      name: 'Fancy'
    },
    {
      predicate: () => {
        // matches from first to last. this one is the fallback
        return true
      },
      component: DytailGraph,
      name: 'Fast'
    }
  ]

  private selectedGraphComponent: typeof Vue | null = GraphPlaceholder
  private dayEquidistantGraphSelected: boolean = false

  private get repo(): Repo {
    return vxm.repoModule.repoById(this.id)!
  }

  private get id() {
    return this.$route.params.id
  }

  private repoExists(id: string): boolean {
    return vxm.repoModule.repoById(id) !== undefined
  }

  private get selectedDimensions(): Dimension[] {
    return vxm.detailGraphModule.selectedDimensions
  }

  private saveStartDateMenu(date: string) {
    ;(this.$refs.startDateMenu as any).save(date)
    vxm.detailGraphModule.startTime = new Date(date)
    this.retrieveGraphData()
  }

  private saveStopDateMenu(date: string) {
    ;(this.$refs.stopDateMenu as any).save(date)
    vxm.detailGraphModule.endTime = new Date(date)
    this.retrieveGraphData()
  }

  private saveDuration(event: Event) {
    const target = event.target as HTMLInputElement
    const duration: number = parseInt(target.value)

    if (isNaN(duration)) {
      return
    }

    if (this.dateLocked === 'start') {
      vxm.detailGraphModule.endTime = new Date(
        new Date().setDate(vxm.detailGraphModule.startTime.getDate() + duration)
      )
    } else {
      vxm.detailGraphModule.startTime = new Date(
        new Date().setDate(vxm.detailGraphModule.endTime.getDate() - duration)
      )
    }
    this.retrieveGraphData()
  }

  private get startTimeString(): string {
    return vxm.detailGraphModule.startTime.toISOString().substring(0, 10)
  }

  // v-model binding
  // noinspection JSUnusedGlobalSymbols
  private set startTimeString(value: string) {
    vxm.detailGraphModule.startTime = new Date(value)
  }

  private get endTimeString(): string {
    return vxm.detailGraphModule.endTime.toISOString().substring(0, 10)
  }

  // v-model binding
  // noinspection JSUnusedGlobalSymbols
  private set endTimeString(value: string) {
    vxm.detailGraphModule.endTime = new Date(value)
  }

  private get duration(): number {
    return vxm.detailGraphModule.duration
  }

  private get yStartsAtZero(): boolean {
    return vxm.detailGraphModule.beginYScaleAtZero
  }

  private set yStartsAtZero(startsAtZero: boolean) {
    vxm.detailGraphModule.beginYScaleAtZero = startsAtZero
  }

  private get graphSupportsDayEquidistantDisplay() {
    return this.selectedGraphComponent === EchartsDetailGraph
  }

  private lockDates(date: 'start' | 'end'): void {
    if (this.dateLocked === 'neither') {
      this.dateLocked = date
    } else if ((this.dateLocked = date)) {
      this.dateLocked = 'neither'
    } else {
      this.dateLocked = this.dateLocked === 'start' ? 'end' : 'start'
    }
  }

  private stopAfterStart(): boolean | string {
    return vxm.detailGraphModule.startTime.getTime() <=
      vxm.detailGraphModule.endTime.getTime()
      ? true
      : 'You have to select a date after the first one!'
  }

  private setSelectedGraphComponent(component: typeof Vue) {
    if (this.selectedGraphComponent === GraphPlaceholder) {
      return
    }
    this.selectedGraphComponent = component
  }

  @Watch('id')
  @Watch('selectedDimensions')
  private async retrieveGraphData(): Promise<void> {
    if (this.stopAfterStart()) {
      this.selectedGraphComponent = GraphPlaceholder
      await vxm.detailGraphModule.fetchDetailGraph()
      const correctSeries = this.availableGraphComponents.find(it =>
        it.predicate()
      )
      if (correctSeries) {
        this.selectedGraphComponent = correctSeries.component
      }
    }
  }

  private ruleIsNumber(input: string): string | boolean {
    return isNaN(parseInt(input)) ? 'Please enter a number' : true
  }

  mounted(): void {
    this.retrieveGraphData()
  }
}
</script>

<style scoped>
/*  https://stackoverflow.com/questions/53391733/untie-text-fields-icon-click-enabling-from-the-input-one */
.lock-button {
  pointer-events: auto;
}
</style>
