<template>
  <v-card outlined tile max-width="400">
    <v-card-actions>
      <v-card-title>
        <v-checkbox
          v-model="repoSelected"
          :color="color"
          @change="updateSelected"
        ></v-checkbox>
        <router-link
          class="ml-3 mx-auto"
          :to="{ name: 'repo-detail', params: { id: repo.id } }"
          tag="button"
        >
          <span>{{ repo.name }}</span>
        </router-link>
      </v-card-title>

      <v-spacer></v-spacer>
      <v-btn icon @click="showBranches = !showBranches">
        <v-icon>{{
          showBranches ? 'mdi-chevron-up' : 'mdi-chevron-down'
        }}</v-icon>
      </v-btn>
    </v-card-actions>
    <v-expand-transition>
      <div v-show="showBranches">
        <v-divider></v-divider>

        <v-card-text>
          <div v-for="branch in this.repo.trackedBranches" :key="branch">
            <v-checkbox
              v-model="selectedBranches"
              :label="branch"
              :value="branch"
              :disabled="!repoSelected"
              :color="color"
              hide-details
            />
          </div>
        </v-card-text>
      </div>
    </v-expand-transition>
  </v-card>
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

  get repo(): Repo {
    return vxm.repoModule.repoByID(this.$props.repoID)
  }

  get color(): string {
    return vxm.colorModule.colorByIndex(this.$props.index)
  }

  get selectedBranches(): string[] {
    return this.repo.trackedBranches
  }

  private repoSelected: boolean = true
  private showBranches: boolean = false

  updateSelected() {
    this.$emit('updateSelect', this.$props.repoID, this.repoSelected)
  }

  created() {
    console.log('hallo')
  }

  @Model('updateSelect')

  mounted() {
    this.updateSelected()
  }
}
</script>
