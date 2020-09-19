<template>
  <span>
    <v-tooltip top v-if="displayTooltip">
      <template #activator="{ on }">
        <span v-on="on" class="text--disabled">
          {{ displayedValue }}
        </span>
      </template>
      {{ tooltipMessage }}
    </v-tooltip>
    <span v-else :style="{ color: color }">{{ displayedValue }}</span>
  </span>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'

@Component
export default class MeasurementValueDisplay extends Vue {
  @Prop()
  private value?: string

  @Prop()
  private color?: string

  @Prop({ default: 'This value was not provided' })
  private tooltipMessage!: string

  private get displayedValue(): string {
    return this.value !== undefined ? this.value : 'N/A'
  }

  private get displayTooltip(): boolean {
    return this.value === undefined
  }
}
</script>

<style scoped></style>
