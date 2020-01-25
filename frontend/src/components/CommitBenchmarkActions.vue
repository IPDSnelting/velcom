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
  </span>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { mdiHistory } from '@mdi/js'

/**
 * @events 'reBenchmark' and 'deleteBenchmarks'
 */
@Component
export default class CommitBenchmarkActions extends Vue {
  // ============== ICONS ==============
  private rebenchmarkIcon = mdiHistory
  // ==============       ==============
}
</script>
