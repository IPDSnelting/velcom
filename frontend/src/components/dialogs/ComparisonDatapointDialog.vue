<template>
  <v-dialog width="600" :value="dialogOpen" @input="onClose">
    <v-card class="datapointDialog">
      <v-card-title></v-card-title>
      <v-card-text>
        <v-row dense>
          <v-col>
            <v-btn
              @click="emit('setReference')"
              width="100%"
              text
              outlined
              color="primary"
            >use datapoint as reference</v-btn>
          </v-col>
        </v-row>
        <v-row dense class="my-2">
          <v-col>
            <v-btn
              @click="emit('viewInDetailGraph')"
              width="100%"
              text
              outlined
              color="primary"
            >view in detail graph</v-btn>
          </v-col>
        </v-row>
        <v-row dense>
          <v-col>
            <v-btn
              @click="emit('removeReference')"
              width="100%"
              text
              outlined
              color="primary"
            >remove reference line</v-btn>
          </v-col>
        </v-row>
      </v-card-text>
      <v-card-actions>
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
import { Commit } from '../../store/types'
import CommitBenchmarkActions from '../CommitBenchmarkActions.vue'

@Component({})
export default class DatapointDialog extends Vue {
  @Prop({ default: false })
  dialogOpen!: boolean

  @Prop({})
  selectedCommit!: Commit | null

  private emit(event: string) {
    this.$emit(event)
  }

  private onClose() {
    this.$emit('close')
  }
}
</script>
