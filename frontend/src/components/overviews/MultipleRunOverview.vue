<template>
  <div>
    <div v-for="item in runs" :key="run(item).runId" class="my-2">
      <run-overview :run="run(item)" class="full-width">
        <template #content v-if="differences(item)">
          <run-significance-chips
            :differences="differences(item)"
            :run-id="run(item).runId"
          ></run-significance-chips>
        </template>
      </run-overview>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import {
  DimensionDifference,
  RunDescription,
  RunDescriptionWithDifferences
} from '@/store/types'
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

  private run(
    run: RunDescription | RunDescriptionWithDifferences
  ): RunDescription {
    return run instanceof RunDescriptionWithDifferences ? run.run : run
  }

  private differences(
    run: RunDescription | RunDescriptionWithDifferences
  ): DimensionDifference[] | undefined {
    return run instanceof RunDescriptionWithDifferences
      ? run.differences
      : undefined
  }
}
</script>

<style scoped>
.full-width {
  width: 100%;
}
</style>
