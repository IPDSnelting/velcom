<template>
  <v-autocomplete
    :disabled="disabled"
    :items="repos"
    :rules="rules"
    :value="repoId"
    :label="label"
    :hide-details="rules.length === 0"
    :clearable="clearable"
    @input="setRepoId"
    item-text="name"
    item-value="id"
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
  private readonly repoId!: string

  @Prop({ default: () => [] })
  private readonly rules!: [(input: string) => string | boolean]

  @Prop({ default: () => [] })
  private readonly repos!: [Repo]

  @Prop({ default: false })
  private readonly disabled!: boolean

  @Prop({ default: 'Repository name' })
  private readonly label!: string

  @Prop({ default: false })
  private readonly clearable!: boolean

  private setRepoId(newValue: string) {
    this.$emit('value', newValue)
  }

  // ============== ICONS ==============
  private readonly repoIcon = mdiSourceBranch
  // ==============       ==============
}
</script>

<style scoped></style>
