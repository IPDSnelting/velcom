<template>
  <div class="queue">
    <v-container>
      <v-row align="baseline" justify="center">
        <h1>Queue Overview</h1>
      </v-row>
      <v-row align="start" justify="center">
        <v-col>
          <worker-overview :workers="workers"></worker-overview>
        </v-col>
      </v-row>
      <v-row align="baseline" justify="center">
        <v-col>
          <queue-overview></queue-overview>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import WorkerOverview from '../components/WorkerOverview.vue'
import QueueOverview from '../components/QueueOverview.vue'
import { vxm } from '../store/classIndex'

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
}
</script>
