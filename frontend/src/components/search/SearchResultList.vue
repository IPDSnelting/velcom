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
          <component :is="item.type" :item="item.item">
            <template #compare-actions="{ hasRun }">
              <v-tooltip left>
                <template #activator="{ on }">
                  <div v-on="on">
                    <v-btn
                      icon
                      small
                      :disabled="!hasRun"
                      @click="$emit('mark-compare', item.item)"
                    >
                      <v-icon>{{ compareIcon }}</v-icon>
                    </v-btn>
                  </div>
                </template>
                <span v-if="hasRun">Mark this run for comparison</span>
                <span v-else>
                  Can't compare commits that were never benchmarked
                </span>
              </v-tooltip>
            </template>
          </component>
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
import { SearchItem, SearchItemCommit, SearchItemRun } from '@/store/types'
import SearchResultCommit from '@/components/search/SearchResultCommit.vue'
import SearchResultRun from '@/components/search/SearchResultRun.vue'
import { mdiScaleBalance } from '@mdi/js'
import SearchResultBranch from '@/components/search/SearchResultBranch.vue'

class DisplayedItem {
  readonly id: string
  readonly type: string
  readonly item: SearchItem

  constructor(item: SearchItem) {
    this.item = item

    if (item instanceof SearchItemCommit) {
      this.type = 'commit'
      this.id = item.repoId + item.hash
    } else if (item instanceof SearchItemRun) {
      this.type = item.tarDescription ? 'tar' : 'run'
      this.id = item.id
    } else {
      this.type = 'branch'
      this.id = item.repoId + item.hash
    }
  }
}

@Component({
  components: {
    commit: SearchResultCommit,
    branch: SearchResultBranch,
    run: SearchResultRun,
    tar: SearchResultRun
  }
})
export default class SearchResultList extends Vue {
  @Prop({ default: () => [] })
  private readonly items!: SearchItem[]

  private get displayedItems(): DisplayedItem[] {
    return this.items.map(it => new DisplayedItem(it))
  }

  private readonly compareIcon = mdiScaleBalance
}
</script>
