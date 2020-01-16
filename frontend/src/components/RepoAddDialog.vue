<template>
  <div>
    <v-dialog width="600" v-model="dialogOpen">
      <template #activator="{ on }">
        <slot :on="on" name="activator"></slot>
      </template>

      <v-card>
        <v-toolbar dark color="primary">
          <v-toolbar-title>Add Repository</v-toolbar-title>
        </v-toolbar>
        <v-card-text>
          <v-form v-model="formValid" ref="form">
            <v-text-field :rules="[notEmpty]" label="*Remote URL" v-model="remoteUrl"></v-text-field>
            <v-text-field :rules="[notEmpty]" label="*Repository name" v-model="repoName"></v-text-field>
            <v-text-field label="Repository access token" v-model="repoToken"></v-text-field>
          </v-form>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="primary" :disabled="!formValid" @click="addRepository">Add Repository</v-btn>
          <v-spacer></v-spacer>
        </v-card-actions>
        <v-alert class="mb-1" type="error" :value="error.length > 0">{{ error }}</v-alert>
      </v-card>
    </v-dialog>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import { Component, Watch } from 'vue-property-decorator'
import { Store } from 'vuex'
import { RootState } from '../store/types'
import { extractErrorMessage } from '../util/ErrorUtils'

@Component
export default class RepoAddDialog extends Vue {
  private remoteUrl: string = ''
  private repoName: string = ''
  private repoToken: string = ''

  private formValid: boolean = false
  private dialogOpen: boolean = false

  private error: string = ''

  private notEmpty(input: string): boolean | string {
    return input.trim().length > 0 ? true : 'This field must not be empty!'
  }

  private addRepository() {
    this.$store
      .dispatch('repoModule/addRepo', {
        name: this.repoName,
        remote_url: this.remoteUrl,
        token: this.repoToken
      })
      .then(it => {
        this.$emit('value', it)
        this.error = ''
      })
      .catch(it => {
        this.error = extractErrorMessage(it)
      })
  }
}
</script>

<style scoped>
</style>
