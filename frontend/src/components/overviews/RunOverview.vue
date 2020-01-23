<template>
  <v-card>
    <v-list-item>
      <v-list-item-content>
        <v-container fluid>
          <v-row no-gutters align="center">
            <v-col cols="8">
              <v-list-item-title>
                <repo-display :repoId="run.commit.repoID"></repo-display>
                <span class="mx-2">—</span>
                <router-link
                  class="ml-3 mx-auto"
                  :to="{ name: 'commit-detail', params: { repoID: run.commit.repoID, hash: run.commit.hash } }"
                  tag="button"
                >
                  <span class="commit-message">{{ run.commit.message }}</span>
                </router-link>
              </v-list-item-title>
              <v-list-item-subtitle>
                <span class="author">{{ run.commit.author }}</span> authored on
                <span
                  class="time"
                  :title="formatDateUTC(run.commit.authorDate)"
                >{{ formatDate(run.commit.authorDate) }}</span>
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

@Component({
  components: {
    'repo-display': InlineMinimalRepoNameDisplay,
    'commit-chip': CommitChip
  }
})
export default class RunOverview extends Vue {
  @Prop({})
  private run!: Run

  private formatDate(date: number): string {
    let myDate = this.getDate(date)

    return myDate.toLocaleString()
  }

  private formatDateUTC(date: number): string {
    let myDate = this.getDate(date)

    return myDate.toUTCString()
  }

  private getDate(date: number): Date {
    let myDate = new Date()
    // TODO: remove clamping
    myDate.setTime((Math.abs(date) % 1.8934156e9) * 1000)
    return myDate
  }
}
</script>
