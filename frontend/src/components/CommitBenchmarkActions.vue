<template>
  <span>
    <v-tooltip top>
      <template #activator="{ on }">
        <v-btn v-on="on" icon @click="$emit('reBenchmark', true)">
          <v-icon class="rocket">{{ rebenchmarkIcon }}</v-icon>
        </v-btn>
      </template>
      Re-runs the benchmarks for this commit
    </v-tooltip>
    <v-tooltip top>
      <template #activator="{ on }">
        <v-btn v-on="on" icon @click="$emit('deleteBenchmarks', true)">
          <v-icon color="red">{{ deleteBenchmarksIcon }}</v-icon>
        </v-btn>
      </template>
      Deletes all benchmarks for this commit
    </v-tooltip>
  </span>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { mdiDelete, mdiHistory } from '@mdi/js'

/**
 * @events 'reBenchmark' and 'deleteBenchmarks'
 */
@Component
export default class CommitBenchmarkActions extends Vue {
  // ============== ICONS ==============
  private rebenchmarkIcon = mdiHistory
  private deleteBenchmarksIcon = mdiDelete
  // ==============       ==============
}
</script>
