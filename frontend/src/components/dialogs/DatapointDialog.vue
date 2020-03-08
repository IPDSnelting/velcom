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
            v-if="allowSelectAsReference && !isCommitToCompare(this.selectedDatapoint)"
            label="Select this commit to compare"
            value="selectCommitToCompare"
          ></v-radio>
          <v-radio
            v-if="allowSelectAsReference && commitToCompare && !isCommitToCompare(this.selectedDatapoint)"
            :label="compareLabel"
            value="compareCommits"
          ></v-radio>
          <v-radio label="remove reference line" value="removeReference"></v-radio>
          <v-radio
            v-for="{ name, event } in this.extraOptions"
            :key="event"
            :label="name"
            :value="event"
          ></v-radio>
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

/**
 * An extra option in the dialog. It will trigger an "extraOption" event
 * with the "eventName" as value.
 */
export type ExtraOption = { text: string; eventName: string }

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

  @Prop({ default: () => [] })
  extraOptions!: ExtraOption[]

  private get selectedCommit(): Commit {
    return this.selectedDatapoint!.commit
  }

  private isCommitToCompare(datapoint: CommitInfo): boolean {
    if (this.commitToCompare) {
      return datapoint.commit.hash === this.commitToCompare.commit.hash
    }
    return false
  }

  private datapointAction: string = 'setReference'

  private get compareLabel(): string {
    return this.commitToCompare
      ? 'Compare this commit to commit ' + this.commitToCompare!.commit.hash
      : ''
  }

  private onConfirm() {
    let matchingExtraOptions = this.extraOptions.filter(
      it => it.eventName === this.datapointAction
    )
    if (matchingExtraOptions.length > 0) {
      this.$emit('extraOption', this.datapointAction)
    } else {
      this.$emit(this.datapointAction)
    }
  }

  private onClose() {
    this.$emit('close')
  }
}
</script>
