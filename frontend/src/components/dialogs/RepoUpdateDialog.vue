<template>
  <div>
    <v-dialog width="600" v-model="dialogOpen">
      <template #activator="{ on }">
        <slot :on="on" name="activator"></slot>
      </template>

      <v-card>
        <v-toolbar dark color="toolbarColor">
          <v-toolbar-title>Update Repository</v-toolbar-title>
        </v-toolbar>
        <v-card-text>
          <v-form v-model="formValid" ref="form">
            <div class="section-header mt-3 mb-2">GENERAL SETTINGS</div>

            <v-text-field
              :rules="[notEmpty]"
              label="*Remote URL"
              v-model="remoteUrl"
            ></v-text-field>
            <v-text-field
              :rules="[notEmpty]"
              label="*Repository name"
              v-model="repoName"
            ></v-text-field>
            <v-container class="pa-0" fluid>
              <v-row no-gutters align="center">
                <transition name="fade">
                  <span
                    v-if="tokenState === 'delete'"
                    class="section-header mr-4"
                    >TOKEN WILL BE DELETED</span
                  >
                </transition>
                <transition name="fade">
                  <span>
                    <v-btn
                      v-if="tokenState === 'unchanged'"
                      @click="tokenState = 'modify'"
                      text
                      outlined
                      class="mr-2"
                      color="primary"
                      >Change token</v-btn
                    >
                    <v-btn
                      v-if="tokenState === 'unchanged'"
                      @click="tokenState = 'delete'"
                      text
                      outlined
                      class="mr-2"
                      color="error"
                      >Delete token</v-btn
                    >
                    <v-btn
                      v-if="tokenState === 'modify' || tokenState === 'delete'"
                      @click="tokenState = 'unchanged'"
                      text
                      outlined
                      color="error"
                      >{{
                        tokenState === 'modify' ? 'KEEP OLD TOKEN' : 'UNDO'
                      }}</v-btn
                    >
                  </span>
                </transition>
                <transition name="fade">
                  <v-text-field
                    v-if="tokenState === 'modify'"
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
            <div class="section-header mt-8 mb-2">BRANCHES</div>
            <v-data-iterator
              :custom-filter="filterName"
              :search="searchValue"
              :items="branchObjects"
              :items-per-page="itemsPerPage"
              :footer-props="{ itemsPerPageOptions: itemsPerPageOptions }"
              sort-by="lowerCased"
              :hide-default-footer="branchObjects.length < itemsPerPage"
            >
              <template v-slot:header>
                <v-text-field
                  :prepend-icon="searchIcon"
                  label="Search for a branch..."
                  v-model="searchValue"
                ></v-text-field>
                <v-checkbox
                  dense
                  class="my-0 pt-0 font-italic"
                  label="Track all branches"
                  :input-value="
                    newTrackedBranches.length === repo.branches.length
                  "
                  @change="toggleAll"
                ></v-checkbox>
              </template>
              <template v-slot:default="props">
                <v-row align="center">
                  <v-col
                    cols="4"
                    class="my-1 py-0"
                    style="word-wrap: anywhere"
                    v-for="branch in props.items"
                    :key="branch.name"
                  >
                    <v-checkbox
                      class="my-0 py-0 full-height-label-checkbox"
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
          <v-btn
            color="primary"
            class="mr-3"
            :disabled="!formValid"
            @click="updateRepo"
            >Update Repository</v-btn
          >
          <v-btn color="error" text outlined @click="dialogOpen = false"
            >Close</v-btn
          >
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
import { vxm } from '@/store'
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
  private searchValue: string = ''

  private newTrackedBranches: string[] = []

  @Prop()
  private repoId!: string

  get branchObjects(): { name: string; lowerCased: string }[] {
    return this.repo.branches.map(it => ({
      name: it.name,
      lowerCased: it.name.toLowerCase()
    }))
  }

  get repo(): Repo {
    return vxm.repoModule.repoById(this.repoId)!
  }

  private filterName(items: { lowerCased: string }[]) {
    return items.filter(
      input => input.lowerCased.indexOf(this.searchValue.toLowerCase()) >= 0
    )
  }

  private notEmpty(input: string): boolean | string {
    return input.trim().length > 0 ? true : 'This field must not be empty!'
  }

  @Watch('dialogOpen')
  @Watch('repoId')
  private watchIdUpdates() {
    this.remoteUrl = this.repo.remoteURL
    this.repoName = this.repo.name
    this.tokenState = 'unchanged'
    this.newToken = ''
    this.searchValue = ''

    this.newTrackedBranches = this.repo.branches
      .filter(it => it.tracked)
      .map(it => it.name)
  }

  private toggleAll() {
    if (
      this.newTrackedBranches.length === this.repo.branches.length &&
      this.newTrackedBranches.length > 0
    ) {
      this.newTrackedBranches = []
    } else {
      this.newTrackedBranches = this.repo.branches.map(branch => branch.name)
    }
  }

  private updateRepo() {
    let newToken: string | null | undefined
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

  mounted(): void {
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

<style>
/* Not scoped, as the v-label does not get any data attribute */
/*noinspection CssUnusedSymbol*/
.full-height-label-checkbox .v-label {
  height: 100% !important;
}
</style>
