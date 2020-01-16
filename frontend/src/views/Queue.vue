<template>
  <div class="queue">
    <h1>This is the queue view ( that sort of rhymes...)</h1>
    <worker-overview></worker-overview>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import WorkerOverview from '../components/WorkerOverview.vue'

@Component({
  components: {
    'worker-overview': WorkerOverview
  }
})
export default class Queue extends Vue {}
</script>
