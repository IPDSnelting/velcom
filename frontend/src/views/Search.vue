<template>
  <v-container>
    <v-row>
      <v-col>
        <h1>Voogle</h1>
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

@Component({
  components: { SearchResultList }
})
export default class Search extends Vue {
  private items: SearchItem[] = []

  private async mounted() {
    this.items = await vxm.runSearchModule.searchRunNew({
      query: 'fix ci'
    })
  }
}
</script>
