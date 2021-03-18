<template>
  <v-container fluid>
    <v-row>
      <v-col cols="3">
        <v-row>
          <v-col :style="{ 'min-height': 68 + 12 * 2 + 'px' }">
            <comparison-dimension-selector
              :possible-dimensions="possibleDimensions"
              :error-message="dimensionSelectorErrorMessage"
              v-model="selectedDimension"
            ></comparison-dimension-selector>
          </v-col>
        </v-row>
        <v-row class="mt-0">
          <v-col>
            <repo-branch-selector></repo-branch-selector>
          </v-col>
        </v-row>
      </v-col>
      <v-col cols="9" ref="graphColumn">
        <v-row>
          <v-col>
            <comparison-graph-settings
              :begin-y-at-zero.sync="beginYAtZero"
              :graph-component.sync="graphComponent"
              :day-equidistant-graph-selected.sync="dayEquidistantGraphSelected"
            ></comparison-graph-settings>
          </v-col>
        </v-row>
        <v-row class="mt-0">
          <v-col>
            <comparison-graph
              :comparison-datapoints="comparisonDatapoints"
              :begin-y-at-zero="beginYAtZero"
              :graph-component="graphComponent"
              :height="graphHeight + 'px'"
            ></comparison-graph>
          </v-col>
        </v-row>
        <v-row class="pt-0">
          <v-col class="pt-0">
            <graph-timespan-controls
              :end-time.sync="endTime"
              :start-time.sync="startTime"
            ></graph-timespan-controls>
          </v-col>
        </v-row>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import RepoBranchSelector from '@/components/graphs/comparison/RepoBranchSelector.vue'
import { vxm } from '@/store'
import GraphTimespanControls from '@/components/graphs/GraphTimespanControls.vue'
import ComparisonDimensionSelector from '@/components/graphs/comparison/ComparisonDimensionSelector.vue'
import { ComparisonDataPoint, Dimension, Repo } from '@/store/types'
import ComparisonGraph from '@/components/graphs/comparison/ComparisonGraph.vue'
import { Watch } from 'vue-property-decorator'
import ComparisonGraphSettings from '@/components/graphs/comparison/ComparisonGraphSettings.vue'
import { spaceDayEquidistant } from '@/util/DayEquidistantUtil'
import { availableGraphComponents } from '@/util/GraphVariantSelection'

@Component({
  components: {
    ComparisonGraphSettings,
    ComparisonGraph,
    ComparisonDimensionSelector,
    GraphTimespanControls,
    RepoBranchSelector
  }
})
export default class RepoComparison extends Vue {
  private comparisonDatapoints: ComparisonDataPoint[] = []
  private graphComponent: typeof Vue | null =
    availableGraphComponents[0].component

  private graphHeight: number = window.innerHeight

  private get startTime(): Date {
    return vxm.comparisonGraphModule.startTime
  }

  // noinspection JSUnusedLocalSymbols
  private set startTime(date: Date) {
    vxm.comparisonGraphModule.startTime = date
  }

  private get endTime(): Date {
    return vxm.comparisonGraphModule.endTime
  }

  // noinspection JSUnusedLocalSymbols
  private set endTime(date: Date) {
    vxm.comparisonGraphModule.endTime = date
  }

  private get selectedDimension() {
    return vxm.comparisonGraphModule.selectedDimension
  }

  // noinspection JSUnusedLocalSymbols
  private set selectedDimension(dimension: Dimension | null) {
    vxm.comparisonGraphModule.selectedDimension = dimension
  }

  private get dayEquidistantGraphSelected() {
    return vxm.comparisonGraphModule.dayEquidistantGraphSelected
  }

  // noinspection JSUnusedLocalSymbols
  private set dayEquidistantGraphSelected(selected: boolean) {
    vxm.comparisonGraphModule.dayEquidistantGraphSelected = selected
  }

  private get beginYAtZero() {
    return vxm.comparisonGraphModule.beginYAtZero
  }

  // noinspection JSUnusedLocalSymbols
  private set beginYAtZero(beginYAtZero: boolean) {
    vxm.comparisonGraphModule.beginYAtZero = beginYAtZero
  }

  private get possibleDimensions() {
    const participatingRepos: Repo[] = this.selectedRepos

    if (participatingRepos.length === 0) {
      return []
    }

    let possibleDimensions: Dimension[] = participatingRepos
      .pop()!
      .dimensions.slice()

    participatingRepos.forEach(repo => {
      const dimensionsInRepo = new Set(repo.dimensions.map(it => it.toString()))
      possibleDimensions = possibleDimensions.filter(it =>
        dimensionsInRepo.has(it.toString())
      )
    })

    possibleDimensions.sort((a, b) => a.toString().localeCompare(b.toString()))

    return possibleDimensions
  }

  private get dimensionSelectorErrorMessage() {
    if (this.possibleDimensions.length > 0) {
      return null
    }
    if (vxm.comparisonGraphModule.selectedBranches.size === 0) {
      return 'No repo selected'
    }
    return 'No mutual dimensions'
  }

  private get selectedRepos(): Repo[] {
    return Array.from(vxm.comparisonGraphModule.selectedBranches.entries())
      .filter(([, value]) => value.length > 0)
      .map(([key]) => vxm.repoModule.repoById(key)!)
  }

  private applyDatapointTransformations(datapoints: ComparisonDataPoint[]) {
    return this.dayEquidistantGraphSelected
      ? spaceDayEquidistant(datapoints)
      : datapoints.map(it => it.positionedAt(it.committerTime))
  }

  @Watch('selectedRepos')
  private async onSelectedRepoChange() {
    if (this.selectedRepos.length > 0) {
      await this.refetchData()
    } else {
      this.comparisonDatapoints = []
    }
  }

  @Watch('dayEquidistantGraphSelected')
  private onDayEquidistantChanged() {
    this.comparisonDatapoints = this.applyDatapointTransformations(
      this.comparisonDatapoints
    )
  }

  private get shouldNotShowResults() {
    if (this.possibleDimensions.length === 0) {
      return true
    }
    const possibleDimension = this.possibleDimensions.find(dim =>
      dim.equals(this.selectedDimension)
    )
    return possibleDimension === undefined
  }

  @Watch('selectedDimension')
  @Watch('startTime')
  @Watch('endTime')
  private async refetchData() {
    if (this.shouldNotShowResults) {
      this.comparisonDatapoints = []
      return
    }
    this.comparisonDatapoints = this.applyDatapointTransformations(
      await vxm.comparisonGraphModule.fetchComparisonGraph()
    )
  }

  private adjustGraphHeight() {
    const graphColumn = this.$refs['graphColumn'] as HTMLElement
    const app = document.querySelector('#app') as HTMLElement

    const hasScrollBar = app.scrollHeight > window.innerHeight
    const causedByGraph = graphColumn.scrollHeight >= window.innerHeight
    if (hasScrollBar && causedByGraph) {
      this.graphHeight -= app.scrollHeight - window.innerHeight
    }
  }

  private onResized() {
    // Set the graph height to always cause a scroll bar. This simplifies the further calculations as we can assume we
    // need to shrink the graph, but never grow it.
    this.graphHeight = window.innerHeight

    // Wait for the browser to reflow the page after the above update, then shrink the graph
    window.requestAnimationFrame(() => {
      this.adjustGraphHeight()
    })
  }

  // noinspection JSUnusedLocalSymbols
  private mounted() {
    this.onResized()
    window.addEventListener('resize', this.onResized)
  }

  // noinspection JSUnusedLocalSymbols
  private beforeDestroy() {
    window.removeEventListener('resize', this.onResized)
  }
}
</script>
