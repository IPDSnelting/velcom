<template>
  <v-card outlined>
    <v-list-item>
      <v-icon large>{{ runIcon }}</v-icon>
      <v-list-item-content>
        <v-container fluid class="my-0 py-0">
          <v-row no-gutters align="center" justify="space-between">
            <v-col cols="auto" class="flex-shrink-too mr-3">
              <v-list-item-title>
                <repo-display
                  v-if="item.repoId"
                  :repoId="item.repoId"
                ></repo-display>
                <span v-if="item.repoId" class="mx-2">—</span>
                <router-link class="concealed-link" :to="linkLocation">
                  {{ item.commitSummary }}
                </router-link>
              </v-list-item-title>
              <v-list-item-subtitle>
                Started at {{ startTimeString }} and ran for
                {{ durationString }}
              </v-list-item-subtitle>
            </v-col>
            <v-col cols="auto">
              <v-container fluid class="ma-0 pa-0">
                <v-row no-gutters align="center" justify="space-between">
                  <v-col cols="auto">
                    <text-chip :text="item.id"></text-chip>
                  </v-col>
                  <span class="pl-3">
                    <slot name="compare-actions" :has-run="true"></slot>
                  </span>
                </v-row>
              </v-container>
            </v-col>
          </v-row>
        </v-container>
      </v-list-item-content>
    </v-list-item>
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import { RawLocation } from 'vue-router'
import TextChip from '@/components/misc/TextChip.vue'
import InlineMinimalRepoDisplay from '@/components/misc/InlineMinimalRepoDisplay.vue'
import { mdiRunFast } from '@mdi/js'
import { SearchItemRun } from '@/store/types'
import { formatDate, formatDurationHuman } from '@/util/Times'

@Component({
  components: {
    'repo-display': InlineMinimalRepoDisplay,
    TextChip: TextChip
  }
})
export default class SearchResultRun extends Vue {
  @Prop()
  private readonly item!: SearchItemRun

  private get linkLocation(): RawLocation {
    return {
      name: 'run-detail',
      params: { first: this.item.id }
    }
  }

  private get startTimeString() {
    return formatDate(this.item.startTime)
  }

  private get durationString() {
    return formatDurationHuman(this.item.startTime, this.item.stopTime)
  }

  private readonly runIcon = mdiRunFast
}
</script>

<style scoped>
.flex-shrink-too {
  flex: 1 1 0;
  min-width: 200px;
}
.commit-hash {
  font-family: monospace;
}
</style>
