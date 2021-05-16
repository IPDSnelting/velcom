<template>
  <div class="queue">
    <v-container>
      <v-row align="baseline" justify="center">
        <v-col>
          <v-card>
            <v-card-title>
              <v-toolbar color="toolbarColor" dark>
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
              <v-toolbar color="toolbarColor" dark>
                Queued commits (in planned execution order)
              </v-toolbar>
            </v-card-title>
            <v-card-text>
              <v-container fluid class="ma-0 pa-0">
                <v-row justify="end" no-gutters>
                  <v-col cols="auto" class="mb-5" v-if="isAdmin">
                    <upload-tar-dialog
                      v-model="uploadTarDialogOpen"
                    ></upload-tar-dialog>
                    <v-btn
                      color="primary"
                      text
                      outlined
                      @click="uploadTarDialogOpen = true"
                    >
                      Upload Tar
                    </v-btn>
                  </v-col>
                  <v-spacer></v-spacer>
                  <v-col cols="auto" class="mb-5" v-if="isAdmin">
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
                  <v-col cols="auto" class="mb-5" v-if="isAdmin">
                    <v-tooltip left>
                      <template #activator="{ on }">
                        <v-btn
                          v-on="on"
                          color="warning"
                          text
                          outlined
                          @click="cancelAllTasks()"
                        >
                          Cancel all tasks
                        </v-btn>
                      </template>
                      Cancels
                      <strong>all</strong> tasks you can see in the queue.
                    </v-tooltip>
                  </v-col>
                </v-row>
                <v-row align="center" class="mt-0">
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
import WorkerOverview from '../components/queue/WorkerOverview.vue'
import QueueOverview from '../components/queue/QueueOverview.vue'
import { vxm } from '@/store'
import { Route, RawLocation } from 'vue-router'
import UploadTarDialog from '@/components/queue/UploadTarDialog.vue'

@Component({
  components: {
    UploadTarDialog,
    'worker-overview': WorkerOverview,
    'queue-overview': QueueOverview
  }
})
export default class Queue extends Vue {
  private uploadTarDialogOpen: boolean = false

  private get workers() {
    return vxm.queueModule.workers
  }

  private get isAdmin() {
    return vxm.userModule.isAdmin
  }

  private async cancelAllTasks() {
    if (!window.confirm(`Do you really want to cancel all tasks?`)) {
      return
    }

    await vxm.queueModule.dispatchDeleteAllOpenTasks()
    this.$globalSnackbar.setSuccess('cancel-queue', 'Cancelled Queue')
  }

  private async refetchRepos() {
    if (!this.isAdmin) {
      return
    }
    await vxm.repoModule.triggerListenerFetch()

    this.$globalSnackbar.setSuccess(
      'listener',
      'Re-fetched repo and updated benchrepo',
      2
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
