<template>
  <run-info :run="run">
    <template #title>
      <span>{{ title }}</span>
      <v-spacer></v-spacer>
      <v-chip
        :to="{
          name: 'run-detail',
          params: { first: run.id }
        }"
        outlined
        label
      >
        Run id: {{ run.id }}
      </v-chip>
    </template>
    <template #before-body>
      <v-row class="mx-1" v-if="commit">
        <v-col>
          <commit-overview-base
            :commit="commit"
            :outlined="true"
          ></commit-overview-base>
        </v-col>
      </v-row>
      <v-row class="mx-1" v-if="tar" justify="center">
        <v-col cols="auto">
          <tar-overview :tar-source="tar"></tar-overview>
        </v-col>
      </v-row>
    </template>
  </run-info>
</template>
<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import CommitOverviewBase from '@/components/overviews/CommitOverviewBase.vue'
import RunInfo from '@/components/rundetail/RunInfo.vue'
import TarOverview from '@/components/overviews/TarOverview.vue'
import { CommitTaskSource, Run, TarTaskSource } from '@/store/types'
import { Prop } from 'vue-property-decorator'

@Component({
  components: {
    'commit-overview-base': CommitOverviewBase,
    'tar-overview': TarOverview,
    'run-info': RunInfo
  }
})
export default class ComparisonRunInfo extends Vue {
  @Prop()
  private readonly run!: Run

  @Prop()
  private readonly title!: string

  private get commit() {
    if (this.run.source instanceof CommitTaskSource) {
      return this.run.source.commitDescription
    }
    return null
  }

  private get tar() {
    if (this.run.source instanceof TarTaskSource) {
      return this.run.source
    }
    return null
  }
}
</script>
