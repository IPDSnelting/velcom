<template>
  <v-container fluid class="ma-0 pa-0">
    <v-row>
      <v-col>
        <run-search-field></run-search-field>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import RunSearchField from '@/components/search/RunSearchField.vue'

@Component({
  components: { RunSearchField }
})
export default class RunSearch extends Vue {}
</script>
