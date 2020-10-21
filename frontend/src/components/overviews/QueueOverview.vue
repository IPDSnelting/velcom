<template>
  <v-container fluid class="my-0 py-0">
    <v-data-iterator
      :items="queueItems"
      :hide-default-footer="queueItems.length < defaultItemsPerPage"
      :items-per-page="defaultItemsPerPage"
      :footer-props="{ itemsPerPageOptions: itemsPerPageOptions }"
      no-data-text="No commits are currently enqueued."
    >
      <template #header v-if="isAdmin">
        <v-row>
          <v-spacer></v-spacer>
          <v-col cols="auto">
            <v-tooltip left>
              <template #activator="{ on }">
                <v-btn
                  v-on="on"
                  color="warning"
                  text
                  outlined
                  @click="cancelAllFetched()"
                  >Cancel all</v-btn
                >
              </template>
              Cancels
              <strong>all</strong> tasks you can see in the queue.
            </v-tooltip>
          </v-col>
        </v-row>
      </template>
      <template v-slot:default="{ items, pagination: { itemsPerPage, page } }">
        <v-row>
          <v-col
            cols="12"
            class="my-1 py-0"
            v-for="(task, index) in items"
            :key="task.id"
          >
            <component
              :is="task.source.type"
              :commit="task.source.commitDescription"
              :source="task.source"
              :id="task.id"
              :linkLocation="taskLinkLocation(task)"
            >
              <template #body_top>
                <v-progress-linear
                  indeterminate
                  v-if="inProgress(task)"
                  color="accent"
                ></v-progress-linear>
              </template>
              <template #avatar>
                <v-list-item-avatar class="index-indicator">{{
                  (page - 1) * itemsPerPage + index + 1
                }}</v-list-item-avatar>
              </template>
              <template #content v-if="getWorker(task)">
                <v-tooltip top>
                  <template #activator="{ on }">
                    <span style="flex: 0 0" class="pt-3">
                      <v-chip v-on="on" outlined label>
                        Running on » {{ getWorkerUnsafe(task).name }} « for
                        {{
                          workerFormattedSinceDurations[
                            getWorkerUnsafe(task).name
                          ]
                        }}
                      </v-chip>
                    </span>
                  </template>
                  <span style="white-space: pre; font-family: monospace">{{
                    getWorkerUnsafe(task).info
                  }}</span>
                </v-tooltip>
              </template>
              <template #actions v-if="isAdmin">
                <v-btn
                  icon
                  v-if="!inProgress(task)"
                  @click="liftToFront(task, $event)"
                >
                  <v-icon class="rocket">{{ liftToFrontIcon }}</v-icon>
                </v-btn>
                <v-progress-circular
                  indeterminate
                  color="accent"
                  class="mx-1"
                  v-else
                ></v-progress-circular>
                <v-btn icon @click="deleteTask(task)">
                  <v-icon color="red">{{ deleteIcon }}</v-icon>
                </v-btn>
              </template>
            </component>
          </v-col>
        </v-row>
      </template>
    </v-data-iterator>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { vxm } from '@/store'
import { Task, Worker } from '@/store/types'
import { mdiDelete, mdiRocket } from '@mdi/js'
import CommitOverviewBase from './CommitOverviewBase.vue'
import { extractErrorMessage } from '@/util/ErrorUtils'
import TarTaskOverview from './TarTaskOverview.vue'
import { formatDurationHuman } from '@/util/TimeUtil'
import { RawLocation } from 'vue-router'

@Component({
  components: {
    COMMIT: CommitOverviewBase,
    UPLOADED_TAR: TarTaskOverview
  }
})
export default class QueueOverview extends Vue {
  private timerIds: number[] = []
  // noinspection JSMismatchedCollectionQueryUpdate
  private itemsPerPageOptions: number[] = [10, 20, 50, 100, 200, -1]
  private defaultItemsPerPage: number = 20
  private liftsInProgress: Set<string> = new Set()
  // Reactivity can be a bit annoying. We need to store this explicitly
  private workerFormattedSinceDurations: { [workerName: string]: string } = {}

