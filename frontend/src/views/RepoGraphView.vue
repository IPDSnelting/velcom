<template>
  <v-container v-if="repo" class="ma-0 pa-0" fluid>
    <v-row :class="{ 'flex-column-reverse': $vuetify.breakpoint.mdAndDown }">
      <v-col cols="12" lg="3">
        <expandable-dimension-selection
          :all-dimensions="allDimensions"
          :selected-dimensions.sync="selectedDimensions"
          :selector-type.sync="selectorType"
          @reload-graph-data="reloadGraphDataCounter++"
        >
        </expandable-dimension-selection>
      </v-col>
      <v-col cols="12" lg="9" class="pl-1">
        <v-row>
          <v-col cols="12">
            <comparison-graph-settings
              :begin-y-at-zero.sync="beginYAtZero"
              :stacked.sync="stacked"
              :normalized.sync="normalized"
              :graph-component.sync="graphComponent"
              :day-equidistant-graph-selected.sync="dayEquidistantGraphSelected"
            >
              <share-graph-link-dialog
                :link-generator="getShareLink"
                :share-options="shareOptions"
              />
            </comparison-graph-settings>
          </v-col>
          <v-col cols="12" class="pt-0">
            <repo-graph
              :selected-graph-component.sync="graphComponent"
              :reload-graph-data-counter="reloadGraphDataCounter"
            ></repo-graph>
          </v-col>
          <v-col cols="12" class="pt-1">
            <graph-timespan-controls
              @reload-graph-data="reloadGraphDataCounter++"
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
import RepoBaseInformation from '@/components/repodetail/RepoBaseInformation.vue'
import { vxm } from '@/store'
import RepoGraph from '@/components/graphs/detail/RepoGraph.vue'
import GraphDimensionSelector from '@/components/repodetail/GraphDimensionSelector.vue'
import GraphTimespanControls from '@/components/graphs/helper/GraphTimespanControls.vue'
import ShareGraphLinkDialog from '@/components/graphs/helper/ShareGraphLinkDialog.vue'
import GraphSettings from '@/components/graphs/helper/GraphSettings.vue'
import { PermanentLinkOptions } from '@/store/modules/detailGraphStore'
import { availableGraphComponents } from '@/util/GraphVariantSelection'
import ExpandableDimensionSelection from '@/components/graphs/helper/ExpandableDimensionSelection.vue'
import { Dimension } from '@/store/types'

@Component({
  components: {
    ExpandableDimensionSelection,
    DetailGraphDimensionSelector: GraphDimensionSelector,
    ComparisonGraphSettings: GraphSettings,
    ShareGraphLinkDialog,
    GraphTimespanControls,
    RepoGraph,
    RepoBaseInformation
  }
})
export default class RepoGraphView extends Vue {
  private reloadGraphDataCounter = 0
  private graphComponent: typeof Vue | null =
    availableGraphComponents[0].component

  // <!--<editor-fold desc="Dimension selection">-->
  private get allDimensions() {
    return this.repo
      ? this.repo.dimensions.filter(d => !d.benchmark.startsWith('~'))
      : []
  }

  private get selectedDimensions() {
    return vxm.detailGraphModule.selectedDimensions
  }

  private set selectedDimensions(dimensions: Dimension[]) {
    vxm.detailGraphModule.selectedDimensions = dimensions
  }

  private get selectorType() {
    return vxm.detailGraphModule.selectedDimensionSelector
  }

  private set selectorType(selector: 'tree' | 'matrix') {
    vxm.detailGraphModule.selectedDimensionSelector = selector
  }
  // <!--</editor-fold>-->

  private get endTime(): Date {
    return vxm.detailGraphModule.endTime
  }

  // noinspection JSUnusedLocalSymbols
  private set endTime(date: Date) {
    vxm.detailGraphModule.endTime = date
  }

  private get startTime(): Date {
    return vxm.detailGraphModule.startTime
  }

  // noinspection JSUnusedLocalSymbols
  private set startTime(date: Date) {
    vxm.detailGraphModule.startTime = date
  }

  private get repoId() {
    return vxm.detailGraphModule.selectedRepoId
  }

  private get repo() {
    return vxm.repoModule.repoById(this.repoId)
  }

  private get beginYAtZero() {
    return vxm.detailGraphModule.beginYScaleAtZero
  }

  // noinspection JSUnusedLocalSymbols
  private set beginYAtZero(beginYAtZero: boolean) {
    vxm.detailGraphModule.beginYScaleAtZero = beginYAtZero
  }

  private get stacked() {
    return vxm.detailGraphModule.stacked
  }

  // noinspection JSUnusedLocalSymbols
  private set stacked(stacked: boolean) {
    vxm.detailGraphModule.stacked = stacked
  }

  private get normalized() {
    return vxm.detailGraphModule.normalized
  }

  // noinspection JSUnusedLocalSymbols
  private set normalized(normalized: boolean) {
    vxm.detailGraphModule.normalized = normalized
  }

  private getShareLink(options: PermanentLinkOptions) {
    return vxm.detailGraphModule.permanentLink(options)
  }

  private get shareOptions() {
    return [
      {
        label: 'Use X-axis zoom instead of start/end date',
        selectable: true,
        unselectableMessage: 'That you see this is a bug. Please report it :)',
        key: 'includeXZoom'
      },
      {
        label: 'Include Y-axis zoom',
        selectable:
          vxm.detailGraphModule.zoomYStartValue !== null ||
          vxm.detailGraphModule.zoomYEndValue !== null,
        unselectableMessage: "You haven't zoomed the Y axis",
        key: 'includeYZoom'
      },
      {
        label: 'Include dimensions',
        selectable: vxm.detailGraphModule.selectedDimensions.length > 0,
        unselectableMessage: "You haven't selected any dimensions",
        key: 'includeDataRestrictions'
      }
    ]
  }

  private get dayEquidistantGraphSelected() {
    return vxm.detailGraphModule.dayEquidistantGraph
  }

  // noinspection JSUnusedLocalSymbols
  private set dayEquidistantGraphSelected(selected: boolean) {
    vxm.detailGraphModule.dayEquidistantGraph = selected
  }
}
</script>

<style scoped>
/*  https://stackoverflow.com/questions/53391733/untie-text-fields-icon-click-enabling-from-the-input-one */
.lock-button {
  pointer-events: auto;
}
</style>
