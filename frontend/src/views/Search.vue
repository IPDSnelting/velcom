<template>
  <v-container>
    <v-row justify="center">
      <v-col>
        <h1>Voogle</h1>
      </v-col>
    </v-row>
    <v-row>
      <v-col class="mx-5">
        <v-text-field
          v-model="searchQuery"
          outlined
          label="Search query…"
          placeholder="Enter a commit message, commit hash, run id or tar description"
        ></v-text-field>
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="12">
        <search-result-list :items="items"></search-result-list>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import SearchResultList from '@/components/search/SearchResultList.vue'
import { vxm } from '@/store'
import { SearchItem } from '@/store/types'
import { Watch } from 'vue-property-decorator'
import { debounce } from '@/util/Debouncer'

@Component({
  components: { SearchResultList }
})
export default class Search extends Vue {
  private searchQuery = ''
  private items: SearchItem[] = []

  private debouncedLookup = debounce(this.executeSearch, 300)

  @Watch('searchQuery')
  private onQueryChange() {
    this.debouncedLookup()
  }

  private async executeSearch() {
    if (this.searchQuery.length === 0) {
      this.items = []
      return
    }
    this.items = await vxm.runSearchModule.searchRunNew({
      query: this.searchQuery
    })
  }
}
</script>
