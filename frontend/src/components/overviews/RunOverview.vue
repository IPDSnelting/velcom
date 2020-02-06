<template>
  <commit-overview-base :commit="commit">
    <template #avatar>
      <v-list-item-avatar>
        <v-tooltip top v-if="isSuccessful">
          <template #activator="{ on }">
            <v-icon v-on="on" size="32px" color="success">{{ successIcon }}</v-icon>
          </template>
          This run was successful :)
        </v-tooltip>
        <v-tooltip top v-else>
          <template #activator="{ on }">
            <v-icon
              :color="isCompleteFailure ? 'error' : 'orange'"
              v-on="on"
              size="32px"
            >{{ isCompleteFailure ? errorIcon : partialErrorIcon }}</v-icon>
          </template>
          <span v-if="isCompleteFailure">This run failed completely :(</span>
          <span v-else>This run suffered at least one failure :/</span>
        </v-tooltip>
      </v-list-item-avatar>
    </template>
    <template #actions v-if="!hideActions" class="ml-3">
      <commit-benchmark-actions
        :hasExistingBenchmark="true"
        @benchmark="benchmark(commit)"
        @benchmarkUpwards="benchmarkUpwards(commit)"
      ></commit-benchmark-actions>
      <slot name="actions"></slot>
    </template>
  </commit-overview-base>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop, Watch } from 'vue-property-decorator'
import { vxm } from '@/store/index'
import { Commit, Run } from '@/store/types'
import InlineMinimalRepoNameDisplay from '../InlineMinimalRepoDisplay.vue'
import CommitChip from '../CommitChip.vue'
import { formatDate, formatDateUTC } from '@/util/TimeUtil'
import {
  mdiCheckboxMarkedCircleOutline,
  mdiCloseCircleOutline,
  mdiAlertCircleCheckOutline
} from '@mdi/js'
import CommitBenchmarkActions from '../CommitBenchmarkActions.vue'
import CommitOverviewBase from './CommitOverviewBase.vue'

@Component({
  components: {
    'commit-benchmark-actions': CommitBenchmarkActions,
    'commit-overview-base': CommitOverviewBase
  }
})
export default class RunOverview extends Vue {
  @Prop()
  private run!: Run

  @Prop()
  private commit!: Commit

  @Prop({ default: false })
  private hideActions!: boolean

  private get isSuccessful(): boolean {
    return !this.isCompleteFailure && !this.isPartialFailure
  }

  private get isCompleteFailure(): boolean {
    return !!this.run.errorMessage
  }

  private get isPartialFailure(): boolean {
    let unsuccessfulMeasurement = this.run.measurements!.find(
      measurement => !measurement.successful
    )

    return !!unsuccessfulMeasurement
  }

  private benchmark(commit: Commit) {
    vxm.queueModule.dispatchPrioritizeOpenTask(commit)
  }

  private benchmarkUpwards(commit: Commit) {
    vxm.queueModule.dispatchQueueUpwardsOf(commit)
  }

  // ============== ICONS ==============
  private successIcon = mdiCheckboxMarkedCircleOutline
  private partialErrorIcon = mdiAlertCircleCheckOutline
  private errorIcon = mdiCloseCircleOutline
  // ==============       ==============
}
</script>

<style scoped>
.commit-message {
  font-style: italic;
}
.flex-shrink-too {
  flex: 1 1 0;
  min-width: 200px;
}
</style>
