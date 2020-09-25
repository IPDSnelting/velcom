<template>
  <div>
    <v-dialog width="600" v-model="dialogOpen">
      <template #activator="{ on }">
        <slot :on="on" name="activator"></slot>
      </template>

      <v-card id="login-dialog">
        <v-toolbar dark color="primary">
          <v-toolbar-title>Login</v-toolbar-title>
        </v-toolbar>
        <v-card-text>
          <v-form v-model="formValid" ref="form">
            <v-radio-group v-model="role">
              <template>
                <div>Log in as...</div>
              </template>
              <v-radio
                v-for="role in roles"
                :key="role"
                :label="role"
                :value="role"
                :data-cy="role"
              ></v-radio>
            </v-radio-group>
            <repo-selection
              :disabled="!idRequired"
              :repos="allRepos"
              :rules="[nonEmptyID]"
              v-model="repoID"
            ></repo-selection>
            <v-text-field
              type="password"
              :rules="[nonEmptyToken]"
              label="Access token"
              v-model="token"
              @keyup.enter="login"
              data-cy="password-input"
            ></v-text-field>
          </v-form>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="primary" :disabled="!formValid" @click="login"
            >Login</v-btn
          >
          <v-spacer></v-spacer>
          <v-btn color="error" @click="dialogOpen = false">Close</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import { Component, Watch } from 'vue-property-decorator'
import { Repo } from '@/store/types'
import { vxm } from '@/store'
import RepoSelectionComponent from '../RepoSelectionComponent.vue'

@Component({
  components: {
    'repo-selection': RepoSelectionComponent
  }
})
export default class LoginDialog extends Vue {
  private repoID: string = ''
  private token: string = ''

  private roles: string[] = ['Web-Admin', 'Repository-Admin']
  private role: string = this.roles[0]

  private formValid: boolean = false
  private dialogOpen: boolean = false

  @Watch('role')
  private onRoleChange() {
    ;(this.$refs.form as any).validate()
  }

  @Watch('dialogOpen')
  private onOpened(opened: boolean) {
    if (!opened) {
      this.token = ''
      this.repoID = ''
    }
  }

  get idRequired(): boolean {
    return this.role !== 'Web-Admin'
  }

  private nonEmptyID(input: string): boolean | string {
    return !this.idRequired || input.trim().length > 0
      ? true
      : 'This field must not be empty!'
  }

  private nonEmptyToken(input: string): boolean | string {
    return input.length > 0 ? true : 'This field must not be empty!'
  }

  private get allRepos(): Repo[] {
    return vxm.repoModule.allRepos
  }

  private login() {
    if (!this.formValid) {
      return
    }

    let payload: {
      role: string
      asRepoAdmin: boolean
      token: string
    }

    if (this.role === 'Web-Admin') {
      payload = {
        role: 'admin',
        asRepoAdmin: false,
        token: this.token
      }
    } else {
      payload = {
        role: this.repoID,
        asRepoAdmin: true,
        token: this.token
      }
    }
    vxm.userModule.logIn(payload).then(() => (this.dialogOpen = false))
  }
}
</script>

<style scoped></style>
