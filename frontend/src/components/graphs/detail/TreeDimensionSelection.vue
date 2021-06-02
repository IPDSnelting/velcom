<template>
  <v-container fluid class="ma-0 pa-0 wrapper fill-height">
    <v-row no-gutters align="center" justify="start">
      <v-col>
        <v-text-field
          hide-details
          label="Search"
          v-model="search"
        ></v-text-field>
      </v-col>
    </v-row>
    <v-row no-gutters>
      <v-col cols="auto">
        <v-treeview
          :search="search"
          @input="changed"
          dense
          open-on-click
          selectable
          :open.sync="openItems"
          :items="benchmarkItems"
          :value="selectedItems"
        >
          <template #label="{ item, leaf }">
            <v-chip
              v-if="leaf"
              outlined
              label
              :input-value="leaf"
              :style="{ 'border-color': metricColor(item) }"
              class="cool-chip"
            >
              <span class="name">{{ item.name }}</span>
            </v-chip>
            <span v-else>{{ item.name }}</span>
          </template>
        </v-treeview>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { vxm } from '@/store'
import { Prop, Watch } from 'vue-property-decorator'
import { Dimension } from '@/store/types'
import { distinct, locallySorted } from '@/util/Arrays'

class BenchmarkItem {
  id: string
  name: string
  children: DimensionItem[]

  constructor(name: string, children: DimensionItem[]) {
    this.id = name
    this.name = name
    this.children = children.sort((a, b) => a.name.localeCompare(b.name))
  }
}

class DimensionItem {
  id: string
  dimension: Dimension
  name: string

  constructor(dimension: Dimension) {
    this.dimension = dimension
    this.id = dimension.toString()
    this.name = dimension.metric
  }
}

@Component
export default class TreeDimensionSelection extends Vue {
  private search: string = ''

  @Prop()
  private allDimensions!: Dimension[]

  @Prop()
  private selectedDimensions!: Dimension[]

  private selectedBenchmarkItems: string[] = []

  private openItems: string[] = []

  @Watch('search')
  private newSearch(search: string) {
    if (!search) {
      this.openItems = []
      return
    }

    const containsIgnoreCase = (id: string, value: string) =>
      id.toLocaleLowerCase().includes(value.toLocaleLowerCase())

    this.openItems = this.benchmarkItems
      .filter(
        it =>
          containsIgnoreCase(it.id, search) ||
          it.children.some(child => containsIgnoreCase(child.id, search))
      )
      .map(it => it.id)
  }

  private get benchmarkItems(): BenchmarkItem[] {
    return this.allBenchmarks.map(
      benchmark =>
        new BenchmarkItem(
          benchmark,
          this.allDimensions
            .filter(dimension => dimension.benchmark === benchmark)
            .map(dimension => new DimensionItem(dimension))
            .sort((a, b) => a.name.localeCompare(b.name))
        )
    )
  }

  private get dimensionItemMap(): Map<string, DimensionItem> {
    const map = new Map()
    this.benchmarkItems
      .flatMap(it => it.children)
      .forEach(it => map.set(it.dimension.toString(), it))
    return map
  }

  private get selectedItems(): string[] {
    const leaves = this.selectedDimensions
      .map(id => this.dimensionItemMap.get(id.toString()))
      .filter(it => it)
      .map(it => it!.id)

    return [...this.selectedBenchmarkItems, ...leaves]
  }

  private metricColor(item: DimensionItem | BenchmarkItem): string {
    if (!this.selectedDimensions) {
      return 'accent'
    }
    if (item instanceof DimensionItem) {
      return vxm.colorModule.colorByIndex(
        vxm.detailGraphModule.colorIndex(item.dimension)!
      )
    } else if (item.children) {
      return this.metricColor(item.children[0])
    } else {
      return 'accent'
    }
  }

  private changed(dimensions: string[]) {
    this.selectedBenchmarkItems = dimensions.filter(it =>
      this.benchmarkItems.find(a => a.id === it)
    )

    this.$emit(
      'update:selectedDimensions',
      dimensions
        .map(it => this.dimensionItemMap.get(it.toString()))
        .filter(it => it)
        .map(it => it!.dimension)
    )
  }

  private get allBenchmarks(): string[] {
    const benchmarks = this.allDimensions.map(it => it.benchmark)
    return locallySorted(distinct(benchmarks))
  }
}
</script>

<style scoped>
.wrapper {
  overflow: auto;
}
.name {
  opacity: 0.7;
}

.cool-chip {
  border-width: thick !important;
  border-style: dotted !important;
}
</style>
