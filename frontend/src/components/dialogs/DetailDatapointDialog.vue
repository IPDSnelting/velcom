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
              @click="setAsReference"
              >Use datapoint as reference</v-btn
            >
          </v-col>
        </v-row>
        <v-row dense>
          <v-col>
            <v-btn
              v-if="hasReferenceLine"
              width="100%"
              color="primary"
              text
              outlined
              @click="removeReferenceLine"
              >remove reference line</v-btn
            >
          </v-col>
        </v-row>
        <v-row v-if="allowSelectCompare" class="mt-4" dense>
          <v-col>
            <v-btn
              width="100%"
              color="primary"
              text
              outlined
              @click="setAsCompare"
              >Select this commit to compare</v-btn
            >
          </v-col>
        </v-row>
        <v-row v-if="allowSelectCompare && commitToCompare" dense>
          <v-col>
            <v-btn
              width="100%"
              color="primary"
              text
              outlined
              class="reflow-button py-2"
              @click="compareCommits"
              >{{ compareLabel }}</v-btn
            >
          </v-col>
        </v-row>
        <v-row v-if="commitToCompare" dense>
          <v-col class="mb-2">
            <v-btn
              width="100%"
              color="primary"
              text
              outlined
              class="reflow-button py-2"
              @click="removeCompare"
              >Reset comparison</v-btn
            >
          </v-col>
        </v-row>
      </v-card-text>
      <v-card-actions>
        <benchmark-actions
          :has-existing-benchmark="commitHasRun"
          :commit-description="selectedDatapointAsCommitDescription"
        ></benchmark-actions>
        <v-spacer></v-spacer>
        <v-btn color="error" @click="onClose">Close</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import { CommitDescription, DetailDataPoint, Dimension } from '@/store/types'
import { vxm } from '@/store'
import { DimensionDetailPoint } from '@/store/modules/detailGraphStore'
import CommitBenchmarkActions from '@/components/CommitBenchmarkActions.vue'

@Component({
  components: {
    'benchmark-actions': CommitBenchmarkActions
  }
})
export default class DetailDatapointDialog extends Vue {
  @Prop({ default: false })
  dialogOpen!: boolean

  @Prop()
  selectedDatapoint!: DetailDataPoint

  @Prop()
  dimension!: Dimension

  private get commitHasValue() {
    return this.selectedDatapoint.successful(this.dimension)
  }

  private get commitHasRun() {
    return !this.selectedDatapoint.unbenchmarked(this.dimension)
  }

  private get allowSelectAsReference(): boolean {
    return this.commitHasValue
  }

  private get allowSelectCompare(): boolean {
    return this.commitHasValue
  }

  private get commitToCompare(): DimensionDetailPoint | null {
    return vxm.detailGraphModule.commitToCompare
  }

  private get hasReferenceLine() {
    return vxm.detailGraphModule.referenceDatapoint !== null
  }

  private get selectedDatapointAsCommitDescription() {
    return new CommitDescription(
      vxm.detailGraphModule.selectedRepoId,
      this.selectedDatapoint.hash,
      this.selectedDatapoint.author,
      this.selectedDatapoint.authorDate,
      this.selectedDatapoint.summary
    )
  }

  private get compareLabel(): string {
    return this.commitToCompare
      ? 'Compare this commit to commit ' + this.commitToCompare.dataPoint.hash
      : ''
  }

  private setAsReference() {
    vxm.detailGraphModule.referenceDatapoint = {
      dataPoint: this.selectedDatapoint,
      dimension: this.dimension
    }
    this.$emit('close')
  }

  private removeReferenceLine() {
    vxm.detailGraphModule.referenceDatapoint = null
    this.$emit('close')
  }

  private setAsCompare() {
    vxm.detailGraphModule.commitToCompare = {
      dimension: this.dimension,
      dataPoint: this.selectedDatapoint
    }
    this.$emit('close')
  }

  private removeCompare() {
    vxm.detailGraphModule.commitToCompare = null
    this.$emit('close')
  }

  private compareCommits() {
    this.$emit('compare-commits')
    this.$emit('close')
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
