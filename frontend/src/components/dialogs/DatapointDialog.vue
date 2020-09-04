<template>
  <v-dialog width="600" :value="dialogOpen" @input="onClose">
    <v-card class="datapointDialog">
      <v-card-title></v-card-title>
      <v-card-text>
        <v-row v-if="allowSelectAsReference" dense>
          <v-col>
            <v-btn
              width="100%"
              color="primary"
              text
              outlined
              @click="emit('setReference'); emit('close')"
            >use datapoint as reference</v-btn>
          </v-col>
        </v-row>
        <v-row v-if="allowSelectAsReference && !isCommitToCompare(this.selectedDatapoint)">
          <v-col>
            <v-btn
              width="100%"
              color="primary"
              text
              outlined
              @click="emit('selectCommitToCompare'); emit('close')"
            >Select this commit to compare</v-btn>
          </v-col>
        </v-row>
        <v-row
          v-if="allowSelectAsReference && commitToCompare && !isCommitToCompare(this.selectedDatapoint)"
          dense
        >
          <v-col class="mb-2">
            <v-btn
              width="100%"
              color="primary"
              text
              outlined
              class="reflow-button py-2"
              @click="emit('compareCommits'); emit('close')"
            >{{ compareLabel }}</v-btn>
          </v-col>
        </v-row>
        <v-row dense>
          <v-col>
            <v-btn
              width="100%"
              color="primary"
              text
              outlined
              @click="emit('removeReference'); emit('close')"
            >remove reference line</v-btn>
          </v-col>
        </v-row>
      </v-card-text>
      <v-card-actions>
        <commit-benchmark-actions
          v-if="selectedDatapoint"
          :hasExistingBenchmark="selectedDatapoint.comparison && selectedDatapoint.comparison.second"
          :commit="selectedCommit"
        ></commit-benchmark-actions>
        <v-spacer></v-spacer>
        <v-btn color="error" @click="onClose">Close</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop, Watch } from 'vue-property-decorator'
import { Commit, RunComparison } from '../../store/types'
import CommitBenchmarkActions from '../CommitBenchmarkActions.vue'

type CommitInfo = { commit: Commit; comparison: RunComparison }

@Component({
  components: {
    'commit-benchmark-actions': CommitBenchmarkActions
  }
})
export default class DatapointDialog extends Vue {
  @Prop({ default: false })
  dialogOpen!: boolean

  @Prop({})
  selectedDatapoint!: CommitInfo | null

  @Prop({})
  commitToCompare!: CommitInfo | null

  @Prop({})
  allowSelectAsReference!: boolean

  private get selectedCommit(): Commit {
    return this.selectedDatapoint!.commit
  }

  private isCommitToCompare(datapoint: CommitInfo): boolean {
    if (this.commitToCompare) {
      return datapoint.commit.hash === this.commitToCompare.commit.hash
    }
    return false
  }

  private get compareLabel(): string {
    return this.commitToCompare
      ? 'Compare this commit to commit ' + this.commitToCompare!.commit.hash
      : ''
  }

  private emit(event: string) {
    this.$emit(event)
  }

  private onClose() {
    this.$emit('close')
  }
}
</script>

<style scoped>
.reflow-button {
  word-wrap: normal;
  white-space: normal;
  height: 100% !important;
}
</style>

<style>
.reflow-button > span {
  width: 100%;
}
</style>
