<template>
  <v-autocomplete
    v-model="selectedRun"
    :loading="isLoading"
    :search-input.sync="search"
    :items="items"
    item-text="text"
    item-value="id"
    label="Search for a run, branch, commit, ..."
    hide-details
    hide-selected
    chips
    clearable
  >
    <template v-slot:no-data>
      <v-list-item>
        <v-list-item-title>
          Search for a commit message, hash, branch or run. Start typing for
          suggestions.
        </v-list-item-title>
      </v-list-item>
    </template>

    <template v-slot:selection="{ attr, on, item, selected }">
      <v-chip
        v-bind="attr"
        :input-value="selected"
        color="primary"
        class="white--text"
        v-on="on"
      >
        <v-icon left>{{ item.icon }}</v-icon>
        <span>{{ shorten(item.text) }}</span>
      </v-chip>
    </template>

    <template v-slot:item="{ parent, item }">
      <v-list-item-avatar size="32">
        <v-icon>{{ item.icon }}</v-icon>
      </v-list-item-avatar>

      <v-list-item-content>
        <v-list-item-title
          v-html="parent.genFilteredText(item.text)"
        ></v-list-item-title>
        <v-list-item-subtitle v-if="item.subtext">
          {{ item.subtext }}
        </v-list-item-subtitle>
      </v-list-item-content>
    </template>
  </v-autocomplete>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { RepoId } from '@/store/types'
import { Prop, Watch } from 'vue-property-decorator'
import { vxm } from '@/store'
import { debounce } from '@/util/Debouncer'
import { mdiFolderZipOutline, mdiSourceBranch, mdiSourceCommit } from '@mdi/js'

class SearchItem {
  readonly id: string
  readonly text: string
  readonly subtext?: string
  readonly type: 'tar' | 'commit' | 'branch'

  constructor(
    id: string,
    text: string,
    subtext?: string,
    type: 'tar' | 'commit' | 'branch'
  ) {
    this.id = id
    this.text = text
    this.subtext = subtext
    this.type = type
  }

  get icon() {
    if (this.type === 'tar') {
      return mdiFolderZipOutline
    }
    if (this.type === 'branch') {
      return mdiSourceBranch
    }
    return mdiSourceCommit
  }
}

@Component
export default class RunSearchField extends Vue {
  private selectedRun: SearchItem | null = null
  private items: SearchItem[] = []
  private isLoading: boolean = false
  private search: string | null = null

  @Prop()
  private readonly repoId!: RepoId

  @Watch('search')
  private async onSearchChange() {
    if (this.search && this.search !== '') {
      this.refreshFunction()
    }
  }

  private shorten(input: string) {
    if (input.length < 30) {
      return input
    }
    return input.substr(0, 30)
  }

  private get refreshFunction() {
    return debounce(() => {
      this.fetchSearchResults()
    }, 250)
  }

  private async fetchSearchResults() {
    this.isLoading = true

    const fetchedItems = await vxm.runSearchModule.searchRun({
      description: this.search,
      orderBy: 'committer_date',
      repoId: this.repoId
    })
    const newItems = fetchedItems.map(
      it => new SearchItem(it.id, it.summary, undefined, it.type)
    )

    this.items = this.branchItems.concat(newItems)
    this.isLoading = false
  }

  private get branchItems() {
    const repo = vxm.repoModule.repoById(this.repoId)
    if (!repo) {
      return []
    }
    return repo.branches
      .map(it => {
        let subtext = it.lastCommit
        if (!it.tracked) {
          subtext += ' (Untracked)'
        }
        return new SearchItem(it.name, it.name, subtext, 'branch')
      })
      .sort((a, b) => a.text.localeCompare(b.text))
  }
}
</script>
