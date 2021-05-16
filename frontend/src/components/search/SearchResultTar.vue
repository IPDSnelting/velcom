<template>
  <v-card outlined>
    <v-list-item>
      <v-icon large>{{ tarIcon }}</v-icon>
      <v-list-item-content>
        <v-container fluid class="my-0 py-0">
          <v-row no-gutters align="center" justify="space-between">
            <v-col cols="auto" class="flex-shrink-too mr-3">
              <v-list-item-title>
                <router-link class="concealed-link" :to="linkLocation">
                  {{ item.tarDescription }}
                </router-link>
              </v-list-item-title>
              <v-list-item-subtitle v-if="attachedRepoId">
                Attached to
                <repo-display :repo-id="attachedRepoId"></repo-display>
              </v-list-item-subtitle>
            </v-col>
            <v-col cols="auto">
              <v-container fluid class="ma-0 pa-0">
                <v-row no-gutters align="center" justify="space-between">
                  <v-col cols="auto">
                    <commit-chip :commitHash="item.id"></commit-chip>
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
import CommitChip from '@/components/runs/CommitChip.vue'
import InlineMinimalRepoDisplay from '@/components/misc/InlineMinimalRepoDisplay.vue'
import { mdiFolderZipOutline } from '@mdi/js'
import { SearchItemRun } from '@/store/types'

@Component({
  components: {
    'repo-display': InlineMinimalRepoDisplay,
    CommitChip
  }
})
export default class SearchResultTar extends Vue {
  @Prop()
  private readonly item!: SearchItemRun

  private get linkLocation(): RawLocation {
    return {
      name: 'run-detail',
      params: { first: this.item.id }
    }
  }

  private get attachedRepoId() {
    return this.item.repoId
  }

  private readonly tarIcon = mdiFolderZipOutline
}
</script>

<style scoped>
.flex-shrink-too {
  flex: 1 1 0;
  min-width: 200px;
}
</style>
