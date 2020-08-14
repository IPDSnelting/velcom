<template>
  <span v-if="isAdmin">
    <v-tooltip top>
      <template #activator="{ on }">
        <v-btn v-on="on" icon @click="benchmark">
          <v-icon class="rocket">{{
            hasExistingBenchmark ? rebenchmarkIcon : benchmarkIcon
          }}</v-icon>
        </v-btn>
      </template>
      <span v-if="hasExistingBenchmark"
        >Re-runs all benchmarks for this commit</span
      >
      <span v-else>Runs all benchmarks for this commit</span>
    </v-tooltip>
    <v-tooltip top>
      <template #activator="{ on }">
        <v-btn icon v-on="on" @click="benchmarkUpwards">
          <v-icon>{{ benchmarkUpwardsIcon }}</v-icon>
        </v-btn>
      </template>
      Benchmarks all commits upwards of this commit (this
      <strong>one</strong> and <strong>up</strong>)
    </v-tooltip>
  </span>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { mdiHistory, mdiFlash, mdiOneUp } from '@mdi/js'
import { Prop } from 'vue-property-decorator'
import { vxm } from '../store'
import { Commit } from '../store/types'

@Component
export default class CommitBenchmarkActions extends Vue {
  @Prop({ default: true })
  private hasExistingBenchmark!: boolean

  @Prop()
  private commit!: Commit

  get isAdmin() {
    return vxm.userModule.isAdmin
  }

  private benchmark() {
    vxm.queueModule.startManualTask({
      repoId: this.commit.repoID,
      hash: this.commit.hash
    })
  }

  private benchmarkUpwards() {
    vxm.queueModule.dispatchQueueUpwardsOf(this.commit)
  }

  // ============== ICONS ==============
  private rebenchmarkIcon = mdiHistory
  private benchmarkIcon = mdiFlash
  private benchmarkUpwardsIcon = mdiOneUp
  // ==============       ==============
}
</script>
