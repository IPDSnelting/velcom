<template>
  <v-dialog width="600" :value="dialogOpen" @input="onClose">
    <v-card class="datapointDialog">
      <v-card-title></v-card-title>
      <v-card-text>
        <v-radio-group v-model="datapointAction">
          <v-radio
            v-if="allowSelectAsReference"
            label="use datapoint as reference"
            value="setReference"
          ></v-radio>
          <v-radio
            v-if="allowSelectAsReference"
            label="Select this commit to compare"
            value="selectCommitToCompare"
          ></v-radio>
          <v-radio
            v-if="allowSelectAsReference && commitToCompare"
            :label="compareLabel"
            value="compareCommits"
          ></v-radio>
          <v-radio label="remove reference line" value="removeReference"></v-radio>
        </v-radio-group>
      </v-card-text>
      <v-card-actions>
        <commit-benchmark-actions
          v-if="selectedDatapoint"
          :hasExistingBenchmark="selectedDatapoint.comparison && selectedDatapoint.comparison.second"
          :commit="selectedCommit"
        ></commit-benchmark-actions>
        <v-spacer></v-spacer>
        <v-btn color="error" @click="onClose">Close</v-btn>
        <v-btn color="primary" @click="onConfirm">Confirm</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop, Watch } from 'vue-property-decorator'
import { Commit, CommitComparison } from '../../store/types'
import CommitBenchmarkActions from '../CommitBenchmarkActions.vue'

type CommitInfo = { commit: Commit; comparison: CommitComparison }

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
  commitToCompare!: CommitInfo

  @Prop({})
  allowSelectAsReference!: boolean

  get selectedCommit(): Commit {
    return this.selectedDatapoint!.commit
  }

  private datapointAction:
    | 'setReference'
    | 'selectCommitToCompare'
    | 'compareCommits'
    | 'removeReference' = 'setReference'

  private get compareLabel(): string {
    return this.commitToCompare
      ? 'Compare this commit to commit ' + this.commitToCompare!.commit.hash
      : ''
  }

  private onConfirm() {
    switch (this.datapointAction) {
      case 'setReference':
        this.setReference()
        break
      case 'selectCommitToCompare':
        this.selectCommitToCompare()
        break
      case 'compareCommits':
        this.compareCommits()
        break
      case 'removeReference':
        this.removeReference()
        break
    }
  }

  private setReference() {
    this.$emit('setReference')
  }
  private selectCommitToCompare() {
    this.$emit('selectCommitToCompare')
  }
  private compareCommits() {
    this.$emit('compareCommits')
  }
  private removeReference() {
    this.$emit('removeReference')
  }

  private onClose() {
    this.$emit('close')
  }
}
</script>
