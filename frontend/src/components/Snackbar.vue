<template>
  <v-snackbar v-model="displaySnackbar" :timeout="timeout">
    {{ text }}
    <v-btn color="pink" text @click="displaySnackbar = false">Close</v-btn>
  </v-snackbar>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'

@Component
export default class Snackbar extends Vue {
  private displaySnackbar: boolean = true
  private text: string = 'Hello world!'
  private timeout: number = 15 * 1000 // 15 seconds
}
</script>

<style scoped>
</style>
