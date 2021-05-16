<template>
  <v-card outlined>
    <v-list-item>
      <v-icon large>{{ commitIcon }}</v-icon>
      <v-list-item-content>
        <v-container fluid class="my-0 py-0">
          <v-row no-gutters align="center" justify="space-between">
            <v-col cols="auto" class="flex-shrink-too mr-3">
              <v-list-item-title>
                <repo-display :repoId="item.repoId"></repo-display>
                <span class="mx-2">—</span>
                <router-link class="concealed-link" :to="commitLinkLocation">
                  <span class="commit-message">{{ item.summary }}</span>
                </router-link>
              </v-list-item-title>
              <v-list-item-subtitle>
                <span class="author">{{ item.author }}</span> authored on
                <span :title="formatDateUTC(item.authorDate)">
                  {{ formatDate(item.authorDate) }}
                </span>
                <div v-if="showCommitter">
                  <span class="author">{{ item.committer }}</span> committed on
                  <span :title="formatDateUTC(item.committerDate)">
                    {{ formatDate(item.committerDate) }}
                  </span>
                </div>
              </v-list-item-subtitle>
            </v-col>
            <v-col cols="auto">
              <v-container fluid class="ma-0 pa-0">
                <v-row no-gutters align="center" justify="space-between">
                  <v-col cols="auto">
                    <commit-chip :commitHash="item.hash"></commit-chip>
                  </v-col>
                  <span class="pl-3">
                    <slot name="compare-actions" :has-run="hasRun"></slot>
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
import { SearchItemCommit } from '@/store/types'
import { RawLocation } from 'vue-router'
import { formatDate, formatDateUTC } from '@/util/TimeUtil'
import CommitChip from '@/components/runs/CommitChip.vue'
import InlineMinimalRepoDisplay from '@/components/misc/InlineMinimalRepoDisplay.vue'
import { mdiSourceCommit } from '@mdi/js'

@Component({
  components: {
    'repo-display': InlineMinimalRepoDisplay,
    CommitChip
  }
})
export default class SearchResultCommit extends Vue {
  @Prop()
  private readonly item!: SearchItemCommit

  private get commitLinkLocation(): RawLocation {
    return {
      name: 'run-detail',
      params: { first: this.item.repoId, second: this.item.hash }
    }
  }

  private get showCommitter() {
    return this.item.committer !== this.item.author
  }

  private formatDate(date: Date) {
    return formatDate(date)
  }

  private formatDateUTC(date: Date) {
    return formatDateUTC(date)
  }

  private get hasRun() {
    return this.item.hasRun
  }

  private readonly commitIcon = mdiSourceCommit
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