  private get queueItems(): Task[] {
    return vxm.queueModule.openTasks.slice()
  }

  private inProgress(task: Task): boolean {
    return this.getWorker(task) !== undefined
  }

  private get isAdmin() {
    return vxm.userModule.isAdmin
  }

  private taskLinkLocation(task: Task): RawLocation {
    return {
      name: 'task-detail',
      params: { taskId: task.id }
    }
  }

  private liftToFront(task: Task, event: Event) {
    if (this.liftsInProgress.has(task.id)) {
      return
    }
    this.liftsInProgress.add(task.id)

    let srcElement: HTMLElement = event.srcElement as HTMLElement

    // No animation possible
    if (!srcElement) {
      vxm.queueModule.dispatchPrioritizeOpenTask(task.id)
      return
    }

    while (!srcElement.classList.contains('v-icon')) {
      srcElement = srcElement.parentElement!

      // found no parent :/
      if (!srcElement) {
        vxm.queueModule.dispatchPrioritizeOpenTask(task.id)
        return
      }
    }

    const offsetTop = srcElement.getBoundingClientRect().top
    const offsetLeft = srcElement.getBoundingClientRect().left
    const parent = srcElement.parentElement!

    const startAngle = Math.random() * 2 * Math.PI
    srcElement.style.rotate = startAngle + 'rad'

    const clonedElement = srcElement.cloneNode(true) as HTMLElement

    vxm.queueModule
      .dispatchPrioritizeOpenTask(task.id)
      .then(() => {
        parent.appendChild(clonedElement)

        clonedElement.style.top = Math.round(offsetTop) + 'px'
        clonedElement.style.left = Math.round(offsetLeft) + 'px'
        clonedElement.classList.add('shoot-off')

        this.flyRocket(clonedElement, startAngle, offsetLeft, offsetTop)
      })
      .finally(() => {
        srcElement.style.rotate = '0deg'
        this.liftsInProgress.delete(task.id)
      })
  }

  private flyRocket(
    clonedElement: HTMLElement,
    startAngle: number,
    rawTargetX: number,
    rawTargetY: number
  ) {
    setTimeout(() => {
      let alpha = (Math.random() * Math.PI) / 4
      if (Math.random() < 0.5) {
        alpha *= -1
      }
      const rocketTilt = Math.PI / 4
      alpha += -startAngle + rocketTilt

      const direction = [Math.cos(alpha), Math.sin(alpha)]

      while (
        rawTargetY > 0 &&
        rawTargetY < window.innerHeight &&
        rawTargetX > 0 &&
        rawTargetX < window.innerWidth
      ) {
        rawTargetX += direction[0]
        rawTargetY -= direction[1]
      }

      clonedElement.style.top = rawTargetY + 'px'
      clonedElement.style.left = rawTargetX + 'px'
      clonedElement.style.rotate = -alpha + Math.PI / 4 + 'rad'

      const animationDuration = 6000
      setTimeout(() => clonedElement.remove(), animationDuration)
    }, 1)
  }

  private deleteTask(task: Task) {
    vxm.queueModule.dispatchDeleteOpenTask({ id: task.id })
  }

  private getWorker(task: Task): Worker | undefined {
    return vxm.queueModule.workers.find(it => it.workingOn === task.id)
  }

  // VTI inference is too bad to realize that never changes
  private getWorkerUnsafe(task: Task): Worker {
    return this.getWorker(task)!
  }

