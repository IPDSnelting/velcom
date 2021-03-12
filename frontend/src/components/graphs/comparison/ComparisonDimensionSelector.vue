<template>
  <v-card>
    <v-card-text>
      <v-row align="center">
        <v-col cols="auto">Dimension to compare</v-col>
        <v-col>
          <v-autocomplete
            :value="selectedItem"
            :items="items"
            @input="selectDimension"
            label="Enter a dimension..."
            return-object
            no-data-text="No repo(s) selected or no common dimensions"
          >
          </v-autocomplete>
        </v-col>
      </v-row>
    </v-card-text>
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Dimension } from '@/store/types'
import { Model, Prop } from 'vue-property-decorator'

class Item {
  readonly text: string
  readonly value: string
  readonly dimension: Dimension

  constructor(dimension: Dimension) {
    this.text = dimension.toString()
    this.value = dimension.toString()
    this.dimension = dimension
  }
}

@Component
export default class ComparisonDimensionSelector extends Vue {
  private selectedItem: Item | null = null

  @Prop()
  private readonly possibleDimensions: Dimension[]

  @Model('input')
  private readonly selectedDimension: Dimension

  private get items() {
    return this.possibleDimensions.map(dimension => new Item(dimension))
  }

  private selectDimension() {
    if (!this.selectedItem) {
      this.$emit('input', null)
    } else {
      this.$emit('input', this.selectedItem.dimension)
    }
  }
}
</script>
