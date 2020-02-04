<template>
  <v-combobox
    :value="commit"
    :items="allCommits"
    :filter="commitFilter"
    :search-input.sync="search"
    :disabled="disabled"
    @input="$emit('value', $event)"
    item-value="hash"
    item-text="hash"
    label="Commit"
    placeholder="Search for a hash or message"
    hide-selected
    return-object
    clearable
  >
    <template v-slot:no-data>
      <v-list-item>
        <span class="subheading">Enter commit hash:</span>
        <v-chip class="ml-2" small color="primary" outlined>{{ search }}</v-chip>
      </v-list-item>
    </template>
    <template v-slot:item="{ item }">
      <v-list-item-content>
        <v-list-item-title>{{ item.hash }}</v-list-item-title>
        <v-list-item-subtitle>{{ item.message }}</v-list-item-subtitle>
      </v-list-item-content>
    </template>
  </v-combobox>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Model, Prop } from 'vue-property-decorator'
import { Repo, Commit } from '../store/types'
import { mdiSourceBranch } from '@mdi/js'

@Component
export default class CommitSelectionComponent extends Vue {
  @Prop({ default: () => [] })
  private allCommits!: Commit[]

  @Prop({ default: false })
  private disabled!: boolean

  @Model('value')
  private commit!: string | Commit

  private search: string = ''

  private commitFilter(item: Commit, queryText: any, itemText: any) {
    return (
      item.hash.toLocaleLowerCase().indexOf(queryText.toLocaleLowerCase()) >
        -1 ||
      (item.message &&
        item.message
          .toLocaleLowerCase()
          .indexOf(queryText.toLocaleLowerCase()) > -1)
    )
  }

  // ============== ICONS ==============
  private repoIcon = mdiSourceBranch
  // ==============       ==============
}
</script>

<style scoped>
</style>
