<template>
  <span v-if="isAdmin">
    <v-tooltip top>
      <template #activator="{ on }">
        <v-btn v-on="on" icon @click="$emit('benchmark', true)">
          <v-icon class="rocket">{{ hasExistingBenchmark ? rebenchmarkIcon : benchmarkIcon }}</v-icon>
        </v-btn>
      </template>
      <span v-if="hasExistingBenchmark">Re-runs all benchmarks for this commit</span>
      <span v-else>Runs all benchmarks for this commit</span>
    </v-tooltip>
  </span>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { mdiHistory, mdiFlash } from '@mdi/js'
import { Prop } from 'vue-property-decorator'
import { vxm } from '../store'

/**
 * @events 'benchmark'
 */
@Component
export default class CommitBenchmarkActions extends Vue {
  @Prop({ default: true })
  private hasExistingBenchmark!: boolean

  get isAdmin() {
    return vxm.userModule.isAdmin
  }

  // ============== ICONS ==============
  private rebenchmarkIcon = mdiHistory
  private benchmarkIcon = mdiFlash
  // ==============       ==============
}
</script>
