<template>
  <div>
    <v-dialog width="600" v-model="dialogOpen">
      <template #activator="{ on }">
        <slot :on="on" name="activator"></slot>
      </template>

      <v-card>
        <v-toolbar dark color="primary">
          <v-toolbar-title>Update Repository</v-toolbar-title>
        </v-toolbar>
        <v-card-text>
          <v-form v-model="formValid" ref="form">
            <div class="section-header mt-3 mb-2">GENERAL SETTINGS</div>

            <v-text-field :rules="[notEmpty]" label="*Remote URL" v-model="remoteUrl"></v-text-field>
            <v-text-field :rules="[notEmpty]" label="*Repository name" v-model="repoName"></v-text-field>
            <div class="section-header mt-3 mb-2">BRANCHES</div>
            <v-data-iterator
              :items="branchObjects"
              :custom-filter="filterName"
              :search="searchValue"
              sort-by="lowerCased"
            >
              <template v-slot:header>
                <v-text-field :prepend-icon="searchIcon" label="Search..." v-model="searchValue"></v-text-field>
                <v-checkbox
                  dense
                  class="my-0 py-0"
                  label="Track all branches"
                  :input-value="newTrackedBranches.length == repo.branches.length"
                  @change="toggleAll"
                ></v-checkbox>
                <div class="section-header mt-3 mb-2">TRACKED BRANCHES</div>
              </template>
              <template v-slot:default="props">
                <v-row>
                  <v-col
                    cols="4"
                    class="my-1 py-0"
                    v-for="branch in props.items"
                    :key="branch.name"
                  >
                    <v-checkbox
                      class="my-0 py-0"
                      v-model="newTrackedBranches"
                      dense
                      :label="branch.name"
                      :value="branch.name"
                    ></v-checkbox>
                  </v-col>
                </v-row>
              </template>
            </v-data-iterator>
          </v-form>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="primary" :disabled="!formValid" @click="updateRepo">Update Repository</v-btn>
          <v-spacer></v-spacer>
          <v-btn color="error" @click="dialogOpen = false">Close</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Repo } from '../store/types'
import { Prop, Watch } from 'vue-property-decorator'
import { vxm } from '../store/classIndex'
import { mdiMagnify } from '@mdi/js'

@Component
export default class RepoUpdateDialog extends Vue {
  private dialogOpen: boolean = false

  private remoteUrl: string = ''
  private repoName: string = ''

  private formValid: boolean = false

  private newTrackedBranches: string[] = []

  private searchValue: string = ''

  @Prop()
  private repoId!: string

  get branchObjects(): { name: string; lowerCased: string }[] {
    return this.repo.branches.map(it => ({
      name: it,
      lowerCased: it.toLowerCase()
    }))
  }

  get repo(): Repo {
    return vxm.repoModule.repoByID(this.repoId)!
  }

  private filterName(items: { lowerCased: string }[], search: string) {
    return items.filter(
      input => input.lowerCased.indexOf(this.searchValue.toLowerCase()) >= 0
    )
  }

  private notEmpty(input: string): boolean | string {
    return input.trim().length > 0 ? true : 'This field must not be empty!'
  }

  @Watch('dialogOpen')
  @Watch('repoId')
  watchIdUpdates() {
    this.remoteUrl = this.repo.remoteURL
    this.repoName = this.repo.name
    this.searchValue = ''

    this.newTrackedBranches = Object.assign([], this.repo.trackedBranches)
  }

  toggleAll() {
    if (
      this.newTrackedBranches.length === this.repo.branches.length &&
      this.newTrackedBranches.length > 0
    ) {
      this.newTrackedBranches = []
    } else {
      this.newTrackedBranches = Object.assign([], this.repo.branches)
    }
  }

  updateRepo() {
    let newRepo = new Repo(
      this.repo.id,
      this.repoName,
      this.repo.branches,
      this.newTrackedBranches,
      this.repo.measurements,
      this.remoteUrl
    )
    // TODO: How do we get the token??
    vxm.repoModule.updateRepo({
      repoToken: '12345',
      id: this.repoId,
      name: this.repoName,
      remoteUrl: this.remoteUrl
    })
  }

  mounted() {
    Vue.nextTick(() => this.watchIdUpdates())
  }

  // ============== ICONS ==============
  private searchIcon = mdiMagnify
  // ==============       ==============
}
</script>

<style scoped>
.section-header {
  font-variant: small-caps;
}
</style>
