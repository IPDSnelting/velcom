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
          return-object
          selectable
          :items="measurementItems"
        ></v-treeview>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { vxm } from '../../store'
import { Prop, Watch } from 'vue-property-decorator'
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

  private get measurementItems(): BenchmarkItem[] {
    return vxm.repoModule.occuringBenchmarks([this.repoId]).map(benchmark => {
      let metrics = vxm.repoModule
        .metricsForBenchmark(benchmark)
        .map(
          metric => new MeasurementItem(new MeasurementID(benchmark, metric))
        )
      return new BenchmarkItem(benchmark, metrics)
    })
  }

  private changed(measurements: MeasurementItem[]) {
    this.$emit(
      'input',
      measurements.map(it => it.measurementId)
    )
  }
}
</script>

<style scoped>
.wrapper {
  max-height: 300px;
  overflow: auto;
}
</style>
