<template>
  <v-card
    :outlined="selectedItem === null"
    :class="{ 'warning-outline': possibleDimensions.length !== 0 }"
  >
    <v-card-text :class="{ disabled: errorMessage !== null }">
      <v-row style="min-height: 86px">
        <v-col cols="auto" class="d-flex align-center">
          <div>Dimension to compare</div>
        </v-col>
        <v-col
          v-if="errorMessage !== null"
          class="d-flex align-center text-warning"
        >
          {{ errorMessage }}
        </v-col>
        <v-col v-if="errorMessage === null">
          <v-autocomplete
            :value="selectedItem"
            :items="items"
            @input="selectDimension"
            label="Enter a dimension..."
            return-object
            hide-details
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
  @Prop()
  private readonly possibleDimensions!: Dimension[]

  @Model('input')
  private readonly selectedDimension!: Dimension

  @Prop()
  private readonly errorMessage!: string | null

  private get items() {
    return this.possibleDimensions.map(dimension => new Item(dimension))
  }

  private get selectedItem() {
    if (!this.selectedDimension) {
      return null
    }
    if (
      !this.possibleDimensions.find(it => it.equals(this.selectedDimension))
    ) {
      return null
    }
    return (
      this.items.find(it => it.dimension.equals(this.selectedDimension)) || null
    )
  }

  private selectDimension(value: Item) {
    if (!value) {
      this.$emit('input', null)
    } else {
      this.$emit('input', value.dimension)
    }
  }
}
</script>

<!--suppress CssUnresolvedCustomProperty -->
<style scoped>
.text-warning {
  color: var(--v-warning-base) !important;
}
</style>

<!--suppress CssUnresolvedCustomProperty -->
<style>
/*noinspection CssUnusedSymbol*/
.warning-outline {
  border-color: var(--v-warning-base) !important;
}
</style>
