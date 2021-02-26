<template>
  <v-skeleton-loader
    class="graph-placeholder-loader"
    :height="placeholderHeight"
    loading
    type="image"
  ></v-skeleton-loader>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'

@Component
export default class GraphPlaceholder extends Vue {
  @Prop({ default: 500 })
  private placeholderHeight!: number
}
</script>

<style>
.graph-placeholder-loader .v-skeleton-loader__bone {
  height: 100% !important;
}
</style>
