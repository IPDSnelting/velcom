<template>
  <v-card>
    <v-list-item>
      <v-list-item-content>
        <v-container fluid>
          <v-row no-gutters align="center">
            <v-col cols="9">
              <v-list-item-title>
                <repo-display :repoId="run.commit.repoID"></repo-display>
                <span class="mx-2">—</span>
                <router-link
                  class="router-link"
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
                  <v-col>
                    <commit-chip :commit="run.commit"></commit-chip>
                  </v-col>
                  <span></span>
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

@Component({
  components: {
    'repo-display': InlineMinimalRepoNameDisplay,
    'commit-chip': CommitChip
  }
})
export default class RunOverview extends Vue {
  @Prop({})
  private run!: Run

  get formattedDate() {
    return formatDate(this.run.commit.authorDate || 0)
  }

  get formattedDateUTC() {
    return formatDateUTC(this.run.commit.authorDate || 0)
  }
}
</script>

<style scoped>
.commit-message {
  font-style: italic;
}

.router-link {
  text-decoration: none;
  color: rgba(0, 0, 0, 0.9);
}
</style>
