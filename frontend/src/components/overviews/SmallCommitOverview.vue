<template>
  <v-card>
    <v-list-item>
      <v-list-item-content>
        <v-container fluid>
          <v-row no-gutters align="center">
            <v-col cols="8">
              <v-list-item-title>
                <repo-display :repoId="commit.repoID"></repo-display>
                <span class="mx-2">—</span>
                <span class="commit-message">{{ commit.message }}</span>
              </v-list-item-title>
              <v-list-item-subtitle>
                <span class="author">{{ commit.author }}</span> authored on
                <span
                  class="time"
                  :title="formatDateUTC(commit.authorDate)"
                >{{ formatDate(commit.authorDate) }}</span>
              </v-list-item-subtitle>
            </v-col>
            <v-col>
              <v-container fluid class="ma-0 pa-0">
                <v-row no-gutters align="center" justify="space-between">
                  <v-col>
                    <commit-chip :commit="commit"></commit-chip>
                  </v-col>
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
import { Commit } from 'vuex'
import InlineMinimalRepoNameDisplay from '../InlineMinimalRepoDisplay.vue'
import CommitChip from '../CommitChip.vue'

@Component({
  components: {
    'repo-display': InlineMinimalRepoNameDisplay,
    'commit-chip': CommitChip
  }
})
export default class SmallCommitOverview extends Vue {
  @Prop()
  private commit!: Commit

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

<style scoped>
</style>
