<template>
  <v-chip
    outlined
    label
    color="accent"
    class="commit-hash-chip"
    @click="copyOnClick ? copyToClipboard(text) : undefined"
    :to="copyOnClick ? undefined : to"
    v-on="on"
  >
    {{ text }}
  </v-chip>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import { copyToClipboard } from '@/util/Clipboards'

@Component
export default class TextChip extends Vue {
  @Prop()
  private text!: string

  @Prop({ default: true })
  private copyOnClick!: boolean

  @Prop()
  private to: any

  @Prop()
  private on?: any

  private copyToClipboard(hash: string) {
    const selection = window.getSelection()
    if (selection && selection.toString()) {
      // Do not overwrite user text selection
      return
    }
    copyToClipboard(hash, this.$globalSnackbar)
  }
}
</script>

<style scoped>
.commit-hash-chip {
  font-family: monospace;
  font-size: 0.8em;

  user-select: text !important;
}
</style>
