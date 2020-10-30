<template>
  <div class="queue">
    <v-container>
      <v-row align="baseline" justify="center">
        <v-col>
          <v-card>
            <v-card-title>
              <v-toolbar color="primary darken-1" dark>
                Available Runners
              </v-toolbar>
            </v-card-title>
            <v-card-text>
              <v-container fluid>
                <v-row align="center">
                  <worker-overview :workers="workers"></worker-overview>
                </v-row>
              </v-container>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
      <v-row align="baseline" justify="center">
        <v-col>
          <v-card>
            <v-card-title>
              <v-toolbar color="primary darken-1" dark>
                Queued commits (in planned execution order)
              </v-toolbar>
            </v-card-title>
            <v-card-text>
              <v-container fluid class="ma-0 pa-0">
                <v-row justify="end" no-gutters>
                  <v-col cols="auto" class="mb-5" v-if="isWebsiteAdmin">
                    <v-tooltip bottom>
                      <template #activator="{ on }">
                        <v-btn
                          v-on="on"
                          class="mr-5 ml-3"
                          outlined
                          color="primary"
                          text
                          @click="refetchRepos"
                        >
                          Refetch all repos
                        </v-btn>
                      </template>
                      Executes a `git fetch` updating the benchmark repo as well
                      all other repos. It should find any new commits and pick
                      up changes to the benchmark repo, but might take a few
                      seconds to complete.
                    </v-tooltip>
                  </v-col>
                  <v-col cols="auto" class="mb-5" v-if="isWebsiteAdmin">
                    <v-tooltip left>
                      <template #activator="{ on }">
                        <v-btn
                          v-on="on"
                          color="warning"
                          text
                          outlined
                          @click="cancelAllFetched()"
                        >
                          Cancel all Tasks
                        </v-btn>
                      </template>
                      Cancels
                      <strong>all</strong> tasks you can see in the queue.
                    </v-tooltip>
                  </v-col>
                </v-row>
                <v-row align="center">
                  <queue-overview></queue-overview>
                </v-row>
              </v-container>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import WorkerOverview from '../components/overviews/WorkerOverview.vue'
import QueueOverview from '../components/overviews/QueueOverview.vue'
import { vxm } from '@/store'
import { Route, RawLocation } from 'vue-router'
import { extractErrorMessage } from '@/util/ErrorUtils'

@Component({
  components: {
    'worker-overview': WorkerOverview,
    'queue-overview': QueueOverview
  }
})
export default class Queue extends Vue {
  private get workers() {
    return vxm.queueModule.workers
  }

  private get isWebsiteAdmin() {
    return vxm.userModule.isAdmin
  }

  private cancelAllFetched() {
    if (!window.confirm('Do you really want to empty the queue?')) {
      return
    }

    this.$globalSnackbar.setLoading('cancel-queue', 2)
    Promise.all(
      vxm.queueModule.openTasks.map(it => {
        return vxm.queueModule.dispatchDeleteOpenTask({
          id: it.id,
          suppressRefetch: true
        })
      })
    )
      .then(() => {
        this.$globalSnackbar.setSuccess(
          'cancel-queue',
          'Cancelled all open tasks!',
          2
        )
      })
      .catch(error => {
        this.$globalSnackbar.setError(
          'cancel-queue',
          extractErrorMessage(error),
          2
        )
      })
      .finally(() => {
        vxm.queueModule.fetchQueue()
      })
  }

  private async refetchRepos() {
    if (!this.isWebsiteAdmin) {
      return
    }
    await vxm.repoModule.triggerListenerFetch()

    this.$globalSnackbar.setSuccess(
      'listener',
      'Re-fetched repo and updated benchrepo'
    )
  }

  beforeRouteLeave(
    to: Route,
    from: Route,
    next: (to?: RawLocation | false | ((vm: Vue) => any) | void) => void
  ): void {
    vxm.queueModule.setOpenTasks([])
    vxm.queueModule.setWorkers([])
    next()
  }
}
</script>
