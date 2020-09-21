<template>
  <v-tooltip :right="type === 'PARENT'" :left="type === 'CHILD'">
    <template #activator="{ on }">
      <v-btn
        v-on="on"
        left
        text
        outlined
        class="d-flex commit-navigation-button"
        :to="{
          name: 'run-detail',
          params: {
            first: commitDescription.repoId,
            second: commitDescription.hash
          }
        }"
      >
        <v-icon class="button-icon" left v-if="type === 'PARENT'">
          {{ parentCommitIcon }}
        </v-icon>
        <span class="button-text">{{ commitDescription.summary }}</span>
        <v-icon class="button-icon" right v-if="type === 'CHILD'">
          {{ childCommitIcon }}
        </v-icon>
      </v-btn>
    </template>
    {{ commitDescription.hash }} by {{ commitDescription.author }}
  </v-tooltip>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import { CommitDescription } from '@/store/types'
import { mdiArrowRight, mdiArrowLeft } from '@mdi/js'

@Component
export default class CommitNavigationButton extends Vue {
  @Prop()
  private commitDescription!: CommitDescription
  @Prop({ default: 'PARENT' })
  private type!: 'PARENT' | 'CHILD'

  // ICONS
  private parentCommitIcon = mdiArrowLeft
  private childCommitIcon = mdiArrowRight
}
</script>

<style scoped>
.button-icon {
  flex-shrink: 0;
}
.button-text {
  min-width: 10px;
  flex-shrink: 1;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>

<style>
.commit-navigation-button,
.commit-navigation-button .v-btn__content {
  max-width: 100%;
}
</style>
