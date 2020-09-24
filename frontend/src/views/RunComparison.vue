<template>
  <v-container>
    <v-row v-if="comparison">
      <v-col>
        <v-card>
          <v-card-title>
            <v-toolbar dark color="primary">Run Comparison</v-toolbar>
          </v-card-title>
          <v-card-text>
            <comparison-table
              :first="comparison.run1"
              :second="comparison.run2"
              :differences="comparison.differences"
            ></comparison-table>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
    <v-row v-if="comparison && comparison.run1">
      <v-col>
        <run-info :run="comparison.run1">
          <template #title>
            <span>First Run Information</span>
            <v-spacer></v-spacer>
            <v-chip
              :to="{
                name: 'run-detail',
                params: { first: comparison.run1.id }
              }"
              outlined
              label
            >
              Run id: {{ comparison.run1.id }}
            </v-chip>
          </template>
        </run-info>
      </v-col>
    </v-row>
    <v-row v-if="comparison && comparison.run2">
      <v-col>
        <run-info :run="comparison.run2">
          <template #title>
            <span>Second Run Information</span>
            <v-spacer></v-spacer>
            <v-chip
              :to="{
                name: 'run-detail',
                params: { first: comparison.run2.id }
              }"
              outlined
              label
            >
              Run id: {{ comparison.run2.id }}
            </v-chip>
          </template>
        </run-info>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { vxm } from '@/store'
import { Watch } from 'vue-property-decorator'
import { RunComparison } from '@/store/types'
import RunComparisonTable from '@/components/comparison/RunComparisonTable.vue'
import RunInfo from '@/components/rundetail/RunInfo.vue'

@Component({
  components: {
    'run-info': RunInfo,
    'comparison-table': RunComparisonTable
  }
})
export default class RunComparisonView extends Vue {
  private comparison: RunComparison | null = null

  private get first(): string {
    return this.$route.params['first']
  }

  private get second(): string {
    return this.$route.params['second']
  }

  private get hash1(): string | undefined {
    return this.$route.query['hash1'] as string | undefined
  }

  private get hash2(): string | undefined {
    return this.$route.query['hash2'] as string | undefined
  }

  @Watch('first')
  @Watch('second')
  @Watch('hash1')
  @Watch('hash2')
  private async fetchData() {
    this.comparison = null

    this.comparison = await vxm.commitDetailComparisonModule.fetchComparison({
      first: this.first,
      second: this.second,
      hash1: this.hash1,
      hash2: this.hash2
    })
  }

  mounted(): void {
    this.fetchData()
  }
}
</script>
