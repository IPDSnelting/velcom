<template>
  <v-autocomplete
    v-model="selectedRun"
    :loading="isLoading"
    :search-input.sync="search"
    :items="items"
    item-text="summary"
    item-value="id"
    label="Search for a run, branch, commit, ..."
    hide-details
    hide-selected
    chips
    clearable
  ></v-autocomplete>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { ShortRunDescription } from '@/store/types'
import { Watch } from 'vue-property-decorator'
import { vxm } from '@/store'

@Component
export default class RunSearchField extends Vue {
  private selectedRun: ShortRunDescription | null = null
  private items: ShortRunDescription[] = []
  private isLoading: boolean = false
  private search: string = ''

  @Watch('search')
  private async onSearchChange() {
    this.isLoading = true

    this.items = await vxm.runSearchModule.searchRun({
      description: this.search
    })
    this.isLoading = false
  }
}
</script>
