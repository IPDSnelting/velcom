<template>
  <v-container fluid :style="{ height: suggestedHeight }" class="pa-0">
    <v-row style="height: 100%">
      <v-col cols="3" class="overflow-y-auto" style="height: 100%">
        <v-card
          outlined
          class="mb-2"
          :class="{ 'warning-outline': baselineRepoId === null }"
        >
          <v-card-text>
            <repo-selection-component
              :repos="possibleBaselineRepos"
              :clearable="true"
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
          class="mt-3 mb-1"
          :all-dimensions="allDimensions"
          :selected-dimensions.sync="selectedDimensions"
          :selector-type.sync="dimensionSelectorType"
        ></expandable-dimension-selection>
      </v-col>
      <v-col cols="9" style="height: 100%">
        <v-card outlined class="mb-2" style="height: 64px">
          <v-card-text
            class="d-flex justify-end align-center py-0"
            style="height: 100%"
          >
            <share-graph-link-dialog
              :link-generator="generatePermanentLink"
              :share-options="[]"
            ></share-graph-link-dialog>
          </v-card-text>
        </v-card>
        <v-card style="height: calc(100% - 64px)">
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
import { Prop, Watch } from 'vue-property-decorator'
import RepoSelectionComponent from '@/components/misc/RepoSelectionComponent.vue'
import ShareGraphLinkDialog from '@/components/graphs/helper/ShareGraphLinkDialog.vue'
import { PermanentLinkOptions } from '@/store/modules/detailGraphStore'

@Component({
  components: {
    ShareGraphLinkDialog,
    RepoSelectionComponent,
    ExpandableDimensionSelection,
    StatusComparisonGraph,
    RepoBranchSelector
  }
})
export default class StatusComparison extends Vue {
  private data: StatusComparisonPoint[] = []

  @Prop({ default: '100%' })
  private readonly suggestedHeight!: string

  private get allDimensions() {
    return vxm.repoModule.occuringDimensions(
      vxm.repoModule.allRepos.map(it => it.id)
    )
  }

  private get selectedBranches() {
    return vxm.comparisonGraphModule.selectedBranches
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
    vxm.comparisonGraphModule.toggleRepoBranch(payload)
  }

  private setRepoBranches(payload: { repoId: string; branches: string[] }) {
    vxm.comparisonGraphModule.setSelectedBranchesForRepo(payload)
  }

  private get baselineRepoId() {
    const baselineId = vxm.statusComparisonModule.baselineRepoId

    // No baseline or doesn't exist anymore
    if (!baselineId || !vxm.repoModule.repoById(baselineId)) {
      return null
    }

    // Baseline is not selected => Ignore it
    if (!vxm.comparisonGraphModule.selectedBranches.has(baselineId)) {
      return null
    }

    return baselineId
  }

  private set baselineRepoId(id: RepoId | null) {
    vxm.statusComparisonModule.baselineRepoId = id
  }

  private get possibleBaselineRepos() {
    return Array.from(vxm.comparisonGraphModule.selectedBranches.keys())
      .map(id => vxm.repoModule.repoById(id))
      .filter(it => it)
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
      return null
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
    const baselineRepoId = vxm.statusComparisonModule.baselineRepoId

    if (baselineRepoId === null) {
      return null
    }

    const baselinePoint = this.data.find(it => it.repoId === baselineRepoId)!
    const run = baselinePoint.run!
    const result = run.result as RunResultSuccess
    return result.measurements.filter(it => it instanceof MeasurementSuccess)
  }

  private generatePermanentLink(options: PermanentLinkOptions) {
    return vxm.statusComparisonModule.permanentLink(options)
  }

  @Watch('selectedBranches')
  private async onBranchesChanged(
    newBranches: Map<RepoId, string[]>,
    oldBranches: Map<RepoId, string[]>
  ) {
    // This might happen if the repos are re-created. We only need to make an HTTP request on actual changes though
    // TODO: This should probably be handled in the store and not the callers?
    if (this.mapEquals(oldBranches, newBranches)) {
      return
    }
    await this.fetchData()
  }

  private mapEquals(
    first: Map<RepoId, string[]>,
    second: Map<RepoId, string[]>
  ) {
    if (first.size !== second.size) {
      return false
    }

    for (const [key, val] of first) {
      if (!second.has(key)) {
        return false
      }
      const firstValues = new Set(val)
      const secondValues = second.get(key)!

      if (firstValues.size !== secondValues.length) {
        return false
      }

      for (const secondValue of secondValues) {
        if (!firstValues.has(secondValue)) {
          return false
        }
      }
    }

    return true
  }

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
