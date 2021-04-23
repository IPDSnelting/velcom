<template>
  <v-card outlined>
    <v-card-title>
      <span v-if="runId === null">Tar '{{ tarSource.description }}'</span>
      <v-row v-if="runId !== null" justify="space-between">
        <v-col> Tar '{{ tarSource.description }}' </v-col>
        <v-col cols="auto">
          <v-tooltip top>
            <template #activator="{ on }">
              <v-btn icon :to="compareRunLocation" v-on="on">
                <v-icon>{{ compareIcon }}</v-icon>
              </v-btn>
            </template>
            Compare this run with another
          </v-tooltip>
        </v-col>
      </v-row>
    </v-card-title>
    <v-card-subtitle v-if="tarSource.repoId">
      Attached to
      <inline-minimal-repo-display
        :repo-id="tarSource.repoId"
      ></inline-minimal-repo-display>
    </v-card-subtitle>
  </v-card>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import { RunId, TarTaskSource } from '@/store/types'
import InlineMinimalRepoNameDisplay from '@/components/InlineMinimalRepoDisplay.vue'
import { mdiScaleBalance } from '@mdi/js'

@Component({
  components: {
    'inline-minimal-repo-display': InlineMinimalRepoNameDisplay
  }
})
export default class TarOverview extends Vue {
  @Prop()
  private readonly tarSource!: TarTaskSource

  @Prop({ default: null })
  private readonly runId!: RunId | null

  private get compareRunLocation() {
    if (!this.runId) {
      return null
    }
    return {
      name: 'search',
      params: {
        runId: this.runId
      }
    }
  }

  // ICONS
  private compareIcon = mdiScaleBalance
}
</script>
