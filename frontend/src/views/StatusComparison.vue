<template>
  <v-container fluid>
    <v-row>
      <v-col
        cols="3"
        class="overflow-y-auto"
        style="height: calc(100vh - 70px)"
      >
        <v-card
          outlined
          class="mb-2"
          :class="{ 'warning-outline': baselineRepoId === null }"
        >
          <v-card-text>
            <repo-selection-component
              :repos="possibleBaselineRepos"
              label="Baseline repository"
              v-model="baselineRepoId"
            ></repo-selection-component>
          </v-card-text>
        </v-card>
        <repo-branch-selector
          :selected-branches="selectedBranches"
          @update:toggle-branch="toggleRepoBranch"
          @update:set-all="setRepoBranches"
        ></repo-branch-selector>
        <expandable-dimension-selection
          class="mt-3"
          :all-dimensions="allDimensions"
          :selected-dimensions.sync="selectedDimensions"
          :selector-type.sync="dimensionSelectorType"
        ></expandable-dimension-selection>
      </v-col>
      <v-col cols="9" style="height: calc(100vh - 70px)">
        <v-card style="height: 100%">
          <v-card-text style="height: 100%" class="py-0 my-0">
            <status-comparison-graph
              :datapoints="data"
              :baseline-data="baselineData"
              :selected-dimensions="selectedDimensions"
            ></status-comparison-graph>
            <v-overlay
              v-if="overlayText"
              absolute
              class="ma-0 pa-0"
              color="black"
            >
              <span class="text-h6">{{ overlayText }}</span>
            </v-overlay>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import RepoBranchSelector from '@/components/graphs/comparison/RepoBranchSelector.vue'
import StatusComparisonGraph from '@/components/graphs/statuscomparison/StatusComparisonGraph.vue'
import {
  Dimension,
  MeasurementSuccess,
  RepoId,
  RunResultSuccess,
  StatusComparisonPoint
} from '@/store/types'
import { vxm } from '@/store'
import ExpandableDimensionSelection from '@/components/graphs/helper/ExpandableDimensionSelection.vue'
import { Watch } from 'vue-property-decorator'
import RepoSelectionComponent from '@/components/misc/RepoSelectionComponent.vue'

@Component({
  components: {
    RepoSelectionComponent,
    ExpandableDimensionSelection,
    StatusComparisonGraph,
    RepoBranchSelector
  }
})
export default class StatusComparison extends Vue {
  private data: StatusComparisonPoint[] = []

  private get allDimensions() {
    return vxm.repoModule.occuringDimensions(
      vxm.repoModule.allRepos.map(it => it.id)
    )
  }

  private get selectedBranches() {
    return vxm.statusComparisonModule.selectedBranchesMap
  }

  private get selectedDimensions() {
    return vxm.statusComparisonModule.selectedDimensions
  }

  private set selectedDimensions(dimensions: Dimension[]) {
    vxm.statusComparisonModule.selectedDimensions = dimensions
  }

  private get dimensionSelectorType() {
    return vxm.statusComparisonModule.selectedDimensionSelector
  }

  private set dimensionSelectorType(selector: 'tree' | 'matrix') {
    vxm.statusComparisonModule.selectedDimensionSelector = selector
  }

  private toggleRepoBranch(payload: { repoId: string; branch: string }) {
    vxm.statusComparisonModule.toggleRepoBranch(payload)
  }

  private setRepoBranches(payload: { repoId: string; branches: string[] }) {
    vxm.statusComparisonModule.setSelectedBranchesForRepo(payload)
  }

  private get baselineRepoId() {
    const baselineId = vxm.statusComparisonModule.baselineRepoId

    if (!baselineId) {
      return null
    }

    // Baseline is not selected => Ignore it
    if (!vxm.statusComparisonModule.selectedBranchesMap.has(baselineId)) {
      return null
    }

    return baselineId
  }

  private set baselineRepoId(id: RepoId | null) {
    vxm.statusComparisonModule.baselineRepoId = id
  }

  private get possibleBaselineRepos() {
    return Array.from(
      vxm.statusComparisonModule.selectedBranchesMap.keys()
    ).map(id => vxm.repoModule.repoById(id))
  }

  private get overlayText() {
    if (this.selectedDimensions.length === 0) {
      return 'Please select a dimension on the left'
    }
    if (this.selectedBranches.size === 0) {
      return 'Please select a repo/branch on the left'
    }
    return this.baselineErrorMessage
  }

  private get baselineErrorMessage() {
    const baselineRepoId = this.baselineRepoId
    if (baselineRepoId === null) {
      return 'Please select a baseline in the top left'
    }

    const baselinePoint = this.data.find(it => it.repoId === baselineRepoId)
    if (!baselinePoint) {
      return 'No data found for your selected baseline'
    }

    const run = baselinePoint.run
    if (!run) {
      return 'Baseline has no valid run - Normalization is impossible'
    }

    if (!(run.result instanceof RunResultSuccess)) {
      return 'Baseline run failed - Normalization is impossible'
    }

    const successfulMeasurement = run.result.measurements.find(
      it => it instanceof MeasurementSuccess
    )
    if (!successfulMeasurement) {
      return 'All metrics for baseline run failed - Normalization is impossible'
    }

    return null
  }

  private get baselineData() {
    if (this.baselineErrorMessage !== null) {
      return null
    }
    const baselineRepoId = vxm.statusComparisonModule.baselineRepoId!
    const baselinePoint = this.data.find(it => it.repoId === baselineRepoId)!
    const run = baselinePoint.run!
    const result = run.result as RunResultSuccess
    return result.measurements.filter(it => it instanceof MeasurementSuccess)
  }

  @Watch('selectedBranches')
  private async fetchData() {
    this.data = await vxm.statusComparisonModule.fetch()
  }

  private async mounted() {
    await this.fetchData()
  }
}
</script>

<!--suppress CssUnresolvedCustomProperty -->
<style scoped>
.warning-outline {
  border-color: var(--v-warning-base) !important;
}
</style>
