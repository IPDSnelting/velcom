<template>
  <v-container fluid>
    <v-timeline>
      <v-timeline-item
        v-for="(run, index) in sortedRuns"
        :key="run.runId"
        :color="runInfo(run).color"
        :small="!isSelected(run)"
        fill-dot
        :class="[index % 2 === 1 ? 'text-right' : '']"
        class="d-flex align-center"
      >
        <router-link
          :to="{ name: 'run-detail', params: { first: run.runId } }"
          :class="isSelected(run) ? ['text-h6'] : ['']"
          class="concealed-link"
        >
          {{ formatDate(run.startTime) }}
        </router-link>
        <template #icon>
          <v-tooltip :left="index % 2 === 0" :right="index % 2 === 1">
            <template #activator="{ on }">
              <v-icon :small="!isSelected(run)" v-on="on" dark>
                {{ runInfo(run).icon }}
              </v-icon>
            </template>
            {{ runInfo(run).explanation }}
          </v-tooltip>
        </template>
      </v-timeline-item>
    </v-timeline>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { RunDescription, RunId } from '@/store/types'
import { Prop } from 'vue-property-decorator'
import { formatDate } from '@/util/TimeUtil'
import {
  mdiCheckCircleOutline,
  mdiProgressAlert,
  mdiAlertCircleOutline
} from '@mdi/js'

@Component
export default class RunTimeline extends Vue {
  @Prop()
  private runs!: RunDescription[]
  @Prop({ default: null })
  private selectedRunId!: RunId | null

  private readonly runInfos: {
    [key: string]: { icon: string; color: string; explanation: string }
  } = {
    SUCCESS: {
      icon: mdiCheckCircleOutline,
      color: 'success',
      explanation: 'This run was successful'
    },
    PARTIAL_SUCCESS: {
      icon: mdiProgressAlert,
      color: 'warning',
      explanation: 'This run suffered at least one failure'
    },
    FAILURE: {
      icon: mdiAlertCircleOutline,
      color: 'error',
      explanation: 'This run failed completely'
    }
  }

  private get sortedRuns(): RunDescription[] {
    return this.runs
      .slice()
      .sort((a, b) => b.startTime.getTime() - a.startTime.getTime())
  }

  private formatDate(date: Date) {
    return formatDate(date)
  }

  private runInfo(run: RunDescription) {
    return this.runInfos[run.success]
  }

  private isSelected(run: RunDescription) {
    return run.runId === this.selectedRunId
  }
}
</script>
