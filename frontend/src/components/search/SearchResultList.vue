<template>
  <v-data-iterator :items="displayedItems" item-key="id">
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
  </v-data-iterator>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import { CommitDescription, SearchItem } from '@/store/types'
import SearchResultCommit from '@/components/search/SearchResultCommit.vue'
import SearchResultTar from '@/components/search/SearchResultTar.vue'
import SearchResultRun from '@/components/search/SearchResultRun.vue'

class DisplayedItem {
  readonly id: string
  readonly type: string
  readonly item: SearchItem

  constructor(item: SearchItem) {
    this.id =
      item instanceof CommitDescription ? item.repoId + item.hash : item.id
    this.item = item

    if (item instanceof CommitDescription) {
      this.type = 'commit'
    } else {
      this.type = item.tarSummary ? 'tar' : 'run'
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
