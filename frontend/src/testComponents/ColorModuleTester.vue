<template>
  <div>
    <v-container>
      <v-layout text-center wrap>
        <v-flex mb-5 xs12>
          <h1 class="headline font-weight-bold mb-3">
            Testing the ColorModule
          </h1>
          <v-layout justify-center>
            <v-btn @click="addColor(1)">add color</v-btn>
          </v-layout>
          <div v-for="(color, index) in allColors" :key="index">
            <v-card :color="color">color no. {{ index }}</v-card>
          </div>
        </v-flex>
      </v-layout>
    </v-container>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import { mapState, mapActions, mapGetters } from 'vuex'

export default Vue.extend({
  name: 'ColorModuleTester',

  computed: {
    ...mapGetters({
      allColors: 'colorModule/allColors',
      allRepos: 'repoModule/allRepos'
    })
  },

  methods: {
    ...mapActions('colorModule', { addColor: 'addColors' })
  },

  mounted () {
    this.$store.dispatch('repoModule/fetchRepos')
  },

  watch: {
    allRepos: function () {
      if (this.allColors.length < this.allRepos.length) {
        let diff = this.allRepos.length - this.allColors.length
        this.$store.dispatch('colorModule/addColors', diff)
      }
    }
  }
})
</script>
