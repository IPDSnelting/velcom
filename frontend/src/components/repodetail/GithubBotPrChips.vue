<template>
  <div>
    <v-tooltip
      top
      v-for="(pr, index) in prs"
      :key="pr.prNumber + '' + pr.sourceCommentId"
    >
      <template v-slot:activator="{ on }">
        <router-link to="https://duckduckgo.com">
          <v-chip outlined label v-on="on" :class="{ 'ml-2': index > 0 }">
            PR #{{ pr.prNumber }}
            <v-icon right>
              {{ icon(pr.state) }}
            </v-icon>
          </v-chip>
        </router-link>
      </template>
      {{ description(pr.state) }}
    </v-tooltip>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import { GithubBotPr, GithubBotPrState } from '@/store/types'
import { mdiCheck, mdiCheckAll, mdiCircleSlice6 } from '@mdi/js'

type StateData = { color: string; icon: string; description: string }
const stateDate: { [key in GithubBotPrState]: StateData } = {
  seen: {
    icon: mdiCheck,
    description: 'PR was seen by the Github Bot'
  },
  reacted: {
    icon: mdiCheckAll,
    description: 'PR was acknowledged by the Github Bot'
  },
  queued: {
    icon: mdiCircleSlice6,
    description: 'PR is currently in the queue'
  }
}

@Component
export default class GithubBotPrChips extends Vue {
  @Prop()
  private readonly prs!: GithubBotPr[]

  private description(state: GithubBotPrState) {
    return stateDate[state].description
  }

  private icon(state: GithubBotPrState) {
    return stateDate[state].icon
  }
}
</script>
