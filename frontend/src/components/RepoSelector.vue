<template>
  <v-expansion-panel>
    <v-expansion-panel-header>
      <v-checkbox
        class="shrink mt-0"
        hide-details
        v-model="repoSelected"
        :color="color"
        @click.native.stop
        @change="updateSelected"
      ></v-checkbox>
      <router-link
        class="ml-3 mx-auto"
        :to="{ name: 'repo-detail', params: { id: repo.id } }"
        tag="button"
      >
        <span class="panel-header">{{ repo.name }}</span>
      </router-link>
    </v-expansion-panel-header>

    <v-expansion-panel-content>
      <div v-for="(branch, index) in this.repo.trackedBranches" :key="index">
        <v-checkbox
          hide-details
          class="shrink mt-0 ml-5"
          v-model="selectedBranches"
          :label="branch"
          :value="branch"
          :disabled="!repoSelected"
          :color="color"
          @change="updateSelected"
        />
      </div>
    </v-expansion-panel-content>
  </v-expansion-panel>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop, Model } from 'vue-property-decorator'
import { Store } from 'vuex'

import { vxm } from '../store/classIndex'
import { Repo } from '../store/types'

@Component
export default class RepoSelector extends Vue {
  @Prop({})
  private repoID!: string

  @Prop({})
  private index!: number

  private selectedBranches: string[] = []

  get repo(): Repo {
    return vxm.repoModule.repoByID(this.$props.repoID)!
  }

  get color(): string {
    return vxm.colorModule.colorByIndex(this.$props.index)
  }
  private repoSelected: boolean = true

  updateSelected() {
    this.$emit(
      'updateSelect',
      this.repo.id,
      this.repoSelected,
      this.selectedBranches
    )
  }

  @Model('updateSelect')
  mounted() {
    this.repo.trackedBranches.forEach(branch => {
      this.selectedBranches.push(branch)
    })
    this.updateSelected()
  }
}
</script>

<style scoped>
.panel-header {
  font-size: large;
}
</style>
