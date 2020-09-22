<template>
  <v-container fluid class="ma-0 pa-0 wrapper">
    <v-row no-gutters align="center" justify="start">
      <v-col cols="12" sm="7" class="mr-4">
        <v-text-field label="Search" v-model="search"></v-text-field>
      </v-col>
      <v-col cols="auto mr-4 mb-2">
        <v-btn text color="primary" @click="changed([])"
          >Deselect all metrics
        </v-btn>
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

class BenchmarkItem {
  id: string
  name: string
  children: DimensionItem[]

  constructor(name: string, children: DimensionItem[]) {
    this.id = name
    this.name = name
    this.children = children
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
export default class DimensionSelection extends Vue {
  private search: string = ''

  @Prop()
  private repoId!: string

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
    const repo = vxm.repoModule.repoById(this.repoId)
    if (!repo) {
      return []
    }

    return vxm.repoModule.occuringBenchmarks([this.repoId]).map(
      benchmark =>
        new BenchmarkItem(
          benchmark,
          repo.dimensions
            .filter(dimension => dimension.benchmark === benchmark)
            .map(dimension => new DimensionItem(dimension))
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
        this.selectedDimensions.findIndex(it => it.equals(item.dimension))
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

    vxm.detailGraphModule.selectedDimensions = dimensions
      .map(it => this.dimensionItemMap.get(it.toString()))
      .filter(it => it)
      .map(it => it!.dimension)
    vxm.detailGraphModule.fetchDetailGraph()
  }
}
</script>

<style scoped>
.wrapper {
  max-height: 300px;
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
