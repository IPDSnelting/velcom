<template>
  <v-container>
    <v-row justify="space-between" class="px-5">
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
          hide-details
        ></v-text-field>
      </v-col>
      <v-col cols="auto">
        <v-row no-gutters align="center">
          <v-col cols="auto" class="mr-4">
            <v-checkbox
              v-model="constrainToRepo"
              label="Limit searches to"
            ></v-checkbox>
          </v-col>
          <v-col cols="auto">
            <repo-selection-component
              :disabled="!constrainToRepo"
              v-model="repoId"
              :repos="allRepos"
            ></repo-selection-component>
          </v-col>
        </v-row>
      </v-col>
    </v-row>
    <v-row justify="center" v-if="compareFirst || compareSecond" class="my-3">
      <v-col class="mx-5" sm="12" md="8">
        <v-card flat>
          <v-card-title class="d-flex justify-center pt-0">
            <div>Compare two runs</div>
          </v-card-title>
          <v-card-text class="pb-0">
            <v-row no-gutters justify="center">
              <v-col cols="5" class="d-flex" style="justify-content: end">
                <v-chip
                  close
                  @click:close="compareFirst = null"
                  :outlined="!compareFirst"
                  :disabled="!compareFirst"
                >
                  <span v-if="compareFirst">
                    {{ summaryForItem(compareFirst) }}
                  </span>
                  <span v-else class="font-italic">Select another run</span>
                </v-chip>
              </v-col>
              <v-col cols="auto" class="mx-2">
                <v-btn icon @click="swapOrder">
                  <v-icon>{{ swapIcon }}</v-icon>
                </v-btn>
              </v-col>
              <v-col cols="5" class="d-flex" style="justify-content: start">
                <v-chip
                  close
                  @click:close="compareSecond = null"
                  :outlined="!compareSecond"
                  :disabled="!compareSecond"
                >
                  <span v-if="compareSecond">
                    {{ summaryForItem(compareSecond) }}
                  </span>
                  <span v-else class="font-italic">Select another run</span>
                </v-chip>
              </v-col>
            </v-row>
          </v-card-text>
          <v-card-actions>
            <v-spacer></v-spacer>
            <v-btn
              color="primary"
              outlined
              :to="compareLink"
              :disabled="!compareFirst || !compareSecond"
            >
              Compare Runs
            </v-btn>
            <v-spacer></v-spacer>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>
    <v-row class="mt-5">
      <v-col cols="12">
        <search-result-list
          :items="items"
          @mark-compare="markCommitForCompare"
        ></search-result-list>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import SearchResultList from '@/components/search/SearchResultList.vue'
import { vxm } from '@/store'
import {
  RepoId,
  SearchItem,
  SearchItemBranch,
  SearchItemCommit,
  SearchItemRun
} from '@/store/types'
import { Watch } from 'vue-property-decorator'
import { debounce } from '@/util/Debouncer'
import { mdiSwapHorizontalBold } from '@mdi/js'
import { RawLocation } from 'vue-router'
import RepoSelectionComponent from '@/components/misc/RepoSelectionComponent.vue'

@Component({
  components: { RepoSelectionComponent, SearchResultList }
})
export default class Search extends Vue {
  private searchQuery = ''
  private items: SearchItem[] = []
  private debouncedLookup = debounce(this.executeSearch, 300)
  private compareFirst: SearchItem | null = null
  private compareSecond: SearchItem | null = null
  private repoId: RepoId | null = null
  private constrainToRepo = false

  @Watch('searchQuery')
  private onQueryChange() {
    this.debouncedLookup()
  }

  private get runInUrl() {
    return this.$route.params['runId']
  }

  private get repoIdInUrl() {
    return this.$route.query['repoId']
  }

  @Watch('runInUrl')
  @Watch('repoIdInUrl')
  private async adjustToUrl() {
    if (this.repoIdInUrl) {
      this.repoId = this.repoIdInUrl as string
      this.constrainToRepo = true
    }

    if (this.runInUrl) {
      const result = await vxm.runSearchModule.searchRun({
        query: this.runInUrl,
        repoId: this.repoId || undefined
      })
      if (!result) {
        return
      }
      this.compareFirst = result[0]
    }
  }

  private get allRepos() {
    return vxm.repoModule.allRepos
  }

  private markCommitForCompare(item: SearchItem) {
    if (!this.compareFirst) {
      this.compareFirst = item
      return
    }
    this.compareSecond = item
  }

  private swapOrder() {
    const tmp = this.compareFirst
    this.compareFirst = this.compareSecond
    this.compareSecond = tmp
  }

  private summaryForItem(item: SearchItem) {
    if (item instanceof SearchItemCommit) {
      return item.summary
    }
    if (item instanceof SearchItemBranch) {
      return item.name
    }
    return item.tarDescription || item.commitSummary || item.id
  }

  private get compareLink(): RawLocation | null {
    if (!this.compareFirst || !this.compareSecond) {
      return null
    }
    const params: { first: string; second: string } = { first: '', second: '' }
    const query: { hash1?: string; hash2?: string } = {}

    if (this.compareFirst instanceof SearchItemRun) {
      params.first = this.compareFirst.id
    } else {
      params.first = this.compareFirst.repoId
      query.hash1 = this.compareFirst.hash
    }
    if (this.compareSecond instanceof SearchItemRun) {
      params.second = this.compareSecond.id
    } else {
      params.second = this.compareSecond.repoId
      query.hash2 = this.compareSecond.hash
    }

    return {
      name: 'run-comparison',
      params,
      query
    }
  }

  private async executeSearch() {
    if (this.searchQuery.length === 0) {
      this.items = []
      return
    }
    this.items = await vxm.runSearchModule.searchRun({
      query: this.searchQuery,
      repoId: this.constrainToRepo ? this.repoId || undefined : undefined
    })
  }

  private mounted() {
    this.adjustToUrl()
  }

  private readonly swapIcon = mdiSwapHorizontalBold
}
</script>
