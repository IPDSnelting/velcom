<template>
  <v-dialog width="800px" v-model="dialogOpen">
    <template #activator="{ on }">
      <v-btn v-on="on" outlined text color="primary">
        Share graph
        <v-icon right>{{ permanentLinkIcon }}</v-icon>
      </v-btn>
    </template>
    <v-card>
      <v-card-title>
        <v-toolbar color="toolbarColor" dark>
          Share a permanent link to this graph
        </v-toolbar>
      </v-card-title>
      <v-card-text>
        <v-container fluid>
          <v-row no-gutters v-if="shareOptions.length > 0">
            <v-col
              v-for="{
                label,
                selectable,
                unselectableMessage,
                key
              } in shareOptions"
              :key="key"
            >
              <v-tooltip top :disabled="selectable">
                <template #activator="{ on }">
                  <span v-on="on">
                    <v-checkbox
                      v-on="on"
                      hide-details
                      v-model="options[key]"
                      :disabled="!selectable"
                      :indeterminate="!selectable"
                      :label="label"
                    ></v-checkbox>
                  </span>
                </template>
                {{ unselectableMessage }}
              </v-tooltip>
            </v-col>
          </v-row>
          <v-row>
            <v-col>
              <v-text-field
                id="url-field"
                readonly
                autofocus
                :value="permanentLinkUrl"
              >
                <template #append>
                  <div class="pb-2 pl-2">
                    <v-btn
                      outlined
                      text
                      color="primary"
                      @click="copyPermanentLink"
                    >
                      Copy
                    </v-btn>
                  </div>
                </template>
              </v-text-field>
            </v-col>
          </v-row>
        </v-container>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>
<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { mdiLinkVariantPlus } from '@mdi/js'
import { copyToClipboard } from '@/util/Clipboards'
import { PermanentLinkOptions } from '@/store/modules/detailGraphStore'
import { Prop, Watch } from 'vue-property-decorator'

export type Option = {
  label: string
  selectable: boolean
  unselectableMessage: string
  key: keyof PermanentLinkOptions
}

@Component
export default class ShareGraphLinkDialog extends Vue {
  private dialogOpen: boolean = false
  private options: PermanentLinkOptions = {
    includeYZoom: true,
    includeXZoom: true,
    includeDataRestrictions: true
  }

  @Prop()
  private readonly linkGenerator!: (options: PermanentLinkOptions) => string

  @Prop()
  private readonly shareOptions!: Option[]

  private get permanentLinkUrl() {
    return this.linkGenerator(this.options)
  }

  private copyPermanentLink() {
    copyToClipboard(this.permanentLinkUrl, this.$globalSnackbar)
  }

  @Watch('dialogOpen')
  private onDialogVisibilityChange() {
    if (this.dialogOpen) {
      this.options = {
        includeDataRestrictions: true,
        includeXZoom: true,
        includeYZoom: true
      }
    }
  }

  // ICONS
  private permanentLinkIcon = mdiLinkVariantPlus
}
</script>
