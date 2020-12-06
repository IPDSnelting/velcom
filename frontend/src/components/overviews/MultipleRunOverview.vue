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
          v-for="(item, index) in props.items"
          :key="index"
        >
          <run-overview :run="run(item)">
            <template #content v-if="showSignificantChips(item)">
              <run-significance-chips :run="item"></run-significance-chips>
            </template>
          </run-overview>
        </v-col>
      </v-row>
    </template>
  </v-data-iterator>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import { RunDescription, RunDescriptionWithDifferences } from '@/store/types'
import RunOverview from './RunOverview.vue'
import RunSignificanceChips from '@/components/RunSignificanceChips.vue'

@Component({
  components: {
    'run-significance-chips': RunSignificanceChips,
    'run-overview': RunOverview
  }
})
export default class MultipleRunOverview extends Vue {
  @Prop()
  private runs!: RunDescription[] | RunDescriptionWithDifferences[]

  @Prop({ default: 3 })
  private numberOfChanges!: number

  // noinspection JSMismatchedCollectionQueryUpdate
  private itemsPerPageOptions: number[] = [10, 20, 50, 100, 200, -1]
  private defaultItemsPerPage: number = 20

  private run(
    run: RunDescription | RunDescriptionWithDifferences
  ): RunDescription {
    return run instanceof RunDescriptionWithDifferences ? run.run : run
  }

  private showSignificantChips(
    run: RunDescription | RunDescriptionWithDifferences
  ) {
    return run instanceof RunDescriptionWithDifferences
  }
}
</script>

<style scoped>
.full-width {
  width: 100%;
}
</style>
