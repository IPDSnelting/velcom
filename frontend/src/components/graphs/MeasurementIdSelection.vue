<template>
  <v-container fluid class="ma-0 pa-0 wrapper">
    <v-row no-gutters>
      <v-col cols="autp">
        <v-text-field label="Search" v-model="search"></v-text-field>
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
          :items="benchmarkItems"
          :value="selectedItems"
        >
          <template #label="{ item, leaf }">
            <v-chip
              v-if="leaf"
              outlined
              label
              :input-value="leaf"
              :color="leaf ? metricColor(item) : 'accent'"
              class="cool-chip"
            >
              <span class="name">{{ item.name }}</span>
            </v-chip>
            <span v-else>{{ item.name}}</span>
          </template>
        </v-treeview>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { vxm } from '../../store'
import { Prop, Watch, Model } from 'vue-property-decorator'
import { MeasurementID } from '../../store/types'

class BenchmarkItem {
  id: string
  name: string
  children: MeasurementItem[]

  constructor(id: string, children: MeasurementItem[]) {
    this.id = id
    this.name = id
    this.children = children
  }
}

class MeasurementItem {
  id: string
  measurementId: MeasurementID
  name: string

  constructor(id: MeasurementID) {
    this.measurementId = id
    this.id = id.toString()
    this.name = id.metric
  }
}

@Component
export default class MeasurementIdSelection extends Vue {
  private search: string = ''

  @Prop()
  private repoId!: string

  @Prop()
  private selectedMeasurements!: MeasurementID[]

  private selectedBenchmarkItems: string[] = []

  private get benchmarkItems(): BenchmarkItem[] {
    return vxm.repoModule.occuringBenchmarks([this.repoId]).map(benchmark => {
      let metrics = vxm.repoModule
        .metricsForBenchmark(benchmark)
        .map(
          metric => new MeasurementItem(new MeasurementID(benchmark, metric))
        )
      return new BenchmarkItem(benchmark, metrics)
    })
  }

  private get measurementItemMap(): Map<string, MeasurementItem> {
    let map = new Map()
    this.benchmarkItems
      .flatMap(it => it.children)
      .forEach(it => map.set(it.measurementId.toString(), it))
    return map
  }

  private get selectedItems(): string[] {
    let leafs = this.selectedMeasurements
      .map(id => this.measurementItemMap.get(id.toString()))
      .filter(it => it)
      .map(it => it!.id)

    let got = [...this.selectedBenchmarkItems, ...leafs]
    console.log('Got: ', got)

    return got
  }

  private metricColor(item: MeasurementItem | BenchmarkItem): string {
    if (!this.selectedMeasurements) {
      return 'accent'
    }
    if (item instanceof MeasurementItem) {
      return vxm.colorModule.colorByIndex(
        this.selectedMeasurements.findIndex(it => it.equals(item.measurementId))
      )
    } else if (item.children) {
      return this.metricColor(item.children[0])
    } else {
      return 'accent'
    }
  }

  private changed(measurements: string[]) {
    this.selectedBenchmarkItems = measurements.filter(it =>
      this.benchmarkItems.find(a => a.id === it)
    )

    let ids = measurements
      .map(it => this.measurementItemMap.get(it))
      .filter(it => it && it instanceof MeasurementItem)
      .map(it => it!.measurementId)

    console.log(
      'Wanted',
      measurements,
      'passing on',
      ids,
      'storing',
      this.selectedBenchmarkItems
    )

    this.$emit('input', ids)
  }
}
</script>

<style scoped>
.wrapper {
  max-height: 300px;
  overflow: auto;
}
.name {
  color: rgba(0, 0, 0, 0.87);
}

.cool-chip {
  border-width: thick !important;
  border-style: dotted !important;
}
</style>
