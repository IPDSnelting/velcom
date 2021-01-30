<template>
  <v-container fluid class="ma-0 pa-0">
    <v-row align="baseline" justify="center" no-gutters>
      <v-col>
        <v-card>
          <v-card-text class="ma-0 pa-0">
            <v-container fluid class="ma-0 px-5">
              <v-row align="center" justify="space-between" no-gutters>
                <v-col v-for="field in timeFields" :key="field.ref">
                  <v-menu
                    :ref="field.ref"
                    v-model="field.opened"
                    :return-value.sync="field.model"
                    :close-on-content-click="false"
                    transition="scale-transition"
                    offset-y
                    min-width="290px"
                  >
                    <template #activator="{ on }">
                      <v-text-field
                        class="mr-5 mb-5"
                        hide-details="auto"
                        :value="field.model"
                        :disabled="dateLocked === field.role"
                        :label="field.role + ':'"
                        :prepend-icon="dateIcon"
                        readonly
                        v-on="on"
                        :rules="field.rules"
                      >
                        <v-icon
                          slot="append"
                          @click="lockDates(field.role)"
                          class="lock-button"
                        >
                          {{ dateLocked === field.role ? lock : openLock }}
                        </v-icon>
                      </v-text-field>
                    </template>
                    <v-date-picker
                      hide-details="auto"
                      v-model="field.model"
                      no-title
                      scrollable
                      :max="today"
                      :allowed-dates="field.allowedDates"
                      :events="[startTimeString, endTimeString]"
                      :event-color="
                        date => (date === startTimeString ? 'blue' : 'red')
                      "
                    >
                      <v-btn
                        text
                        color="primary"
                        @click="field.saveFunction(field, today)"
                      >
                        Today
                      </v-btn>
                      <v-spacer></v-spacer>
                      <v-btn text color="primary" @click="field.opened = false">
                        Cancel
                      </v-btn>
                      <v-btn
                        text
                        color="primary"
                        @click="field.saveFunction(field, field.model)"
                      >
                        OK
                      </v-btn>
                    </v-date-picker>
                  </v-menu>
                </v-col>

                <v-col>
                  <v-form @submit.prevent="saveDuration">
                    <v-text-field
                      @blur="saveDuration"
                      @input="temporaryDuration = $event"
                      :value="duration"
                      :disabled="dateLocked === 'neither'"
                      label="number of days to fetch:"
                      class="mr-5"
                      :rules="[ruleIsNumber]"
                    ></v-text-field>
                  </v-form>
                </v-col>
              </v-row>
            </v-container>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { mdiCalendar, mdiLock, mdiLockOpenVariant } from '@mdi/js'
import { vxm } from '@/store'

type TimeField = {
  ref: string
  model: string
  role: 'start' | 'end'
  opened: boolean
  saveFunction: (field: TimeField, value: string) => void
  rules: ((input: string) => string | boolean)[]
  allowedDates?: (input: string) => boolean
}

@Component
export default class RepoGraphTimespanControls extends Vue {
  // <!--<editor-fold desc="Fields">-->
  /**
   * The value of the "duration" input field. Not applied until saveDuration is called.
   */
  private temporaryDuration: string = '' + this.duration

  private dateLocked: 'start' | 'end' | 'neither' = 'end'
  private readonly timeFields: TimeField[] = [
    {
      ref: 'startDateMenu',
      model: this.startTimeString,
      role: 'start',
      opened: false,
      saveFunction: (field: TimeField, value: string) => {
        this.saveMenu(field.ref, value)
        vxm.detailGraphModule.startTime = new Date(value)
        this.$emit('reload-graph-data')
      },
      allowedDates: (value: string) => {
        return new Date(value) <= vxm.detailGraphModule.endTime
      },
      rules: []
    },
    {
      ref: 'endDateMenu',
      model: this.endTimeString,
      role: 'end',
      opened: false,
      saveFunction: (field: TimeField, value: string) => {
        this.saveMenu(field.ref, value)
        vxm.detailGraphModule.endTime = new Date(value)
        this.$emit('reload-graph-data')
      },
      allowedDates: (value: string) => {
        return new Date(value) >= vxm.detailGraphModule.startTime
      },
      rules: [this.ruleStopAfterStart]
    }
  ]
  // <!--</editor-fold>-->

  private saveMenu(ref: string, value: string) {
    let field: any
    const readRef = this.$refs[ref]

    if (readRef instanceof Vue) {
      field = readRef
    } else if (Array.isArray(readRef)) {
      field = readRef[0]
    }
    field.save(value)
  }

  // <!--<editor-fold desc="Duration">-->
  private get duration(): number {
    return vxm.detailGraphModule.duration
  }

  private saveDuration() {
    const duration = parseInt(this.temporaryDuration)

    if (isNaN(duration)) {
      return
    }

    if (this.duration === duration) {
      // Do not fetch everything again
      return
    }

    const durationAsMillis = duration * 1000 * 60 * 60 * 24 // ms * minutes * hours * days

    if (this.dateLocked === 'start') {
      vxm.detailGraphModule.endTime = new Date(
        vxm.detailGraphModule.startTime.getTime() + durationAsMillis
      )
    } else {
      vxm.detailGraphModule.startTime = new Date(
        vxm.detailGraphModule.endTime.getTime() - durationAsMillis
      )
    }

    this.$emit('reload-graph-data')
  }
  // <!--</editor-fold>-->

  // <!--<editor-fold desc="Start/Stop time">-->
  private get startTimeString(): string {
    return vxm.detailGraphModule.startTime.toISOString().substring(0, 10)
  }

  private get endTimeString(): string {
    return vxm.detailGraphModule.endTime.toISOString().substring(0, 10)
  }
  // <!--</editor-fold>-->

  private lockDates(date: 'start' | 'end'): void {
    if (this.dateLocked === 'neither') {
      this.dateLocked = date
    } else if ((this.dateLocked = date)) {
      this.dateLocked = 'neither'
    } else {
      this.dateLocked = this.dateLocked === 'start' ? 'end' : 'start'
    }
  }

  // <!--<editor-fold desc="Rules">-->
  private ruleStopAfterStart(input: string): boolean | string {
    return vxm.detailGraphModule.startTime <= new Date(input)
      ? true
      : 'You have to select a date after the first one!'
  }

  private ruleIsNumber(input: string): string | boolean {
    return isNaN(parseInt(input)) ? 'Please enter a number' : true
  }
  // <!--</editor-fold>-->

  private get today() {
    return new Date().toISOString().substr(0, 10)
  }

  // ============== ICONS ==============
  private dateIcon = mdiCalendar
  private openLock = mdiLockOpenVariant
  private lock = mdiLock
  // ==============       ==============
}
</script>
