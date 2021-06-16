<template>
  <v-container>
    <v-row v-if="(!run1Successful || !run2Successful) && comparison">
      <v-col>
        <v-card>
          <v-card-title>
            <v-toolbar dark color="toolbarColor">Run Comparison</v-toolbar>
          </v-card-title>
          <v-card-text class="mx-2">
            <div class="text-body-1 mb-3">
              Comparison not possible
              <span v-if="run2Successful">as the first run failed.</span>
              <span v-else-if="run1Successful">as the second run failed.</span>
              <span v-else>as both runs failed.</span>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
    <v-row v-if="comparison && run1Successful && run2Successful">
      <v-col>
        <v-card>
          <v-card-title>
            <v-toolbar dark color="toolbarColor">Run Comparison</v-toolbar>
          </v-card-title>
          <v-card-text style="max-width: 1185px; margin: auto">
            <run-significance-chips
              class="mb-6"
              :center="true"
              :no-links="true"
              :differences="comparison.significantDifferences"
              :run-id="comparison.run2.id"
              :failed-significant-dimensions="
                comparison.significantFailedDimensions
              "
            ></run-significance-chips>
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
        <comparison-run-info
          :run="comparison.run1"
          :title="'First Run Information' + (run1Successful ? '' : ' (Failed)')"
        />
      </v-col>
    </v-row>
    <v-row v-if="comparison && comparison.run2" class="mt-3 mb-3">
      <v-col>
        <comparison-run-info
          :run="comparison.run2"
          :title="
            'Second Run Information' + (run2Successful ? '' : ' (Failed)')
          "
        />
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { vxm } from '@/store'
import { Watch } from 'vue-property-decorator'
import { RunComparison, RunResultSuccess } from '@/store/types'
import RunComparisonTable from '@/components/comparison/RunComparisonTable.vue'
import CommitOverviewBase from '@/components/overviews/CommitOverviewBase.vue'
import RunInfo from '@/components/rundetail/RunInfo.vue'
import TarOverview from '@/components/overviews/TarOverview.vue'
import ComparisonRunInfo from '@/components/comparison/ComparisonRunInfo.vue'
import RunSignificanceChips from '@/components/runs/RunSignificanceChips.vue'

@Component({
  components: {
    RunSignificanceChips,
    ComparisonRunInfo,
    'tar-overview': TarOverview,
    'commit-overview-base': CommitOverviewBase,
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

  private get run1Successful() {
    return (
      this.comparison && this.comparison.run1.result instanceof RunResultSuccess
    )
  }

  private get run2Successful() {
    return (
      this.comparison && this.comparison.run2.result instanceof RunResultSuccess
    )
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
