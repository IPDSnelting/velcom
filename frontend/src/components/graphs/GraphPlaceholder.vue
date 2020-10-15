<template>
  <v-skeleton-loader
    class="graph-placeholder-loader mt-5"
    height="500"
    loading
    type="image"
  ></v-skeleton-loader>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'

@Component
export default class GraphPlaceholder extends Vue {}
</script>

<style>
.graph-placeholder-loader .v-skeleton-loader__bone {
  height: 100% !important;
}
</style>
