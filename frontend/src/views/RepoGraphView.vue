<template>
  <v-container v-if="repo" class="ma-0 pa-0">
    <v-row align="baseline" justify="center" no-gutters>
      <v-col class="ma-0 pa-0">
        <repo-graph
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

@Component({
  components: {
    GraphTimespanControls,
    RepoGraphControls,
    RepoGraph,
    RepoBaseInformation
  }
})
export default class RepoGraphView extends Vue {
  private reloadGraphDataCounter = 0

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
}
</script>

<style scoped>
/*  https://stackoverflow.com/questions/53391733/untie-text-fields-icon-click-enabling-from-the-input-one */
.lock-button {
  pointer-events: auto;
}
</style>
