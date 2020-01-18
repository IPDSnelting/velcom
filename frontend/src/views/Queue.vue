<template>
  <div class="queue">
    <h1>This is the queue view ( that sort of rhymes...)</h1>
    <worker-overview></worker-overview>
    <queue-overview></queue-overview>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import WorkerOverview from '../components/WorkerOverview.vue'
import QueueOverview from '../components/QueueOverview.vue'

@Component({
  components: {
    'worker-overview': WorkerOverview,
    'queue-overview': QueueOverview
  }
})
export default class Queue extends Vue {}
</script>
