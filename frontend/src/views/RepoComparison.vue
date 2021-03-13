<template>
  <v-container fluid>
    <v-row>
      <v-col cols="3">
        <v-row>
          <v-col>
            <comparison-dimension-selector
              :possible-dimensions="possibleDimensions"
              :error-message="dimensionSelectorErrorMessage"
              v-model="selectedDimension"
            ></comparison-dimension-selector>
          </v-col>
        </v-row>
        <v-row>
          <v-col>
            <repo-branch-selector></repo-branch-selector>
          </v-col>
        </v-row>
      </v-col>
      <v-col cols="9">
        <v-row>
          <v-col>
            <graph-timespan-controls
              :end-time.sync="endTime"
              :start-time.sync="startTime"
            ></graph-timespan-controls>
          </v-col>
        </v-row>
        <v-row>
          <v-col>
            <comparison-graph
              :comparison-datapoints="comparisonDatapoints"
            ></comparison-graph>
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

@Component({
  components: {
    ComparisonGraph,
    ComparisonDimensionSelector,
    GraphTimespanControls,
    RepoBranchSelector
  }
})
export default class RepoComparison extends Vue {
  private comparisonDatapoints: ComparisonDataPoint[] = []

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

  @Watch('selectedRepos')
  private async onSelectedRepoChange() {
    if (this.selectedRepos.length > 0) {
      await this.refetchData()
    } else {
      this.comparisonDatapoints = []
    }
  }

  @Watch('selectedDimension')
  @Watch('startTime')
  @Watch('endTime')
  private async refetchData() {
    if (this.possibleDimensions.length === 0) {
      this.comparisonDatapoints = []
      return
    }
    this.comparisonDatapoints = await vxm.comparisonGraphModule.fetchComparisonGraph()
  }
}
</script>
