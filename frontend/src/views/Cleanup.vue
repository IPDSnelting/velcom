<template>
  <v-container>
    <v-row>
      <v-col>
        <CleanupDimensions></CleanupDimensions>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import CleanupDimensions from '@/components/cleanup/CleanupDimensions.vue'
@Component({
  components: { CleanupDimensions }
})
export default class Cleanup extends Vue {}
</script>
