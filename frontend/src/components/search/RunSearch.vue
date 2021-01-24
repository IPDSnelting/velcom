<template>
  <v-container fluid class="ma-0 pa-0">
    <v-row align="center">
      <v-col cols="auto">Compare</v-col>
      <v-col cols="4">
        <run-search-field :repo-id="repoId"></run-search-field>
      </v-col>
      <v-col cols="auto">to</v-col>
      <v-col cols="4">
        <run-search-field :repo-id="repoId"></run-search-field>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import RunSearchField from '@/components/search/RunSearchField.vue'
import { vxm } from '@/store'

@Component({
  components: { RunSearchField }
})
export default class RunSearch extends Vue {
  private get repoId() {
    return vxm.detailGraphModule.selectedRepoId
  }
}
</script>
