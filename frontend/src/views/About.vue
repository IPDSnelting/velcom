<template>
  <div class="about">
    <v-container>
      <v-row align="baseline" justify="center">
        <h1>About VelCom</h1>
      </v-row>
      <v-row align="baseline" justify="center">
        <p class="text-center">
          "The name stands for 'Velocity Commit', and velocity is related to speed and
          performance.
          <br />It also sounds like a german saying 'welcome', which is
          nice and friendly."
        </p>
        <p class="text-center">
          We try to be as nice to you as possible. You'll loose your nerves brooding over
          benchmarks anyway.
        </p>
      </v-row>
      <v-row align="baseline" justify="center">
        <h1>Impressum</h1>
      </v-row>
      <v-row align="baseline" justify="center">
        <h1>...</h1>
      </v-row>
    </v-container>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'

@Component
export default class About extends Vue {}
</script>
