<template>
  <div>
    <v-dialog width="600" v-model="dialogOpen">
      <template #activator="{ on }">
        <slot :on="on" name="activator"></slot>
      </template>

      <v-card>
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
              ></v-radio>
            </v-radio-group>
            <v-text-field
              :disabled="!this.idRequired()"
              :rules="[nonEmptyID]"
              label="*Repository ID"
              v-model="repoID"
            ></v-text-field>
            <v-text-field
              :rules="[nonEmptyToken]"
              label="Access token"
              v-model="token"
            ></v-text-field>
          </v-form>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="primary" :disabled="!formValid" @click="login"
            >Login</v-btn
          >
          <v-spacer></v-spacer>
        </v-card-actions>
        <v-alert class="mb-1" type="error" :value="error.length > 0">{{
          error
        }}</v-alert>
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
import { store, vxm } from '../store/classIndex'

@Component
export default class LoginDialog extends Vue {
  private repoID: string = ''
  private token: string = ''

  private roles: Array<string> = ['Web-Admin', 'Repository-Admin']
  private role: string = this.roles[0]

  private formValid: boolean = false
  private dialogOpen: boolean = false

  private error: string = ''

  @Watch('role')
  private onRoleChange() {
    ;(this.$refs.form as any).validate()
  }

  private idRequired(): boolean {
    return this.role !== 'Web-Admin'
  }

  private nonEmptyID(input: string): boolean | string {
    return !this.idRequired() || input.trim().length > 0
      ? true
      : 'This field must not be empty!'
  }

  private nonEmptyToken(input: string): boolean | string {
    return input.length > 0 ? true : 'This field must not be empty!'
  }

  private login() {
    var payload: {
      role: string
      asRepoAdmin: boolean
      token: string
    }

    if (this.role === 'Web-Admin') {
      payload = {
        role: 'ADMIN',
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
    vxm.userModule.logIn(payload)
  }
}
</script>

<style scoped></style>
