<template>
  <v-container v-if="repo" class="ma-0 pa-0">
    <v-row>
      <v-col>
        <comparison-graph-settings
          :begin-y-at-zero.sync="beginYAtZero"
          :graph-component.sync="graphComponent"
          :day-equidistant-graph-selected.sync="dayEquidistantGraphSelected"
        >
          <v-col cols="auto">
            <share-graph-link-dialog
              :link-generator="getShareLink"
              data-restriction-label="Include repos and branches"
            />
          </v-col>
        </comparison-graph-settings>
      </v-col>
    </v-row>
    <v-row align="baseline" justify="center" no-gutters>
      <v-col class="ma-0 pa-0">
        <repo-graph
          :selected-graph-component.sync="graphComponent"
          :reload-graph-data-counter="reloadGraphDataCounter"
        ></repo-graph>
      </v-col>
    </v-row>
    <v-row align="baseline" justify="center" class="mt-2" no-gutters>
      <v-col>
        <repo-graph-controls
          @reload-graph-data="reloadGraphDataCounter++"
        ></repo-graph-controls>
      </v-col>
    </v-row>
    <v-row align="baseline" justify="center" class="mt-2" no-gutters>
      <v-col>
        <graph-timespan-controls
          @reload-graph-data="reloadGraphDataCounter++"
          :end-time.sync="endTime"
          :start-time.sync="startTime"
        ></graph-timespan-controls>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import RepoBaseInformation from '@/components/repodetail/RepoBaseInformation.vue'
import { vxm } from '@/store'
import RepoGraph from '@/components/repodetail/RepoGraph.vue'
import RepoGraphControls from '@/components/repodetail/RepoGraphControls.vue'
import GraphTimespanControls from '@/components/graphs/GraphTimespanControls.vue'
import ShareGraphLinkDialog from '@/views/ShareGraphLinkDialog.vue'
import ComparisonGraphSettings from '@/components/graphs/comparison/ComparisonGraphSettings.vue'
import { PermanentLinkOptions } from '@/store/modules/detailGraphStore'
import { availableGraphComponents } from '@/util/GraphVariantSelection'

@Component({
  components: {
    ComparisonGraphSettings,
    ShareGraphLinkDialog,
    GraphTimespanControls,
    RepoGraphControls,
    RepoGraph,
    RepoBaseInformation
  }
})
export default class RepoGraphView extends Vue {
  private reloadGraphDataCounter = 0
  private graphComponent: typeof Vue | null =
    availableGraphComponents[0].component

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

  private getShareLink(options: PermanentLinkOptions) {
    return vxm.comparisonGraphModule.permanentLink(options)
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
