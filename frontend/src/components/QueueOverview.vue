<template>
  <v-container>
    <v-data-iterator :items="queueItems" hide-default-footer>
      <template v-slot:header>
        <v-toolbar class="mb-2" color="indigo darken-5" dark flat>
          <v-toolbar-title>Queued commits (in planned execution order)</v-toolbar-title>
        </v-toolbar>
      </template>
      <template v-slot:default="props">
        <v-row>
          <v-col
            cols="12"
            class="my-1 py-0"
            v-for="(commit, index) in props.items"
            :key="commit.repoID + commit.hash"
          >
            <v-card>
              <v-list-item>
                <v-list-item-avatar class="index-indicator">{{ index + 1 }}</v-list-item-avatar>
                <v-list-item-content>
                  <v-row>
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
                    <v-col cols="4">
                      <v-chip
                        outlined
                        label
                        color="accent"
                        class="commit-hash-chip"
                        @click="copyToClipboard(commit.hash)"
                      >{{ commit.hash }}</v-chip>
                    </v-col>
                  </v-row>
                </v-list-item-content>
              </v-list-item>
            </v-card>
          </v-col>
        </v-row>
      </template>
    </v-data-iterator>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { vxm } from '../store/classIndex'
import { Commit } from '../store/types'
import InlineMinimalRepoNameDisplay from './InlineMinimalRepoDisplay.vue'

@Component({
  components: {
    'repo-display': InlineMinimalRepoNameDisplay
  }
})
export default class QueueOverview extends Vue {
  private get queueItems(): Commit[] {
    return vxm.queueModule.openTasks
  }

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
    myDate.setTime((Math.abs(date) % 1.8934156e9) * 1000)
    return myDate
  }

  private copyToClipboard(hash: string) {
    let selection = window.getSelection()
    if (selection && selection.toString() !== '') {
      // Do not overwrite user text selection
      return
    }
    navigator.clipboard
      .writeText(hash)
      .then(it => this.$globalSnackbar.setSuccess('Copied!'))
      .catch(error =>
        this.$globalSnackbar.setError('Could not copy to clipboard :( ' + error)
      )
  }

  mounted() {
    vxm.queueModule.fetchQueue()
  }
}
</script>

<style scoped>
.author {
  text-decoration: underline;
}

.index-indicator {
  font-weight: bold;
  font-size: 1.5em;
}

.commit-message {
  font-style: italic;
}

.commit-hash-chip {
  font-size: 0.8em;
}
</style>
