<template>
  <component
    :is="run.source.type"
    :commit="commit"
    :source="run.source"
    :id="run.runId"
  >
    <template #avatar>
      <v-list-item-avatar>
        <v-tooltip top v-if="isSuccessful">
          <template #activator="{ on }">
            <v-icon v-on="on" size="32px" color="success">{{
              successIcon
            }}</v-icon>
          </template>
          This run was successful :)
        </v-tooltip>
        <v-tooltip top v-else>
          <template #activator="{ on }">
            <v-icon
              :color="isCompleteFailure ? 'error' : 'orange'"
              v-on="on"
              size="32px"
              >{{ isCompleteFailure ? errorIcon : partialErrorIcon }}</v-icon
            >
          </template>
          <span v-if="isCompleteFailure">This run failed completely :(</span>
          <span v-else>This run suffered at least one failure :/</span>
        </v-tooltip>
      </v-list-item-avatar>
    </template>
    <template #actions v-if="!hideActions && commit" class="ml-3">
      <commit-benchmark-actions
        :hasExistingBenchmark="true"
        :commitDescription="commit"
      ></commit-benchmark-actions>
      <slot name="actions"></slot>
    </template>
    <template #content>
      <slot name="content"></slot>
    </template>
  </component>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import { vxm } from '@/store'
import {
  CommitDescription,
  CommitTaskSource,
  RunDescription
} from '@/store/types'
import {
  mdiCheckboxMarkedCircleOutline,
  mdiCloseCircleOutline,
  mdiAlertCircleCheckOutline
} from '@mdi/js'
import CommitBenchmarkActions from '../CommitBenchmarkActions.vue'
import CommitOverviewBase from './CommitOverviewBase.vue'
import TarTaskOverview from './TarTaskOverview.vue'

@Component({
  components: {
    'commit-benchmark-actions': CommitBenchmarkActions,
    COMMIT: CommitOverviewBase,
    UPLOADED_TAR: TarTaskOverview
  }
})
export default class RunOverview extends Vue {
  @Prop()
  private run!: RunDescription

  private get hideActions(): boolean {
    return !vxm.userModule.isAdmin
  }

  private get isSuccessful(): boolean {
    return this.run.success === 'SUCCESS'
  }

  private get isCompleteFailure(): boolean {
    return this.run.success === 'FAILURE'
  }

  // FIXME: Delete this?
  private get isPartialFailure(): boolean {
    return this.run.success === 'PARTIAL_SUCCESS'
  }

  private get commit(): CommitDescription | undefined {
    return this.run.source instanceof CommitTaskSource
      ? this.run.source.commitDescription
      : undefined
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
