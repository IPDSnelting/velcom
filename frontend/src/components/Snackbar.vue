<template>
  <v-snackbar
    shaped
    v-model="displaySnackbar"
    :timeout="timeout"
    :color="color"
  >
    <v-progress-circular
      class="mr-2"
      v-if="loading"
      indeterminate
    ></v-progress-circular>
    {{ text }}
    <template v-slot:action="{ attrs }">
      <v-btn
        dark
        color="ping"
        text
        v-bind="attrs"
        @click="displaySnackbar = false"
        >Close</v-btn
      >
    </template>
  </v-snackbar>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { ISnackbar } from '@/util/Snackbar'

declare type Color = 'error' | 'snackbarSuccess' | 'info'

@Component
export default class Snackbar extends Vue implements ISnackbar {
  private displaySnackbar: boolean = false
  private text: string = 'Hello world!'
  private timeout: number = 15 * 1000 // 15 seconds
  private color: Color = 'error'
  private loading: boolean = false
  private currentPriority: number = 1

  setError(tag: string, error: string, priority?: number): void {
    this.displayNormalText(
      this.appendTag(error, `'${tag}'`, ' for '),
      'error',
      15 * 1000,
      priority
    )
  }

  setSuccess(tag: string, message: string, priority?: number): void {
    this.displayNormalText(
      this.appendTag(message, `(${tag})`),
      'snackbarSuccess',
      2 * 1000,
      priority
    )
  }

  setLoading(tag: string, priority?: number): void {
    this.displayNormalText(
      this.appendTag('Please stand by, processing', `'${tag}'`) + '...',
      'info',
      1000 * 60 * 5, // ms * seconds * minutes
      priority
    )
    this.loading = true
  }

  finishedLoading(tag: string, priority?: number): void {
    const shouldHideExisting = this.currentPriority <= (priority || 1)

    if (shouldHideExisting && this.displaySnackbar) {
      this.displaySnackbar = false
    }
  }

  private appendTag(text: string, tag: string, interpolation?: string): string {
    if (tag && tag.replace('()', '').replace("''", '')) {
      return text + (interpolation || ' ') + tag
    }
    return text
  }

  private displayNormalText(
    text: string,
    color: Color,
    timeout: number,
    priority: number = 1
  ) {
    if (this.currentPriority > priority && this.displaySnackbar) {
      return
    }

    this.currentPriority = priority

    this.timeout = timeout
    this.text = text
    this.color = color
    this.displaySnackbar = false
    this.loading = false
    Vue.nextTick(() => (this.displaySnackbar = true))
  }

  created(): void {
    Vue.prototype.$globalSnackbar = this
  }
}
</script>
