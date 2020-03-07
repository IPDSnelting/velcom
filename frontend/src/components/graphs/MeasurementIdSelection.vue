<template>
  <v-row no-gutters>
    <v-col>
      <v-select
        class="mr-5"
        :items="occuringBenchmarks"
        :value="selectedBenchmark"
        @input="selectBenchmark"
        label="benchmark"
      ></v-select>
    </v-col>
    <v-col>
      <v-select
        class="mr-5"
        :items="metricsForBenchmark(this.selectedBenchmark)"
        :value="selectedMetric"
        @input="selectMetric"
        label="metric"
      ></v-select>
    </v-col>
  </v-row>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { vxm } from '../../store'
import { Prop, Watch } from 'vue-property-decorator'

@Component
export default class MeasurementIdSelection extends Vue {
  @Prop()
  private repoId!: string

  @Prop()
  private selectedMetric!: string

  @Prop()
  private selectedBenchmark!: string

  get occuringBenchmarks(): string[] {
    return vxm.repoModule.occuringBenchmarks([this.repoId])
  }

  get metricsForBenchmark(): (benchmark: string) => string[] {
    return (benchmark: string) => vxm.repoModule.metricsForBenchmark(benchmark)
  }

  @Watch('selectedBenchmark')
  onBenchmarkChange() {
    let newMetrics = this.metricsForBenchmark(this.selectedBenchmark)
    if (!newMetrics.includes(this.selectedMetric)) {
      if (newMetrics) {
        this.selectMetric(newMetrics[0])
      }
    }
  }

  private selectBenchmark(benchmark: string) {
    this.$emit('changeBenchmark', benchmark)
  }

  private selectMetric(metric: string) {
    this.$emit('changeMetric', metric)
  }
}
</script>

<style scoped>
</style>
