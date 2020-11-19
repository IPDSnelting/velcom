<template>
  <v-dialog width="800" :value="open" @input="$emit('input', $event)">
    <v-card>
      <v-card-title>
        <v-toolbar dark color="primary">Upload TAR file</v-toolbar>
      </v-card-title>
      <v-card-text>
        <v-form v-model="vueFormValid" @submit="uploadTar">
          <v-text-field
            class="ml-3"
            label="Description"
            v-model="tarDescription"
            :rules="[notEmpty]"
          ></v-text-field>

          <v-row align="center" justify="start" class="mb-0 pb-0">
            <v-col cols="auto">
              <v-btn
                class="ma-0"
                text
                :color="assignToRepo ? 'warning' : 'primary'"
                @click="assignToRepo = !assignToRepo"
              >
                {{ assignToRepo ? 'Make stand-alone' : 'Assign to repo' }}
              </v-btn>
            </v-col>
            <v-col cols="auto">
              <repo-selection-component
                v-if="assignToRepo"
                v-model="repoId"
                :repos="allRepos"
              ></repo-selection-component>
            </v-col>
          </v-row>

          <file-select-component
            :error-style="tarFile == null"
            v-model="tarFile"
          ></file-select-component>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn
          :disabled="!formValid"
          color="primary"
          class="mr-5"
          @click="uploadTar"
        >
          Upload tar
        </v-btn>
        <v-btn color="error" text @click="$emit('input', false)">Close</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script lang="ts">
import Vue from 'vue'
import { Component, Model, Watch } from 'vue-property-decorator'
import RepoSelectionComponent from '@/components/RepoSelectionComponent.vue'
import { vxm } from '@/store'
import FileSelectComponent from '@/components/FileSelectComponent.vue'

@Component({
  components: { FileSelectComponent, RepoSelectionComponent }
})
export default class UploadTarDialog extends Vue {
  private vueFormValid: boolean = false
  private tarDescription: string = ''
  private repoId: string | null = null
  private tarFile: File | null = null
  private assignToRepo: boolean = false

  @Model('input', { type: Boolean })
  private open!: boolean

  private get allRepos() {
    return vxm.repoModule.allRepos
  }

  @Watch('open')
  private onOpen() {
    if (this.open) {
      this.tarDescription = ''
      this.tarFile = null
      this.repoId = null
    }
  }

  private get formValid(): boolean {
    return this.vueFormValid && this.tarFile !== null
  }

  private async uploadTar() {
    if (!this.formValid) {
      return
    }
    await vxm.queueModule.uploadTar({
      repoId: this.repoId,
      description: this.tarDescription,
      file: this.tarFile!
    })
    await vxm.queueModule.fetchQueue()
    this.$emit('input', false)
  }

  private notEmpty(input: string): boolean | string {
    return input.trim().length > 0 ? true : 'This field must not be empty!'
  }
}
</script>
