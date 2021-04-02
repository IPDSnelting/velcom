<template>
  <v-card
    :outlined="selectedItem === null"
    :class="{ 'warning-outline': possibleDimensions.length !== 0 }"
    style="height: 100%"
  >
    <v-card-text
      :class="{ disabled: errorMessage !== null }"
      style="height: 100%"
    >
      <v-row class="my-0" align="center" style="height: 100%">
        <v-col
          v-if="errorMessage !== null"
          class="d-flex align-center text-warning py-0"
        >
          {{ errorMessage }}
        </v-col>
        <v-col v-if="errorMessage === null" class="py-0">
          <v-autocomplete
            :value="selectedItem"
            :items="items"
            @input="selectDimension"
            class="py-0"
            label="Dimension to compare…"
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
