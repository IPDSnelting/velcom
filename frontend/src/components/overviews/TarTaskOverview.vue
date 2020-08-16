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
                <repo-display
                  v-if="source.repoId"
                  :repoId="source.repoId"
                ></repo-display>
                <span class="mx-2">—</span>
                <router-link
                  class="concealed-link"
                  :to="{ name: 'run-detail', params: { first: id } }"
                >
                  <span class="commit-message">{{ source.description }}</span>
                </router-link>
              </v-list-item-title>
              <v-list-item-content v-if="$scopedSlots['content']" class="py-0">
                <slot name="content"></slot>
              </v-list-item-content>
            </v-col>
            <v-col cols="auto">
              <v-container fluid class="ma-0 pa-0">
                <v-row no-gutters align="center" justify="space-between">
                  <v-col cols="auto">
                    <commit-chip :commitHash="id"></commit-chip>
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
import { Task, TarTaskSource, RunId, TaskId } from '../../store/types'
import CommitChip from '../CommitChip.vue'
import InlineMinimalRepoNameDisplay from '../InlineMinimalRepoDisplay.vue'

@Component({
  components: {
    'repo-display': InlineMinimalRepoNameDisplay,
    'commit-chip': CommitChip
  }
})
export default class TarTaskOverview extends Vue {
  @Prop()
  private source!: TarTaskSource
  @Prop()
  private id!: RunId | TaskId
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
