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

  setError(error: string) {
    this.displayNormalText(error, 'error')
  }

  setSuccess(message: string) {
    this.displayNormalText(message, 'success', 2 * 1000)
  }

  setLoading() {
    this.displayNormalText('Please stand by...', 'info')
    this.loading = true
  }

  finishedLoading() {
    this.displayNormalText('Success!', 'success', 2 * 1000)
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
