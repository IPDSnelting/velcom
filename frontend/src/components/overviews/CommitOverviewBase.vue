<template>
  <v-card :outlined="outlined">
    <slot name="body_top"></slot>
    <v-list-item>
      <slot name="avatar" v-if="$vuetify.breakpoint.smAndUp"></slot>
      <v-list-item-content>
        <v-container fluid class="ma-0 pa-1">
          <v-row
            align="center"
            justify="center"
            v-if="!$vuetify.breakpoint.smAndUp"
            class="pa-0 ma-0"
          >
            <v-col cols="auto" class="pa-0 ma-0">
              <slot name="avatar"></slot>
            </v-col>
          </v-row>
          <v-row no-gutters align="center" justify="space-between">
            <v-col cols="12" sm="auto" class="flex-shrink-too mr-3">
              <v-list-item-title
                :class="
                  $vuetify.breakpoint.xs
                    ? ['d-flex', 'flex-wrap', 'justify-center']
                    : []
                "
              >
                <repo-display :repoId="commit.repoId"></repo-display>
                <span v-if="$vuetify.breakpoint.smAndUp" class="mx-2">—</span>
                <span
                  style="flex: 0 0 100%"
                  class="my-1"
                  v-if="$vuetify.breakpoint.xs"
                ></span>
                <router-link
                  class="concealed-link"
                  style="max-width: 100%"
                  :to="linkLocation || commitLinkLocation"
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
              <v-container
                fluid
                :class="$vuetify.breakpoint.mdAndUp ? ['ma-0', 'pa-0'] : []"
              >
                <v-row no-gutters align="center" justify="center">
                  <v-col cols="auto">
                    <text-chip :text="commit.hash"></text-chip>
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
import { formatDate, formatDateUTC } from '@/util/Times'
import { CommitDescription } from '@/store/types'
import InlineMinimalRepoNameDisplay from '../misc/InlineMinimalRepoDisplay.vue'
import TextChip from '../misc/TextChip.vue'
import { RawLocation } from 'vue-router'

@Component({
  components: {
    'repo-display': InlineMinimalRepoNameDisplay,
    'text-chip': TextChip
  }
})
export default class CommitOverviewBase extends Vue {
  @Prop()
  private commit!: CommitDescription

  @Prop({ default: false })
  private outlined!: boolean

  @Prop({ default: null })
  private linkLocation!: RawLocation

  private get formattedDate() {
    return formatDate(this.commit.authorDate || new Date(0))
  }

  private get formattedDateUTC() {
    return formatDateUTC(this.commit.authorDate || new Date(0))
  }

  private get commitLinkLocation(): RawLocation {
    return {
      name: 'run-detail',
      params: { first: this.commit.repoId, second: this.commit.hash }
    }
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
