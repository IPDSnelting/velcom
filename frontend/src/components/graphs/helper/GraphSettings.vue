<template>
  <v-card>
    <v-card-text>
      <v-row align="center" justify="space-between">
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
        <v-col cols="auto" style="gap: 12px" class="d-flex flex-wrap">
          <v-btn
            @click="$emit('update:stacked', !stacked)"
            color="primary"
            outlined
            text
          >
            Stack Charts
            <v-icon class="ml-2" style="margin-right: -6px">
              {{ stacked ? iconOn : iconOff }}
            </v-icon>
          </v-btn>
          <v-btn
            @click="$emit('update:beginYAtZero', !beginYAtZero)"
            color="primary"
            outlined
            text
          >
            Start Y-Axis at zero
            <v-icon class="ml-2" style="margin-right: -6px">
              {{ beginYAtZero ? iconOn : iconOff }}
            </v-icon>
          </v-btn>
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
          <slot></slot>
        </v-col>
      </v-row>
    </v-card-text>
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { availableGraphComponents } from '@/util/GraphVariantSelection'
import { Prop } from 'vue-property-decorator'
import { mdiCheckboxBlankOutline, mdiCheckboxMarked } from '@mdi/js'

@Component
export default class GraphSettings extends Vue {
  @Prop()
  private graphComponent!: typeof Vue

  @Prop({ default: true })
  private beginYAtZero!: boolean

  @Prop({ default: false })
  private stacked!: boolean

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
