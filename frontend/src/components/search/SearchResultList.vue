<template>
  <v-data-iterator
    :items="displayedItems"
    item-key="id"
    :items-per-page="50"
    :footer-props="{ itemsPerPageOptions: [10, 20, 50, 100, -1] }"
  >
    <template v-slot:default="props">
      <v-row>
        <v-col
          v-for="item in props.items"
          :key="item.name"
          cols="12"
          class="py-1"
        >
          <component :is="item.type" :item="item.item"></component>
        </v-col>
      </v-row>
    </template>

    <template #no-data>
      <v-row justify="center">
        <v-col cols="auto">
          <span>Nothing found or no search term entered</span>
        </v-col>
      </v-row>
    </template>
  </v-data-iterator>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import { SearchItem, SearchItemCommit } from '@/store/types'
import SearchResultCommit from '@/components/search/SearchResultCommit.vue'
import SearchResultTar from '@/components/search/SearchResultTar.vue'
import SearchResultRun from '@/components/search/SearchResultRun.vue'

class DisplayedItem {
  readonly id: string
  readonly type: string
  readonly item: SearchItem

  constructor(item: SearchItem) {
    this.id =
      item instanceof SearchItemCommit ? item.repoId + item.hash : item.id
    this.item = item

    if (item instanceof SearchItemCommit) {
      this.type = 'commit'
    } else {
      this.type = item.tarDescription ? 'tar' : 'run'
    }
  }
}

@Component({
  components: {
    commit: SearchResultCommit,
    tar: SearchResultTar,
    run: SearchResultRun
  }
})
export default class SearchResultList extends Vue {
  @Prop({ default: () => [] })
  private readonly items!: SearchItem[]

  private get displayedItems(): DisplayedItem[] {
    return this.items.map(it => new DisplayedItem(it))
  }
}
</script>
