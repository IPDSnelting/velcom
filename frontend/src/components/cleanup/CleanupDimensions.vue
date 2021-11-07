<template>
  <v-container>
    <v-row>
      <v-col>
        <v-card>
          <v-card-title>
            <v-toolbar color="toolbarColor" dark>Cleanup Dimension</v-toolbar>
          </v-card-title>
          <v-card-text>
            <v-container fluid class="mt-0 pt-0">
              <v-row justify="center">
                <v-col>
                  <v-data-table
                    v-model="selectedDimensions"
                    :headers="headers"
                    :items="allDimensions || []"
                    :items-per-page="-1"
                    :loading="allDimensions === null"
                    checkbox-color="primary"
                    item-key="key"
                    show-select
                    multi-sort
                    dense
                  >
                  </v-data-table>
                </v-col>
              </v-row>
            </v-container>
          </v-card-text>
          <v-card-actions class="d-flex justify-center" v-if="isAdmin">
            <v-dialog v-model="showDeleteDialog" width="700">
              <template #activator="{ on }">
                <v-btn
                  v-on="on"
                  color="warning"
                  text
                  outlined
                  @click="showDeleteDialog = true"
                  :disabled="selectedDimensions.length === 0"
                >
                  Delete selected dimensions
                </v-btn>
              </template>
              <v-card>
                <v-card-title>
                  <v-toolbar color="warning" dark>Confirm deletion</v-toolbar>
                </v-card-title>
                <v-card-text>
                  <div class="mx-2">
                    <div
                      class="subtitle-1 font-weight-bold d-inline-block mb-2"
                    >
                      You are about to irrevocably delete the following
                      dimensions and their measurements:
                    </div>
                    <ul>
                      <li
                        v-for="{ dimension } in selectedDimensions"
                        :key="dimension.toString()"
                      >
                        {{ dimension.toString() }}
                      </li>
                    </ul>
                    <br />
                    In total,
                    <span class="font-weight-bold subtitle-1">
                      {{ attachedMeasurementCount }} measurements
                    </span>
                    will be deleted.
                    <br />
                    Please confirm this by typing
                    <span
                      style="font-family: monospace"
                      class="font-weight-bold"
                      >{{ expectedConfirmationText }}</span
                    >
                    in the text field below.

                    <v-text-field
                      :placeholder="expectedConfirmationText"
                      v-model="confirmText"
                      hide-details
                    ></v-text-field>
                  </div>
                </v-card-text>
                <v-card-actions class="d-flex justify-center">
                  <v-btn
                    color="warning"
                    :disabled="!confirmedDeletion"
                    text
                    outlined
                    @click="deleteSelectedDimensions"
                  >
                    Confirm deletion
                  </v-btn>
                </v-card-actions>
              </v-card>
            </v-dialog>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Component from 'vue-class-component'
import Vue from 'vue'
import { CleanupDimension } from '@/store/types'
import { vxm } from '@/store'

@Component
export default class CleanupDimensions extends Vue {
  private readonly headers = [
    { text: 'Benchmark', value: 'dimension.benchmark', align: 'left' },
    { text: 'Metric', value: 'dimension.metric', align: 'left' },
    { text: 'Total run count', value: 'runs', align: 'left' },
    { text: 'Untracked run count', value: 'untrackedRuns', align: 'left' },
    { text: 'Unreachable run count', value: 'unreachableRuns', align: 'left' }
  ]

  private selectedDimensions: CleanupDimension[] = []
  private showDeleteDialog = false
  private confirmText: string = ''

  private get allDimensions(): CleanupDimension[] | null {
    if (vxm.cleanupModule.dimensions === null) {
      return null
    }
    return vxm.cleanupModule.dimensions
      .slice()
      .sort((a, b) =>
        a.dimension.toString().localeCompare(b.dimension.toString())
      )
  }

  private get attachedMeasurementCount() {
    return this.selectedDimensions.map(it => it.runs).reduce((a, b) => a + b, 0)
  }

  private get expectedConfirmationText() {
    return (
      'delete ' +
      this.attachedMeasurementCount +
      ' measurements and ' +
      this.selectedDimensions.length +
      ' dimensions'
    )
  }

  private get confirmedDeletion() {
    return this.confirmText === this.expectedConfirmationText
  }

  private get isAdmin() {
    return vxm.userModule.isAdmin
  }

  private async deleteSelectedDimensions() {
    await vxm.cleanupModule.deleteDimensions(
      this.selectedDimensions.map(it => it.dimension)
    )
    this.showDeleteDialog = false
    this.selectedDimensions = []
    await vxm.cleanupModule.fetchDimensions()
  }

  private async mounted() {
    await vxm.cleanupModule.fetchDimensions()
  }
}
</script>

<style scoped></style>
