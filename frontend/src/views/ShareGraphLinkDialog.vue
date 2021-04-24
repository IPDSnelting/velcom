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
          <v-row no-gutters>
            <v-col
              v-for="{
                label,
                selectable,
                unselectableMessage,
                key
              } in selectableOptions"
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
import { vxm } from '@/store'
import { mdiLinkVariantPlus } from '@mdi/js'
import { copyToClipboard } from '@/util/ClipboardUtils'
import { PermanentLinkOptions } from '@/store/modules/detailGraphStore'
import { Prop, Watch } from 'vue-property-decorator'
import { RawLocation } from 'vue-router'

@Component
export default class ShareGraphLinkDialog extends Vue {
  private dialogOpen: boolean = false

  private options: PermanentLinkOptions = {
    includeYZoom: true,
    includeXZoom: true,
    includeDimensions: true
  }

  @Prop()
  private linkGenerator!: (options: PermanentLinkOptions) => RawLocation

  private get selectableOptions() {
    return [
      {
        label: 'Use X-axis zoom instead of start/end date',
        selectable: true,
        unselectableMessage: 'That you see this is a bug. Please report it :)',
        key: 'includeXZoom'
      },
      {
        label: 'Include Y-axis zoom',
        selectable:
          vxm.detailGraphModule.zoomYStartValue !== null ||
          vxm.detailGraphModule.zoomYEndValue !== null,
        unselectableMessage: "You haven't zoomed the Y axis",
        key: 'includeYZoom'
      },
      {
        label: 'Include dimensions',
        selectable: vxm.detailGraphModule.selectedDimensions.length > 0,
        unselectableMessage: "You haven't selected any dimensions",
        key: 'includeDimensions'
      }
    ]
  }

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
        includeDimensions: true,
        includeXZoom: true,
        includeYZoom: true
      }
    }
  }

  // ICONS
  private permanentLinkIcon = mdiLinkVariantPlus
}
</script>
