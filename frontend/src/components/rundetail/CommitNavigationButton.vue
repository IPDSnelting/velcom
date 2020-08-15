<template>
  <v-tooltip :right="type === 'PARENT'" :left="type === 'CHILD'">
    <template #activator="{ on }">
      <v-btn
        v-on="on"
        text
        outlined
        :to="{
          name: 'run-detail',
          params: {
            first: commitDescription.repoId,
            second: commitDescription.hash,
          },
        }"
      >
        {{ commitDescription.summary }}
        <v-icon right>{{
          type === 'PARENT' ? parentCommitIcon : childCommitIcon
        }}</v-icon>
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
</style>
