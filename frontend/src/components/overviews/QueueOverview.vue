<template>
  <v-container>
    <v-data-iterator
      :items="queueItems"
      :hide-default-footer="queueItems.length < defaultItemsPerPage"
      :items-per-page="defaultItemsPerPage"
      :footer-props="{ itemsPerPageOptions: itemsPerPageOptions }"
      no-data-text="No commits are currently enqueued."
    >
      <template v-slot:default="props">
        <v-row>
          <v-col
            cols="12"
            class="my-1 py-0"
            v-for="(commit, index) in props.items"
            :key="commit.repoID + commit.hash"
          >
            <v-card>
              <v-progress-linear indeterminate v-if="inProgress(commit)" color="accent"></v-progress-linear>
              <v-list-item>
                <v-list-item-avatar class="index-indicator">{{ index + 1 }}</v-list-item-avatar>
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
                            :title="formattedDateUTC(commit.authorDate)"
                          >{{ formattedDate(commit.authorDate) }}</span>
                        </v-list-item-subtitle>
                        <v-list-item-content v-if="getWorker(commit)">
                          <v-tooltip top>
                            <template #activator="{ on }">
                              <span style="flex: 0 0;">
                                <v-chip
                                  v-on="on"
                                  outlined
                                  label
                                >Running on » {{ getWorker(commit).name }} «</v-chip>
                              </span>
                            </template>
                            <span
                              style="white-space: pre; font-family: monospace;"
                            >{{ getWorker(commit).osData }}</span>
                          </v-tooltip>
                        </v-list-item-content>
                      </v-col>
                      <v-col>
                        <v-container fluid class="ma-0 pa-0">
                          <v-row no-gutters align="center" justify="space-between">
                            <v-col>
                              <commit-chip :commit="commit"></commit-chip>
                            </v-col>
                            <span>
                              <v-btn
                                icon
                                v-if="!inProgress(commit)"
                                @click="liftToFront(commit, $event)"
                              >
                                <v-icon class="rocket">{{ liftToFrontIcon }}</v-icon>
                              </v-btn>
                              <v-progress-circular indeterminate color="accent" v-else></v-progress-circular>
                              <v-btn icon @click="deleteCommit(commit)">
                                <v-icon color="red">{{ deleteIcon }}</v-icon>
                              </v-btn>
                            </span>
                          </v-row>
                        </v-container>
                      </v-col>
                    </v-row>
                  </v-container>
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
import { vxm } from '@/store/index'
import { Commit, Worker } from '@/store/types'
import InlineMinimalRepoNameDisplay from '../InlineMinimalRepoDisplay.vue'
import { mdiRocket, mdiDelete } from '@mdi/js'
import CommitChip from '../CommitChip.vue'
import { formatDateUTC, formatDate } from '../../util/TimeUtil'

@Component({
  components: {
    'repo-display': InlineMinimalRepoNameDisplay,
    'commit-chip': CommitChip
  }
})
export default class QueueOverview extends Vue {
  private timerId: number | undefined = undefined
  private itemsPerPageOptions: number[] = [10, 20, 50, 100, 200, -1]
  private defaultItemsPerPage: number = 20

  private get queueItems(): Commit[] {
    let openTasks = vxm.queueModule.openTasks.slice()
    vxm.queueModule.workers
      .map(it => it.currentTask)
      .filter(it => it)
      .sort((a, b) => a!.message!.localeCompare(b!.message!))
      .forEach(it => openTasks.unshift(it!))
    return openTasks
  }

  private formattedDateUTC(date: number) {
    return formatDateUTC(date)
  }

  private formattedDate(date: number) {
    return formatDate(date)
  }

  private inProgress(commit: Commit) {
    return vxm.queueModule.openTasks.indexOf(commit) < 0
  }

  private liftToFront(commit: Commit, event: Event) {
    let srcElement: HTMLElement = event.srcElement as HTMLElement

    // No animation possible
    if (!srcElement) {
      vxm.queueModule.dispatchPrioritizeOpenTask(commit)
      return
    }

    while (!srcElement.classList.contains('v-icon')) {
      srcElement = srcElement.parentElement!

      // found no parent :/
      if (!srcElement) {
        vxm.queueModule.dispatchPrioritizeOpenTask(commit)
        return
      }
    }

    let offsetTop = srcElement.getBoundingClientRect().top
    let offsetLeft = srcElement.getBoundingClientRect().left
    let parent = srcElement.parentElement!
    srcElement = srcElement.cloneNode(true) as HTMLElement

    vxm.queueModule.dispatchPrioritizeOpenTask(commit).then(() => {
      parent.appendChild(srcElement)

      srcElement.style.top = Math.round(offsetTop) + 'px'
      srcElement.style.left = Math.round(offsetLeft) + 'px'
      srcElement.classList.add('shoot-off')

      setTimeout(() => {
        let alpha = (Math.random() * Math.PI) / 2
        let sinAlpha = Math.sin(alpha)

        let hypotenuseLength = offsetTop / sinAlpha

        let targetX = hypotenuseLength * Math.cos(alpha) + offsetLeft
        let targetY = offsetTop - hypotenuseLength * Math.sin(alpha)

        srcElement.style.top = targetY + 'px'
        srcElement.style.left = targetX + 'px'

        const animationDuration = 1000
        setTimeout(() => srcElement.remove(), animationDuration)
      }, 1)
    })
  }

  private deleteCommit(commit: Commit) {
    vxm.queueModule.dispatchDeleteOpenTask(commit)
  }

  private getWorker(commit: Commit): Worker | undefined {
    return vxm.queueModule.workers
      .filter(it => it.currentTask)
      .find(
        it =>
          it.currentTask!.repoID === commit.repoID &&
          it.currentTask!.hash === commit.hash
      )
  }

  created() {
    vxm.queueModule.fetchQueue()
    this.timerId = setInterval(() => vxm.queueModule.fetchQueue(), 10 * 1000)
  }

  beforeDestroy() {
    if (this.timerId) {
      clearInterval(this.timerId)
    }
  }

  // ============== ICONS ==============
  private liftToFrontIcon = mdiRocket
  private deleteIcon = mdiDelete
  // ==============       ==============
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

.rocket:hover {
  animation: shake 4s linear;
  transform: translate3d(0, 0, 0);
  animation-iteration-count: infinite;
  animation-delay: 0;
}

.my-row-col {
  justify-content: space-between;
}

.shoot-off {
  transition: all 1s ease-in;
  position: fixed;
  z-index: 200;
  animation: shake 1s linear, rotate 1s linear;
  animation-delay: 0;
  animation-iteration-count: infinite;
}

@keyframes rotate {
  0% {
    rotate: 0deg;
  }
  100% {
    rotate: 360deg;
  }
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
