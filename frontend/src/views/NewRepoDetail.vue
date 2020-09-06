<template>
  <v-container v-if="repoExists(id)">
    <v-row>
      <v-col>
        <repo-base-information :repo="repo"></repo-base-information>
      </v-col>
    </v-row>
    <v-row align="baseline" justify="center">
      <v-col>
        <v-card
          ><v-card-text class="ma-0 pa-0">
            <v-container fluid class="ma-0 px-5 pb-0">
              <v-row align="center" justify="space-between" no-gutters>
                <v-col :md="useMatrixSelector ? '' : '5'" sm="12" cols="12">
                  <v-btn
                    @click="useMatrixSelector = !useMatrixSelector"
                    text
                    color="primary"
                  >
                    <span v-if="useMatrixSelector">Use tree selector</span>
                    <span v-if="!useMatrixSelector">Use matrix selector</span>
                  </v-btn>
                  <matrix-measurement-id-selection
                    v-if="useMatrixSelector"
                    @input="selectedMeasurements = $event"
                    :selectedDimensions="selectedDimensions"
                    :repoId="id"
                  ></matrix-measurement-id-selection>
                </v-col>
              </v-row>
            </v-container>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { vxm } from '../store/index'
import RepoBaseInformation from '@/components/repodetail/RepoBaseInformation.vue'
import MeasurementIdSelection from '../components/graphs/DimensionSelection.vue'
import MatrixMeasurementIdSelection from '../components/graphs/MatrixDimensionSelection.vue'
import { Dimension, Repo } from '@/store/types'

@Component({
  components: {
    'repo-base-information': RepoBaseInformation,
    'matrix-measurement-id-selection': MatrixMeasurementIdSelection,
    'normal-measurement-id-selection': MeasurementIdSelection
  }
})
export default class RepoDetail extends Vue {
  private useMatrixSelector: boolean = false

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

  private set selectedDimensions(dimensions: Dimension[]) {
    vxm.detailGraphModule.selectedDimensions = dimensions
  }
}
</script>

<style scoped>
.hint {
  font-size: 14px;
  font-weight: 400;
  opacity: 0.7;
}
</style>
