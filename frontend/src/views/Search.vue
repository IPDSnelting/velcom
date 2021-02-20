<template>
  <v-container>
    <v-row>
      <v-col>
        <h1>Voogle</h1>
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="12">
        <search-result-list :items="results"></search-result-list>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import SearchResultList from '@/components/search/SearchResultList.vue'
import { CommitDescription, ShortRunDescription } from '@/store/types'
import { vxm } from '@/store'

@Component({
  components: { SearchResultList }
})
export default class Search extends Vue {
  private get results() {
    return [
      new CommitDescription(
        vxm.repoModule.allRepos[0].id,
        'hash me',
        'I Al Istannen',
        new Date(),
        'This is a summary mate!'
      ),
      new ShortRunDescription(
        'run me',
        undefined,
        'This is a nice tar',
        undefined
      ),
      new ShortRunDescription(
        'run me',
        'This a commit',
        undefined,
        'commit hash'
      )
    ]
  }
}
</script>
