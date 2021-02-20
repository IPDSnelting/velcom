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
                <span>{{ item.author }}</span> authored on
                <span :title="formattedDateUTC">{{ formattedDate }}</span>
              </v-list-item-subtitle>
            </v-col>
            <v-col cols="auto">
              <v-container fluid class="ma-0 pa-0">
                <v-row no-gutters align="center" justify="space-between">
                  <v-col cols="auto">
                    <commit-chip :commitHash="item.hash"></commit-chip>
                  </v-col>
                  <span class="pl-3" v-if="hasRun"> I has run </span>
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
import { CommitDescription } from '@/store/types'
import { RawLocation } from 'vue-router'
import { formatDate, formatDateUTC } from '@/util/TimeUtil'
import CommitChip from '@/components/CommitChip.vue'
import InlineMinimalRepoDisplay from '@/components/InlineMinimalRepoDisplay.vue'
import { mdiSourceCommit } from '@mdi/js'

@Component({
  components: {
    'repo-display': InlineMinimalRepoDisplay,
    CommitChip
  }
})
export default class SearchResultCommit extends Vue {
  @Prop()
  private readonly item!: CommitDescription

  private get commitLinkLocation(): RawLocation {
    return {
      name: 'run-detail',
      params: { first: this.item.repoId, second: this.item.hash }
    }
  }

  private get formattedDate() {
    return formatDate(this.item.authorDate || new Date(0))
  }

  private get formattedDateUTC() {
    return formatDateUTC(this.item.authorDate || new Date(0))
  }

  private hasRun() {
    return true
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
