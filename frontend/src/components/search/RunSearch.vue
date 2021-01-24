<template>
  <v-container fluid>
    <v-row align="center" justify="center">
      <v-col cols="auto">Compare</v-col>
      <v-col cols="12" sm="4">
        <run-search-field
          @input="firstRun = $event"
          :repo-id="repoId"
        ></run-search-field>
      </v-col>
      <v-col cols="auto">to</v-col>
      <v-col cols="12" sm="4">
        <run-search-field
          @input="secondRun = $event"
          :repo-id="repoId"
        ></run-search-field>
      </v-col>
    </v-row>
    <v-row align="center" justify="center" class="mt-6">
      <v-col cols="auto">
        <v-btn
          :disabled="!firstRun || !secondRun"
          color="primary"
          @click="compare"
        >
          Compare…
        </v-btn>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import RunSearchField, {
  RunSearchValue
} from '@/components/search/RunSearchField.vue'
import { vxm } from '@/store'
import { Dictionary } from 'vue-router/types/router'

@Component({
  components: { RunSearchField }
})
export default class RunSearch extends Vue {
  private firstRun: RunSearchValue | null = null
  private secondRun: RunSearchValue | null = null

  private get repoId() {
    return vxm.detailGraphModule.selectedRepoId
  }

  private compare() {
    if (!this.firstRun || !this.secondRun) {
      return
    }
    const query: Dictionary<string> = {}

    if (!this.firstRun.isRun) {
      query.hash1 = this.firstRun.value
    }
    if (!this.secondRun.isRun) {
      query.hash2 = this.secondRun.value
    }

    this.$router.push({
      name: 'run-comparison',
      params: {
        first: this.firstRun.isRun ? this.firstRun.value : this.repoId,
        second: this.secondRun.isRun ? this.secondRun.value : this.repoId
      },
      query: query
    })
  }
}
</script>
