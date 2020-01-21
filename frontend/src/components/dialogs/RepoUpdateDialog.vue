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
            <v-container class="pa-0">
              <v-row no-gutters align="center">
                <transition name="fade">
                  <span
                    v-if="tokenState == 'delete'"
                    class="section-header mr-4"
                  >TOKEN WILL BE DELETED</span>
                </transition>
                <transition name="fade">
                  <span>
                    <v-btn
                      v-if="tokenState == 'unchanged'"
                      @click="tokenState = 'modify'"
                      text
                      outlined
                      class="mr-2"
                      color="primary"
                    >Change token</v-btn>
                    <v-btn
                      v-if="tokenState == 'unchanged'"
                      @click="tokenState = 'delete'"
                      text
                      outlined
                      class="mr-2"
                      color="error"
                    >Delete token</v-btn>
                    <v-btn
                      v-if="tokenState == 'modify' || tokenState == 'delete'"
                      @click="tokenState = 'unchanged'"
                      text
                      outlined
                      color="error"
                    >{{ tokenState == 'modify' ? "KEEP OLD TOKEN" : "UNDO"}}</v-btn>
                  </span>
                </transition>
                <transition name="fade">
                  <v-text-field
                    v-if="tokenState == 'modify'"
                    :rules="[notEmpty]"
                    label="*New token"
                    v-model="newToken"
                    dense
                    hide-details="auto"
                    class="ml-4 mt-0 pt-0"
                  ></v-text-field>
                </transition>
              </v-row>
            </v-container>
            <div class="section-header mt-3 mb-2">BRANCHES</div>
            <v-data-iterator
              :items="branchObjects"
              :items-per-page="itemsPerPage"
              :footer-props="{ itemsPerPageOptions: itemsPerPageOptions }"
              sort-by="lowerCased"
              :hide-default-footer="branchObjects.length < itemsPerPage"
            >
              <template v-slot:header>
                <v-checkbox
                  dense
                  class="my-0 pt-0 font-italic"
                  label="Track all branches"
                  :input-value="newTrackedBranches.length == repo.branches.length"
                  @change="toggleAll"
                ></v-checkbox>
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
import { Repo } from '@/store/types'
import { Prop, Watch } from 'vue-property-decorator'
import { vxm } from '@/store/index'
import { mdiMagnify } from '@mdi/js'

@Component
export default class RepoUpdateDialog extends Vue {
  private dialogOpen: boolean = false

  private remoteUrl: string = ''
  private repoName: string = ''
  private newToken: string = ''

  private tokenState: 'delete' | 'modify' | 'unchanged' = 'unchanged'
  private itemsPerPage: number = 30
  private itemsPerPageOptions: number[] = [1, 10, 20, 30, 50, 100, -1]

  private formValid: boolean = false

  private newTrackedBranches: string[] = []

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

  private notEmpty(input: string): boolean | string {
    return input.trim().length > 0 ? true : 'This field must not be empty!'
  }

  @Watch('dialogOpen')
  @Watch('repoId')
  watchIdUpdates() {
    this.remoteUrl = this.repo.remoteURL
    this.repoName = this.repo.name
    this.tokenState = 'unchanged'

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
    let newToken
    if (this.tokenState === 'modify') {
      newToken = this.newToken
    } else if (this.tokenState === 'delete') {
      newToken = null
    }
    vxm.repoModule
      .updateRepo({
        repoToken: newToken,
        id: this.repoId,
        name: this.repoName,
        remoteUrl: this.remoteUrl,
        trackedBranches: this.newTrackedBranches
      })
      .then(() => (this.dialogOpen = false))
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
