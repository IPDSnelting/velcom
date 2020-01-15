<template>
  <div>
    <v-container>
    <v-layout text-center wrap>
      <v-flex mb-5 xs12>
        <h1 class="headline font-weight-bold mb-3">Testing the RepoModule</h1>
        <div v-for="(repo, index) in allRepos" :key="index">
          <p>{{index }}. Repository "{{ repo.name }}" with ID: {{ repo.id }}</p>
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
  name: 'RepoModuleTester',

  computed: {
    ...mapGetters({
      allRepos: 'repoModule/allRepos'

    })
  },

  mounted () {
    this.$store.dispatch('repoModule/fetchRepos')
  }
})
</script>
