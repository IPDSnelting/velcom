<template>
  <div>
    <v-tooltip
      top
      v-for="(pr, index) in prs"
      :key="pr.prNumber + '' + pr.sourceCommentId"
    >
      <template v-slot:activator="{ on }">
        <a :href="commentLink(pr)" rel="noopener nofollow" target="_blank">
          <v-chip
            outlined
            label
            link
            v-on="on"
            :class="{ 'ml-2': index > 0 }"
            :color="color(pr.state)"
          >
            For PR #{{ pr.prNumber }}
            <v-icon right>
              {{ icon(pr.state) }}
            </v-icon>
          </v-chip>
        </a>
      </template>
      {{ description(pr.state) }}
    </v-tooltip>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import {
  mdiAlertCircleOutline,
  mdiCheck,
  mdiCheckAll,
  mdiCircleSlice6
} from '@mdi/js'
import { GithubBotCommand, GithubBotCommandState } from '@/store/types'

type StateData = {
  color?: string
  icon: string
  description: string
}
const stateDate: { [key in GithubBotCommandState]: StateData } = {
  NEW: {
    icon: mdiCheck,
    description: 'Command was seen by the Github Bot'
  },
  MARKED_SEEN: {
    icon: mdiCheckAll,
    description: 'Command was acknowledged by the Github Bot'
  },
  QUEUED: {
    color: 'success',
    icon: mdiCircleSlice6,
    description: 'Command is currently in the queue'
  },
  ERROR: {
    color: 'error',
    icon: mdiAlertCircleOutline,
    description: 'Error processing command'
  }
}

@Component
export default class GithubBotCommandChips extends Vue {
  @Prop()
  private readonly prs!: GithubBotCommand[]

  private description(state: GithubBotCommandState) {
    return stateDate[state].description
  }

  private icon(state: GithubBotCommandState) {
    return stateDate[state].icon
  }

  private color(state: GithubBotCommandState) {
    return stateDate[state].color
  }

  private commentLink(command: GithubBotCommand) {
    return `https://github.com/IPDSnelting/velcom/pull/${command.prNumber}#issuecomment-${command.sourceCommentId}`
  }
}
</script>
