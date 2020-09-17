<template>
  <div class="queue">
    <v-container>
      <v-row align="baseline" justify="center">
        <v-col>
          <v-card>
            <v-card-title>
              <v-toolbar color="primary darken-1" dark>
                Available Runners
              </v-toolbar>
            </v-card-title>
            <v-card-text>
              <v-container fluid>
                <v-row align="center">
                  <worker-overview :workers="workers"></worker-overview>
                </v-row>
              </v-container>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
      <v-row align="baseline" justify="center">
        <v-col>
          <v-card>
            <v-card-title>
              <v-toolbar color="primary darken-1" dark>
                Queued commits (in planned execution order)
              </v-toolbar>
            </v-card-title>
            <v-card-text>
              <v-container fluid class="ma-0 pa-0">
                <v-row align="center">
                  <queue-overview></queue-overview>
                </v-row>
              </v-container>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import WorkerOverview from '../components/overviews/WorkerOverview.vue'
import QueueOverview from '../components/overviews/QueueOverview.vue'
import { vxm } from '@/store'
import { Route, RawLocation } from 'vue-router'

@Component({
  components: {
    'worker-overview': WorkerOverview,
    'queue-overview': QueueOverview
  }
})
export default class Queue extends Vue {
  private get workers() {
    return vxm.queueModule.workers
  }

  beforeRouteLeave(
    to: Route,
    from: Route,
    next: (to?: RawLocation | false | ((vm: Vue) => any) | void) => void
  ): void {
    vxm.queueModule.setOpenTasks([])
    vxm.queueModule.setWorkers([])
    next()
  }
}
</script>
