<template>
  <v-container fluid class="pa-0">
    <v-row>
      <v-col v-for="worker in workers" :key="worker.name" cols="auto">
        <v-card v-if="!dense">
          <v-card-title>{{ worker.name }}</v-card-title>
          <v-card-text>
            <span class="worker-description">{{ formatWorkerInformation(worker) }}</span>
          </v-card-text>
        </v-card>
        <v-tooltip bottom>
          <template #activator="{ on }">
            <v-chip outlined label color="accent" v-on="on" v-if="dense">{{ worker.name }}</v-chip>
          </template>
          <span class="worker-description">{{ formatWorkerInformation(worker) }}</span>
        </v-tooltip>
      </v-col>
      <v-col class="subtitle-1 text-center" v-if="workers.length === 0">
        <div class="font-weight-bold">No worker registered :(</div>
        <span>I will not be able to benchmark anything!</span>
        <br />I'd be grateful if you could spare a worker, I promise I will take good care of it!
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Worker, Commit } from '@/store/types'
import { Prop } from 'vue-property-decorator'
import { Store } from 'vuex'

@Component
export default class WorkerOverview extends Vue {
  @Prop()
  private workers!: Worker[]

  @Prop({ default: false })
  private dense!: boolean

  formatWorkerInformation(worker: Worker) {
    if (!worker.info) {
      return 'No data known :/'
    }
    return worker.info.split(/\s*,\s*/).join('\n')
  }
}
</script>

<style scoped>
.worker-description {
  white-space: pre-wrap;
  font-family: monospace;
}
</style>
