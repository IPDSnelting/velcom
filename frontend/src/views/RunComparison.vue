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
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { vxm } from '@/store'
import { Watch } from 'vue-property-decorator'
import { RunComparison } from '@/store/types'
import RunComparisonTable from '@/components/comparison/RunComparisonTable.vue'

@Component({
  components: {
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
