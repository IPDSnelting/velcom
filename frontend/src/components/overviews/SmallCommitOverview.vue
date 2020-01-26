<template>
  <v-card>
    <v-list-item>
      <slot name="avatar"></slot>
      <v-list-item-content>
        <v-container fluid>
          <v-row no-gutters align="center">
            <v-col :cols="$scopedSlots['actions'] ? 8 : 9">
              <v-list-item-title>
                <repo-display :repoId="commit.repoID"></repo-display>
                <span class="mx-2">—</span>
                <router-link
                  class="concealed-link"
                  tag="span"
                  :to="{ name: 'commit-detail', params: { repoID: commit.repoID, hash: commit.hash } }"
                >
                  <span class="commit-message">{{ commit.summary }}</span>
                </router-link>
              </v-list-item-title>
              <v-list-item-subtitle>
                <span class="author">{{ commit.author }}</span> authored on
                <span class="time" :title="formattedDateUTC">{{ formattedDate }}</span>
              </v-list-item-subtitle>
            </v-col>
            <v-col>
              <v-container fluid class="ma-0 pa-0">
                <v-row no-gutters align="center" justify="space-between">
                  <v-col cols="auto">
                    <commit-chip :commit="commit"></commit-chip>
                  </v-col>
                  <span>
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
import InlineMinimalRepoNameDisplay from '../InlineMinimalRepoDisplay.vue'
import CommitChip from '../CommitChip.vue'
import { formatDate, formatDateUTC } from '@/util/TimeUtil'
import { Commit } from '../../store/types'

@Component({
  components: {
    'repo-display': InlineMinimalRepoNameDisplay,
    'commit-chip': CommitChip
  }
})
export default class SmallCommitOverview extends Vue {
  @Prop()
  private commit!: Commit

  get formattedDate() {
    return formatDate(this.commit.authorDate || 0)
  }

  get formattedDateUTC() {
    return formatDateUTC(this.commit.authorDate || 0)
  }
}
</script>

<style scoped>
.commit-message {
  font-style: italic;
}
</style>
