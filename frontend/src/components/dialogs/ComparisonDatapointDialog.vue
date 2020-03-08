<template>
  <v-dialog width="600" :value="dialogOpen" @input="onClose">
    <v-card class="datapointDialog">
      <v-card-title></v-card-title>
      <v-card-text>
        <v-radio-group v-model="datapointAction">
          <v-radio label="use datapoint as reference" value="setReference"></v-radio>
          <v-radio label="remove reference line" value="removeReference"></v-radio>
        </v-radio-group>
      </v-card-text>
      <v-card-actions>
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
import { Commit } from '../../store/types'
import CommitBenchmarkActions from '../CommitBenchmarkActions.vue'

@Component({})
export default class DatapointDialog extends Vue {
  @Prop({ default: false })
  dialogOpen!: boolean

  @Prop({})
  selectedCommit!: Commit | null

  private datapointAction: 'setReference' | 'removeReference' = 'setReference'

  private onConfirm() {
    switch (this.datapointAction) {
      case 'setReference':
        this.setReference()
        break
      case 'removeReference':
        this.removeReference()
        break
    }
  }

  private setReference() {
    this.$emit('setReference')
  }
  private removeReference() {
    this.$emit('removeReference')
  }

  private onClose() {
    this.$emit('close')
  }
}
</script>
