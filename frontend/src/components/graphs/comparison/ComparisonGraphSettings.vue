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
            <span v-if="beginYAtZero">Begin Y-Axis at minimum value</span>
            <span v-else>Begin Y-Axis at zero</span>
          </v-btn>
        </v-col>
        <v-col cols="auto">
          <v-btn
            color="primary"
            outlined
            text
            class="mr-4"
            @click="
              $emit(
                'update:dayEquidistantGraphSelected',
                !dayEquidistantGraphSelected
              )
            "
          >
            <span v-if="dayEquidistantGraphSelected">
              Disable Day-Equidistant Graph
            </span>
            <span v-else>Enable Day-Equidistant Graph</span>
          </v-btn>
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

@Component
export default class ComparisonGraphSettings extends Vue {
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
}
</script>
