<template>
  <v-card outlined>
    <v-card-title> Tar '{{ tarSource.description }}' </v-card-title>
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
import { TarTaskSource } from '@/store/types'
import InlineMinimalRepoNameDisplay from '@/components/InlineMinimalRepoDisplay.vue'

@Component({
  components: {
    'inline-minimal-repo-display': InlineMinimalRepoNameDisplay
  }
})
export default class TarOverview extends Vue {
  @Prop()
  private readonly tarSource!: TarTaskSource
}
</script>
