<template>
  <v-snackbar v-model="displaySnackbar" :timeout="timeout" :color="color">
    <v-progress-circular v-if="loading" indeterminate></v-progress-circular>
    {{ text }}
    <v-btn dark color="ping" text @click="displaySnackbar = false">Close</v-btn>
  </v-snackbar>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import { ISnackbar } from '../util/Snackbar'

declare type Color = 'error' | 'success' | 'info'

@Component
export default class Snackbar extends Vue implements ISnackbar {
  private displaySnackbar: boolean = false
  private text: string = 'Hello world!'
  private timeout: number = 15 * 1000 // 15 seconds
  private color: Color = 'error'
  private loading: boolean = false

  setError(tag: string, error: string) {
    this.displayNormalText(this.appendTag(error, `'${tag}'`, ' for '), 'error')
  }

  setSuccess(tag: string, message: string) {
    this.displayNormalText(
      this.appendTag(message, `(${tag})`),
      'success',
      2 * 1000
    )
  }

  setLoading(tag: string) {
    this.displayNormalText(
      this.appendTag('Please stand by, loading', `'${tag}'`) + '...',
      'info',
      60 * 60 * 1000
    )
    this.loading = true
  }

  finishedLoading(tag: string) {
    this.displayNormalText(
      this.appendTag('Success', `'${tag}'`, ' for ') + '!',
      'success',
      2 * 1000
    )
  }

  private appendTag(text: string, tag: string, interpolation?: string): string {
    if (tag && tag.replace('()', '')) {
      return text + (interpolation || ' ') + tag
    }
    return text
  }

  private displayNormalText(
    text: string,
    color: Color,
    timeout: number = 15 * 1000
  ) {
    this.timeout = timeout
    this.text = text
    this.color = color
    this.displaySnackbar = false
    this.loading = false
    Vue.nextTick(() => (this.displaySnackbar = true))
  }

  created() {
    Vue.prototype.$globalSnackbar = this
  }
}
</script>

<style scoped>
</style>
