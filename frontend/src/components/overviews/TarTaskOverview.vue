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
                <span class="mx-2" v-if="source.repoId">—</span>
                <router-link class="concealed-link" :to="linkLocation">
                  <span class="tar-message">{{ source.description }}</span>
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
                    <text-chip :text="id"></text-chip>
                  </v-col>
                  <span :class="$scopedSlots['actions'] ? ['pl-3'] : ['']">
                    <slot name="actions"></slot>
                  </span>
                </v-row>
              </v-container>
            </v-col>
            <v-col cols="auto" class="ml-3">
              <v-tooltip top>
                <template #activator="{ on }">
                  <v-btn v-on="on" small icon :to="compareRunLocation">
                    <v-icon>{{ compareIcon }}</v-icon>
                  </v-btn>
                </template>
                Compare this run with another
              </v-tooltip>
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
import { TarTaskSource, RunId, TaskId } from '@/store/types'
import TextChip from '../misc/TextChip.vue'
import InlineMinimalRepoNameDisplay from '../misc/InlineMinimalRepoDisplay.vue'
import { RawLocation } from 'vue-router'
import { mdiScaleBalance } from '@mdi/js'

@Component({
  components: {
    'repo-display': InlineMinimalRepoNameDisplay,
    'text-chip': TextChip
  }
})
export default class TarTaskOverview extends Vue {
  @Prop()
  private readonly source!: TarTaskSource

  @Prop()
  private readonly id!: RunId | TaskId

  @Prop()
  private readonly linkLocation!: RawLocation

  private get compareRunLocation() {
    return {
      name: 'search',
      params: {
        runId: this.id
      }
    }
  }

  // ===== ICONS =====
  private compareIcon = mdiScaleBalance
}
</script>

<style scoped>
.tar-message {
  font-style: italic;
}
.flex-shrink-too {
  flex: 1 1 0;
  min-width: 200px;
}
</style>
