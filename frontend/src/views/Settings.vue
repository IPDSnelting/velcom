<template>
  <v-container>
    <v-row v-if="isAdmin">
      <v-col>
        <CleanupDimensions></CleanupDimensions>
      </v-col>
    </v-row>
    <v-row v-if="!isAdmin" justify="center" class="mt-4">
      <v-col cols="auto">
        <pre class="font-weight-bold">
           .----------.      ____________
          /  .-.  .-.  \    /            \
         /   | |  | |   \   |  Aaaaaaah! |
         \   `-'  `-'  _/   / ___________/
         /\     .--.  / |  /_/
         \ |   /  /  / /
         / |   `--' /\ \
        / / `-------' \ \      Jym Dyer
        </pre>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import CleanupDimensions from '@/components/cleanup/CleanupDimensions.vue'
import { vxm } from '@/store'

@Component({
  components: { CleanupDimensions }
})
export default class Settings extends Vue {
  private get isAdmin() {
    return vxm.userModule.isAdmin
  }
}
</script>
