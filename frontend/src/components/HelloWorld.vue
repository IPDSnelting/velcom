<template>
  <v-container>
    <v-layout text-center wrap>
      <v-flex mb-5 xs12>
        <h2 class="headline font-weight-bold mb-3">Generating new colors</h2>

        <v-layout justify-center>
          <v-btn @click="addColor">add color</v-btn>
        </v-layout>
        <div v-for="(color, index) in allColors" :key="index">
          <v-card :color="color">color no. {{index}}</v-card>
        </div>
      </v-flex>
    </v-layout>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import { mapState, mapActions, mapGetters } from 'vuex'

export default Vue.extend({
  name: 'HelloWorld',

  data: () => ({
  }),

  computed: {
    ...mapGetters({
      allColors: 'colorModule/allColors'
    })
  },

  methods: {
    ...mapActions('colorModule', { addColor: 'addColor' })
  }
})
</script>
