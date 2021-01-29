<template>
  <v-container>
    <v-row>
      <v-col>
        <v-card>
          <v-card-title>
            <v-toolbar color="toolbarColor" dark>
              Select Runs To Compare
            </v-toolbar>
          </v-card-title>
          <v-card-text>
            <v-row>
              <v-col>
                <run-search
                  :initial-first-run-id="firstRunId"
                  :repo-id="effectiveRepoId"
                ></run-search>
              </v-col>
            </v-row>
            <v-row align="center" justify="center">
              <v-col cols="auto">
                <v-checkbox
                  v-model="constrainToRepo"
                  label="Limit searches to the following repo"
                ></v-checkbox>
              </v-col>
              <v-col cols="auto">
                <repo-selection-component
                  v-model="repoId"
                  :repos="allRepos"
                ></repo-selection-component>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import RunSearch from '@/components/search/RunSearch.vue'
import RepoSelectionComponent from '@/components/RepoSelectionComponent.vue'
import { vxm } from '@/store'
import { Watch } from 'vue-property-decorator'

@Component({
  components: { RepoSelectionComponent, RunSearch }
})
export default class PrepareRunCompare extends Vue {
  private repoId: string = ''
  private constrainToRepo: boolean = false

  @Watch('urlRepoId')
  private onUrlRepoIdChange() {
    if (this.urlRepoId) {
      this.repoId = this.urlRepoId
      this.constrainToRepo = true
    }
  }

  private get firstRunId() {
    return this.$route.params['first']
  }

  private get urlRepoId(): string | undefined {
    return this.$route.query['repoId'] as string | undefined
  }

  private get effectiveRepoId(): string | undefined {
    return this.constrainToRepo ? this.repoId : ''
  }

  private get allRepos() {
    return vxm.repoModule.allRepos
  }

  private mounted() {
    this.onUrlRepoIdChange()
  }
}
</script>
