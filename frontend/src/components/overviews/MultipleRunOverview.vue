<template>
  <v-data-iterator
    class="full-width"
    :items="runs"
    :hide-default-footer="runs.length < defaultItemsPerPage"
    :items-per-page="defaultItemsPerPage"
    :footer-props="{ itemsPerPageOptions: itemsPerPageOptions }"
  >
    <template v-slot:default="props">
      <v-row>
        <v-col
          cols="12"
          class="my-1 py-0"
          v-for="( { run, commit }, index) in props.items"
          :key="index"
        >
          <run-overview :commit="commit" :run="run"></run-overview>
        </v-col>
      </v-row>
    </template>
  </v-data-iterator>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import { Run, Commit } from '../../store/types'
import RunOverview from './RunOverview.vue'

export class AttributedRun {
  readonly run: Run
  readonly commit: Commit

  constructor(run: Run, commit: Commit) {
    this.run = run
    this.commit = commit
  }
}

@Component({
  components: {
    'run-overview': RunOverview
  }
})
export default class MultipleRunOverview extends Vue {
  @Prop()
  private runs!: AttributedRun[]

  private itemsPerPageOptions: number[] = [10, 20, 50, 100, 200, -1]
  private defaultItemsPerPage: number = 20
}
</script>

<style scoped>
.full-width {
  width: 100%;
}
</style>
