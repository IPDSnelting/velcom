<template>
  <v-card>
    <slot name="body_top"></slot>
    <v-list-item>
      <slot name="avatar"></slot>
      <v-list-item-content>
        <v-container fluid class="ma-0 pa-1">
          <v-row no-gutters align="center" justify="space-between">
            <v-col cols="auto" class="flex-shrink-too mr-3">
              <v-list-item-title>
                <repo-display :repoId="commit.repoId"></repo-display>
                <span class="mx-2">—</span>
                <router-link
                  class="concealed-link"
                  :to="{
                    name: 'run-detail',
                    params: { first: commit.repoId, second: commit.hash }
                  }"
                >
                  <span class="commit-message">{{ commit.summary }}</span>
                </router-link>
              </v-list-item-title>
              <v-list-item-subtitle>
                <span class="author">{{ commit.author }}</span> authored on
                <span class="time" :title="formattedDateUTC">{{
                  formattedDate
                }}</span>
              </v-list-item-subtitle>
              <v-list-item-content v-if="$scopedSlots['content']" class="py-0">
                <slot name="content"></slot>
              </v-list-item-content>
            </v-col>
            <v-col cols="auto">
              <v-container fluid class="ma-0 pa-0">
                <v-row no-gutters align="center" justify="space-between">
                  <v-col cols="auto">
                    <commit-chip :commitHash="commit.hash"></commit-chip>
                  </v-col>
                  <span :class="$scopedSlots['actions'] ? ['pl-3'] : ['']">
                    <slot name="actions"></slot>
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
import { formatDate, formatDateUTC } from '@/util/TimeUtil'
import { CommitDescription } from '@/store/types'
import InlineMinimalRepoNameDisplay from '../InlineMinimalRepoDisplay.vue'
import CommitChip from '../CommitChip.vue'

@Component({
  components: {
    'repo-display': InlineMinimalRepoNameDisplay,
    'commit-chip': CommitChip
  }
})
export default class CommitOverviewBase extends Vue {
  @Prop()
  private commit!: CommitDescription

  private get formattedDate() {
    return formatDate(this.commit.authorDate || new Date(0))
  }

  private get formattedDateUTC() {
    return formatDateUTC(this.commit.authorDate || new Date(0))
  }
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
