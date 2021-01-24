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
import { RepoId, ShortRunDescription } from '@/store/types'
import { Prop, Watch } from 'vue-property-decorator'
import { vxm } from '@/store'
import { debounce } from '@/util/Debouncer'

@Component
export default class RunSearchField extends Vue {
  private selectedRun: ShortRunDescription | null = null
  private items: ShortRunDescription[] = []
  private isLoading: boolean = false
  private search: string = ''

  @Prop()
  private readonly repoId!: RepoId

  @Watch('search')
  private async onSearchChange() {
    this.refreshFunction()
  }

  private get refreshFunction() {
    return debounce(() => {
      this.fetchSearchResults()
    }, 250)
  }

  private async fetchSearchResults() {
    this.isLoading = true

    this.items = await vxm.runSearchModule.searchRun({
      description: this.search,
      orderBy: 'committer_date',
      repoId: this.repoId
    })
    this.isLoading = false
  }
}
</script>