  private formatWorkingSince(worker: Worker) {
    if (!worker || !worker.workingSince) {
      return ''
    }
    return formatDurationHuman(worker.workingSince, new Date())
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

  private updateWorkerTimes() {
    vxm.queueModule.workers.forEach(it =>
      Vue.set(
        this.workerFormattedSinceDurations,
        it.name,
        this.formatWorkingSince(it)
      )
    )
  }

  created(): void {
    vxm.queueModule.fetchQueue()
    this.timerIds.push(
      setInterval(() => vxm.queueModule.fetchQueue(), 10 * 1000)
    )
    this.updateWorkerTimes()
    this.timerIds.push(setInterval(this.updateWorkerTimes, 1000))
  }

  // noinspection JSUnusedGlobalSymbols
  beforeDestroy(): void {
    this.timerIds.forEach(clearInterval)
  }

  // ============== ICONS ==============
  private liftToFrontIcon = mdiRocket
  private deleteIcon = mdiDelete
  // ==============       ==============
}
</script>

<style scoped>
.index-indicator {
  font-weight: bold;
  font-size: 1.5em;
}

.rocket:hover {
  animation: shake 4s linear;
  transform: translate3d(0, 0, 0);
  animation-iteration-count: infinite;
  animation-delay: 0s;
}

/*noinspection CssUnusedSymbol*/
.shoot-off {
  transition: top 1s ease-in, left 1s ease-in;
  position: fixed;
  z-index: 200;
  animation: shake 1s linear;
  animation-delay: 0s;
  animation-iteration-count: infinite;
}

@keyframes shake {
  0% {
    transform: translate3d(-1px, 1px, 0);
  }
  1% {
    transform: translate3d(2px, -2px, 0);
  }
  2% {
    transform: translate3d(2px, -2px, 0);
  }
  3% {
    transform: translate3d(1px, -1px, 0);
  }
  4% {
    transform: translate3d(1px, -1px, 0);
  }
  5% {
    transform: translate3d(0px, 0px, 0);
  }
  6% {
    transform: translate3d(2px, -2px, 0);
  }
  7% {
    transform: translate3d(-2px, 2px, 0);
  }
  8% {
    transform: translate3d(-2px, 2px, 0);
  }
  9% {
    transform: translate3d(2px, -2px, 0);
  }
  10% {
    transform: translate3d(-1px, 1px, 0);
  }
  11% {
    transform: translate3d(0px, 0px, 0);
  }
  12% {
    transform: translate3d(0px, 0px, 0);
  }
  13% {
    transform: translate3d(-2px, 2px, 0);
  }
  14% {
    transform: translate3d(0px, 0px, 0);
  }
  15% {
    transform: translate3d(0px, 0px, 0);
  }
  16% {
    transform: translate3d(2px, -2px, 0);
  }
  17% {
    transform: translate3d(2px, -2px, 0);
  }
  18% {
    transform: translate3d(1px, -1px, 0);
  }
  19% {
    transform: translate3d(-2px, 2px, 0);
  }
  20% {
    transform: translate3d(-1px, 1px, 0);
  }
  21% {
    transform: translate3d(0px, 0px, 0);
  }
  22% {
    transform: translate3d(-1px, 1px, 0);
  }
  23% {
    transform: translate3d(0px, 0px, 0);
  }
  24% {
    transform: translate3d(1px, -1px, 0);
  }
  25% {
    transform: translate3d(1px, -1px, 0);
  }
  26% {
    transform: translate3d(-2px, 2px, 0);
  }
  27% {
    transform: translate3d(-1px, 1px, 0);
  }
  28% {
    transform: translate3d(-1px, 1px, 0);
  }
  29% {
    transform: translate3d(0px, 0px, 0);
  }
  30% {
    transform: translate3d(1px, -1px, 0);
  }
  31% {
    transform: translate3d(-1px, 1px, 0);
  }
  32% {
    transform: translate3d(0px, 0px, 0);
  }
  33% {
    transform: translate3d(1px, -1px, 0);
  }
  34% {
    transform: translate3d(-1px, 1px, 0);
  }
  35% {
    transform: translate3d(0px, 0px, 0);
  }
  36% {
    transform: translate3d(2px, -2px, 0);
  }
  37% {
    transform: translate3d(2px, -2px, 0);
  }
  38% {
    transform: translate3d(2px, -2px, 0);
  }
  39% {
    transform: translate3d(0px, 0px, 0);
  }
  40% {
    transform: translate3d(-2px, 2px, 0);
  }
  41% {
    transform: translate3d(0px, 0px, 0);
  }
  42% {
    transform: translate3d(-2px, 2px, 0);
  }
  43% {
    transform: translate3d(0px, 0px, 0);
  }
  44% {
    transform: translate3d(-2px, 2px, 0);
  }
  45% {
    transform: translate3d(2px, -2px, 0);
  }
  46% {
    transform: translate3d(-1px, 1px, 0);
  }
  47% {
    transform: translate3d(-2px, 2px, 0);
  }
  48% {
    transform: translate3d(0px, 0px, 0);
  }
  49% {
    transform: translate3d(-2px, 2px, 0);
  }
  50% {
    transform: translate3d(-2px, 2px, 0);
  }
  51% {
    transform: translate3d(2px, -2px, 0);
  }
  52% {
    transform: translate3d(-2px, 2px, 0);
  }
  53% {
    transform: translate3d(2px, -2px, 0);
  }
  54% {
    transform: translate3d(-2px, 2px, 0);
  }
  55% {
    transform: translate3d(1px, -1px, 0);
  }
  56% {
    transform: translate3d(1px, -1px, 0);
  }
  57% {
    transform: translate3d(2px, -2px, 0);
  }
  58% {
    transform: translate3d(2px, -2px, 0);
  }
  59% {
    transform: translate3d(1px, -1px, 0);
  }
  60% {
    transform: translate3d(-2px, 2px, 0);
  }
  61% {
    transform: translate3d(2px, -2px, 0);
  }
  62% {
    transform: translate3d(0px, 0px, 0);
  }
  63% {
    transform: translate3d(-2px, 2px, 0);
  }
  64% {
    transform: translate3d(-2px, 2px, 0);
  }
  65% {
    transform: translate3d(-2px, 2px, 0);
  }
  66% {
    transform: translate3d(2px, -2px, 0);
  }
  67% {
    transform: translate3d(0px, 0px, 0);
  }
  68% {
    transform: translate3d(2px, -2px, 0);
  }
  69% {
    transform: translate3d(0px, 0px, 0);
  }
  70% {
    transform: translate3d(1px, -1px, 0);
  }
  71% {
    transform: translate3d(-2px, 2px, 0);
  }
  72% {
    transform: translate3d(0px, 0px, 0);
  }
  73% {
    transform: translate3d(2px, -2px, 0);
  }
  74% {
    transform: translate3d(0px, 0px, 0);
  }
  75% {
    transform: translate3d(1px, -1px, 0);
  }
  76% {
    transform: translate3d(1px, -1px, 0);
  }
  77% {
    transform: translate3d(2px, -2px, 0);
  }
  78% {
    transform: translate3d(-1px, 1px, 0);
  }
  79% {
    transform: translate3d(-2px, 2px, 0);
  }
  80% {
    transform: translate3d(2px, -2px, 0);
  }
  81% {
    transform: translate3d(-2px, 2px, 0);
  }
  82% {
    transform: translate3d(0px, 0px, 0);
  }
  83% {
    transform: translate3d(0px, 0px, 0);
  }
  84% {
    transform: translate3d(1px, -1px, 0);
  }
  85% {
    transform: translate3d(1px, -1px, 0);
  }
  86% {
    transform: translate3d(1px, -1px, 0);
  }
  87% {
    transform: translate3d(2px, -2px, 0);
  }
  88% {
    transform: translate3d(-1px, 1px, 0);
  }
  89% {
    transform: translate3d(0px, 0px, 0);
  }
  90% {
    transform: translate3d(2px, -2px, 0);
  }
  91% {
    transform: translate3d(-2px, 2px, 0);
  }
  92% {
    transform: translate3d(1px, -1px, 0);
  }
  93% {
    transform: translate3d(-2px, 2px, 0);
  }
  94% {
    transform: translate3d(-2px, 2px, 0);
  }
  95% {
    transform: translate3d(-2px, 2px, 0);
  }
  96% {
    transform: translate3d(-2px, 2px, 0);
  }
  97% {
    transform: translate3d(-2px, 2px, 0);
  }
  98% {
    transform: translate3d(-2px, 2px, 0);
  }
  99% {
    transform: translate3d(-2px, 2px, 0);
  }
}
</style>
