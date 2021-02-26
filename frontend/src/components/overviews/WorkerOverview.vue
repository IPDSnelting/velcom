<template>
  <v-container fluid class="pa-0">
    <v-row>
      <v-col v-for="worker in workers" :key="worker.name" cols="auto">
        <v-card :disabled="worker.lostConnection">
          <v-card-title>
            {{ worker.name }}
            <span v-if="worker.lostConnection" class="ml-2 font-italic">
              (Lost connection)
            </span>
          </v-card-title>
          <v-card-text>
            <span class="worker-description">{{
              formatWorkerInformation(worker)
            }}</span>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col class="subtitle-1 text-center" v-if="workers.length === 0">
        <div class="font-weight-bold">No worker registered :(</div>
        <span>I will not be able to benchmark anything!</span>
        <br />Please setup a runner and point it to this instance.
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Worker } from '@/store/types'
import { Prop } from 'vue-property-decorator'

@Component
export default class WorkerOverview extends Vue {
  @Prop()
  private workers!: Worker[]

  private formatWorkerInformation(worker: Worker) {
    if (!worker.info) {
      return 'No data known :/'
    }
    return worker.info
  }
}
</script>

<style scoped>
.worker-description {
  white-space: pre-wrap;
  font-family: monospace;
}
</style>
