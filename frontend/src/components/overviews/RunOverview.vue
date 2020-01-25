<template>
  <v-card>
    <v-list-item>
      <v-list-item-avatar>
        <v-tooltip top>
          <template #activator="{ on }">
            <v-icon v-if="isSuccessful" v-on="on" size="32px" color="success">{{ successIcon }}</v-icon>
          </template>
          This run was successfull!
        </v-tooltip>
        <v-tooltip top>
          <template #activator="{ on }">
            <v-icon v-if="!isSuccessful" color="error" v-on="on" size="32px">{{ errorIcon }}</v-icon>
          </template>
          This run suffered at least one failure :(
        </v-tooltip>
      </v-list-item-avatar>
      <v-list-item-content>
        <v-container fluid>
          <v-row no-gutters align="center">
            <v-col :cols="$scopedSlots['actions'] ? 8 : 9">
              <v-list-item-title>
                <repo-display :repoId="run.commit.repoID"></repo-display>
                <span class="mx-2">—</span>
                <router-link
                  class="concealed-link"
                  :to="{ name: 'commit-detail', params: { repoID: run.commit.repoID, hash: run.commit.hash } }"
                >
                  <span class="commit-message">{{ run.commit.message }}</span>
                </router-link>
              </v-list-item-title>
              <v-list-item-subtitle>
                <span class="author">{{ run.commit.author }}</span> authored on
                <span class="time" :title="formattedDateUTC">{{ formattedDate }}</span>
              </v-list-item-subtitle>
            </v-col>
            <v-col>
              <v-container fluid class="ma-0 pa-0">
                <v-row no-gutters align="center" justify="space-between">
                  <v-col cols="auto">
                    <commit-chip :commit="run.commit"></commit-chip>
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
import { Prop, Watch } from 'vue-property-decorator'
import { vxm } from '@/store/index'
import { Commit, Run } from '@/store/types'
import InlineMinimalRepoNameDisplay from '../InlineMinimalRepoDisplay.vue'
import CommitChip from '../CommitChip.vue'
import { formatDate, formatDateUTC } from '@/util/TimeUtil'
import { mdiCheckboxMarkedCircleOutline, mdiCloseCircleOutline } from '@mdi/js'

@Component({
  components: {
    'repo-display': InlineMinimalRepoNameDisplay,
    'commit-chip': CommitChip
  }
})
export default class RunOverview extends Vue {
  @Prop({})
  private run!: Run

  private get isSuccessful(): boolean {
    return !this.run.errorMessage && !!this.run.measurements
  }

  private get formattedDate() {
    return formatDate(this.run.commit.authorDate || 0)
  }

  private get formattedDateUTC() {
    return formatDateUTC(this.run.commit.authorDate || 0)
  }

  // ============== ICONS ==============
  private successIcon = mdiCheckboxMarkedCircleOutline
  private errorIcon = mdiCloseCircleOutline
  // ==============       ==============
}
</script>

<style scoped>
.commit-message {
  font-style: italic;
}
</style>
