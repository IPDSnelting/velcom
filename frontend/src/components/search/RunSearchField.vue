<template>
  <v-autocomplete
    v-model="selectedRun"
    return-object
    :loading="isLoading"
    :search-input.sync="search"
    :items="items"
    :filter="filter"
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
          <span v-if="items.length === 0 && !search">
            Start typing for suggestions…
          </span>
          <span v-else>No run matches your query</span>
        </v-list-item-title>
      </v-list-item>
    </template>

    <template v-slot:selection="{ attr, on, item, selected }">
      <div
        v-bind="attr"
        :input-value="selected"
        v-on="on"
        class="d-flex align-center selection-container"
      >
        <v-icon color="primary" left>{{ item.icon }}</v-icon>
        <div class="selection-text">{{ item.text }}</div>
      </div>
    </template>

    <template v-slot:item="{ parent, item }">
      <v-list-item-avatar size="32">
        <v-icon>{{ item.icon }}</v-icon>
      </v-list-item-avatar>

      <v-list-item-content class="highlight-partial-match">
        <v-list-item-title
          v-html="parent.genFilteredText(item.text)"
        ></v-list-item-title>
        <v-list-item-subtitle
          v-if="item.subtext"
          v-html="parent.genFilteredText(item.subtext)"
        >
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
  readonly repoId?: string
  readonly commitHash?: string

  constructor(
    id: string,
    text: string,
    subtext: string | undefined,
    type: 'tar' | 'commit' | 'branch',
    repoId?: string,
    commitHash?: string
  ) {
    this.id = id
    this.text = text
    this.subtext = subtext
    this.type = type
    this.repoId = repoId
    this.commitHash = commitHash
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

export type RunSearchValue = { value: string; repoId?: string }

@Component
export default class RunSearchField extends Vue {
  private selectedRun: SearchItem | null = null
  private items: SearchItem[] = []
  private isLoading: boolean = false
  private search: string | null = null

  @Prop({ default: null })
  private readonly repoId!: RepoId | null

  @Watch('search')
  private async onSearchChange() {
    if (this.search && this.search !== '') {
      this.refreshFunction()
    }
  }

  @Watch('selectedRun')
  private onRunSelected() {
    if (!this.selectedRun) {
      this.$emit('input', null)
    } else {
      this.$emit('input', {
        value: this.selectedRun.commitHash || this.selectedRun.id,
        repoId: this.selectedRun.repoId
      } as RunSearchValue)
    }
  }

  private filter(item: SearchItem, search: string) {
    return (
      item.text.toLocaleLowerCase().includes(search.toLocaleLowerCase()) ||
      item.subtext?.toLocaleLowerCase().includes(search.toLocaleLowerCase())
    )
  }

  private get refreshFunction() {
    return debounce(() => {
      this.fetchSearchResults()
    }, 250)
  }

  private async fetchSearchResults() {
    this.isLoading = true

    const fetchedItems = await vxm.runSearchModule.searchRun({
      description: this.search || undefined,
      commitHash: this.search || undefined,
      orderBy: 'committer_date',
      repoId: this.repoId || undefined
    })
    const newItems = fetchedItems.map(
      it => new SearchItem(it.id, it.summary, it.commitHash, it.type)
    )

    this.items = this.branchItems
      .sort((a, b) => a.text.localeCompare(b.text))
      .concat(newItems.sort((a, b) => a.text.localeCompare(b.text)))

    this.isLoading = false
  }

  private get branchItems() {
    return vxm.repoModule.allRepos
      .filter(repo => (this.repoId ? this.repoId === repo.id : true))
      .flatMap(repo => {
        return repo.branches.map(it => {
          let subtext = it.lastCommit
          if (!it.tracked) {
            subtext += ' (Untracked)'
          }
          return new SearchItem(
            it.name,
            it.name,
            subtext,
            'branch',
            repo.id,
            it.lastCommit
          )
        })
      })
  }
}
</script>

<!--suppress CssUnresolvedCustomProperty -->
<style scoped>
.selection-text {
  height: 18px;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
  flex: 1 1 100%;
}
.selection-container {
  color: var(--v-primary-base);
  flex: 1 1 100%;
}
</style>

<style>
.theme--dark .highlight-partial-match .v-list-item__mask {
  color: var(--v-accent-base) !important;
  background-color: transparent !important;
}

.theme--light .highlight-partial-match .v-list-item__mask {
  background-color: var(--v-primary-base) !important;
  color: white !important;
}
</style>
