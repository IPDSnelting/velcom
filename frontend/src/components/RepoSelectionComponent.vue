<template>
  <v-autocomplete
    :disabled="disabled"
    :items="repos"
    :rules="rules"
    :value="repoId"
    @input="setRepoId"
    item-text="name"
    item-value="id"
    label="Repository name"
    :hide-details="rules.length === 0"
    class="mt-0 pt-0"
  >
    <template v-slot:item="data">
      <v-list-item-avatar>
        <v-icon small>{{ repoIcon }}</v-icon>
      </v-list-item-avatar>
      <v-list-item-content>
        <v-list-item-title>{{ data.item.name }}</v-list-item-title>
        <v-list-item-subtitle>{{ data.item.id }}</v-list-item-subtitle>
      </v-list-item-content>
    </template>
  </v-autocomplete>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Model, Prop } from 'vue-property-decorator'
import { Repo } from '@/store/types'
import { mdiSourceBranch } from '@mdi/js'

@Component
export default class RepoSelectionComponent extends Vue {
  @Model('value', { type: String })
  private repoId!: string

  private setRepoId(newValue: string) {
    this.$emit('value', newValue)
  }

  @Prop({ default: () => [] })
  private rules!: [(input: string) => string | boolean]

  @Prop({ default: () => [] })
  private repos!: [Repo]

  @Prop({ default: false })
  private disabled!: boolean

  // ============== ICONS ==============
  private repoIcon = mdiSourceBranch
  // ==============       ==============
}
</script>

<style scoped></style>
