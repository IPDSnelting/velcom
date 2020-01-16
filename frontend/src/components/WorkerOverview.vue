<template>
  <v-container>
    <v-row>
      <v-col v-for="worker in workers" :key="worker.name" cols="auto">
        <v-card>
          <v-card-title>{{ worker.name }}</v-card-title>
          <v-card-text>
            <span class="worker-description">{{ formatWorkerInformation(worker) }}</span>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Worker, RootState, QueueState } from '../store/types'
import { Prop } from 'vue-property-decorator'
import { Store } from 'vuex'

@Component
export default class WorkerOverview extends Vue {
  @Prop({
    default: () => [
      new Worker(
        'Test Runner',
        'Linux amd64 4.19.94-1-lts, 8 cores, 4167MB max RAM'
      ),
      new Worker(
        'Test Runner 1',
        'Linux amd64dsdsdsds 4.19.94-1-lts, 8 cores, 4167MB max RAM'
      ),
      new Worker(
        'Test Runner 2',
        'Linux amd64 4.19.94dsdsdsds-1-lts, 8 cores, 4167MB max RAM'
      )
    ]
  })
  private workers!: Worker[]

  formatWorkerInformation(worker: Worker) {
    if (!worker.osData) {
      return 'No data known :/'
    }
    return worker.osData.split(/\s*,\s*/).join('\n')
  }
}
</script>

<style scoped>
.worker-description {
  white-space: pre-wrap;
  font-family: monospace;
}
</style>
