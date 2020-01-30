<template>
  <v-container>
    <v-row no-gutters class="ma-3">
      <v-col>
        <v-card>
          <v-card-title>
            <v-toolbar dense dark color="primary">
              <v-toolbar-title>Comparison results</v-toolbar-title>
            </v-toolbar>
          </v-card-title>
          <v-card-text>
            <commit-info-table
              v-if="commitComparison"
              :comparison="commitComparison"
              :compare="true"
            ></commit-info-table>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
    <v-row no-gutters>
      <v-col cols="12">
        <commit-information
          v-if="commitComparison && commitComparison.firstCommit"
          :commit="commitComparison.firstCommit"
          :hasExistingBenchmark="firstHasBenchmark"
        ></commit-information>
      </v-col>
      <v-col>
        <commit-information
          v-if="commitComparison && commitComparison.secondCommit"
          :commit="commitComparison.secondCommit"
          :hasExistingBenchmark="secondHasBenchmark"
        ></commit-information>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { vxm } from '../store'
import { CommitInfo, CommitComparison } from '../store/types'
import { Watch } from 'vue-property-decorator'
import CommitInformation from '../components/CommitInformation.vue'
import CommitInfoTable from '../components/CommitInfoTable.vue'

@Component({
  components: {
    'commit-information': CommitInformation,
    'commit-info-table': CommitInfoTable
  }
})
export default class CommitComparisonDisplay extends Vue {
  private get repoID() {
    return this.$route.params.repoID
  }

  private get firstHash() {
    return this.$route.params.hashOne
  }

  private get secondHash() {
    return this.$route.params.hashTwo
  }

  private firstHasBenchmark(): boolean {
    return this.commitComparison ? !!this.commitComparison.first : false
  }

  private secondHasBenchmark(): boolean {
    return this.commitComparison ? !!this.commitComparison.second : false
  }

  private get commitComparison(): CommitComparison | null {
    return this.commitInfo ? this.commitInfo.comparison : null
  }

  private get commitInfo(): CommitInfo | null {
    return vxm.commitComparisonModule.commitInfo(
      this.repoID,
      this.firstHash,
      this.secondHash
    )
  }

  @Watch('repoID')
  @Watch('firstHash')
  @Watch('secondHash')
  private updateThySelf() {
    vxm.commitComparisonModule.fetchCommitInfo({
      repoId: this.repoID,
      first: this.firstHash,
      second: this.secondHash
    })
  }

  created() {
    this.updateThySelf()
  }
}
</script>
