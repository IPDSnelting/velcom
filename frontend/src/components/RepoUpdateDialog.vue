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
            <v-text-field :rules="[notEmpty]" label="*Remote URL" v-model="remoteUrl"></v-text-field>
            <v-text-field :rules="[notEmpty]" label="*Repository name" v-model="repoName"></v-text-field>
            <div class="section-header mt-3 mb-2">TRACKED BRANCHES</div>
            <v-container>
              <v-row>
                <v-col cols="12">
                  <v-checkbox
                    dense
                    class="my-0 py-0"
                    label="Check all"
                    :input-value="newTrackedBranches.length == repo.branches.length"
                    @change="toggleAll"
                  ></v-checkbox>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="2" :key="branch" v-for="branch in repo.branches">
                  <v-checkbox
                    class="my-0 py-0"
                    v-model="newTrackedBranches"
                    dense
                    :label="branch"
                    :value="branch"
                  ></v-checkbox>
                </v-col>
              </v-row>
            </v-container>
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

@Component
export default class RepoUpdateDialog extends Vue {
  private dialogOpen: boolean = false

  private remoteUrl: string = ''
  private repoName: string = ''

  private formValid: boolean = false

  private newTrackedBranches: string[] = []

  @Prop()
  private repoId!: string

  get repo(): Repo {
    return vxm.repoModule.repoByID(this.repoId)
  }

  private notEmpty(input: string): boolean | string {
    return input.trim().length > 0 ? true : 'This field must not be empty!'
  }

  @Watch('dialogOpen')
  @Watch('repoId')
  watchIdUpdates() {
    this.remoteUrl = this.repo.remoteURL
    this.repoName = this.repo.name

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
}
</script>

<style scoped>
.section-header {
  font-variant: small-caps;
}
</style>
