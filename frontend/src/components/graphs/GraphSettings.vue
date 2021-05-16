<template>
  <v-card>
    <v-card-text>
      <v-row align="center">
        <v-col cols="auto">
          <v-btn-toggle v-model="selectedGraphVariant" mandatory dense>
            <v-btn
              v-for="{ component, name } in availableGraphComponents"
              :key="name"
              :value="component"
            >
              {{ name }}
            </v-btn>
          </v-btn-toggle>
        </v-col>
        <v-spacer></v-spacer>
        <v-col cols="auto">
          <v-btn
            @click="$emit('update:beginYAtZero', !beginYAtZero)"
            color="primary"
            outlined
            text
          >
            Start Y-Axis at zero
            <v-icon class="ml-2" style="margin-right: -6px">{{ beginYAtZero ? iconOn : iconOff }}</v-icon>
          </v-btn>
        </v-col>
        <v-col cols="auto">
          <v-btn
            color="primary"
            outlined
            text
            @click="
              $emit(
                'update:dayEquidistantGraphSelected',
                !dayEquidistantGraphSelected
              )
            "
          >
            Use Day-Equidistant Graph
            <v-icon class="ml-2" style="margin-right: -6px">
              {{ dayEquidistantGraphSelected ? iconOn : iconOff }}
            </v-icon>
          </v-btn>
        </v-col>
        <slot></slot>
      </v-row>
    </v-card-text>
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { availableGraphComponents } from '@/util/GraphVariantSelection'
import { Prop } from 'vue-property-decorator'
import {
  mdiCheckboxBlankOff,
  mdiCheckboxBlankOutline,
  mdiCheckboxMarked
} from '@mdi/js'

@Component
export default class GraphSettings extends Vue {
  @Prop()
  private graphComponent!: typeof Vue

  @Prop({ default: true })
  private beginYAtZero!: boolean

  @Prop({ default: true })
  private dayEquidistantGraphSelected!: boolean

  private get selectedGraphVariant() {
    return this.graphComponent
  }

  // noinspection JSUnusedLocalSymbols
  private set selectedGraphVariant(variant: typeof Vue) {
    this.$emit('update:graphComponent', variant)
  }

  private get availableGraphComponents() {
    return availableGraphComponents
  }

  // ICONS
  private readonly iconOff = mdiCheckboxBlankOutline
  private readonly iconOn = mdiCheckboxMarked
}
</script>
