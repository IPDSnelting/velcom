<template>
  <div>
    <v-container>
      <v-layout text-center wrap>
        <v-flex mb-5 xs12>
          <h1 class="headline font-weight-bold mb-3">Testing the RepoModule</h1>
          <div v-for="(repo, index) in allRepos" :key="index">
            <v-btn @click="deleteRepo(repo.id)">delete</v-btn>
            <p>
              {{ index }}. Repository "{{ repo.name }}" with ID: {{ repo.id }}
            </p>
          </div>
        </v-flex>
      </v-layout>
    </v-container>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
// import { mapState, mapActions, mapGetters } from 'vuex'
import { store, vxm } from '../store/modules/repoModule/repoModuleClass'

export default Vue.extend({
  name: 'RepoModuleTester',

  computed: {
    allRepos: function() {
      return vxm.repoModule.allRepos
    }

    /* ...mapGetters({
      allRepos: 'repoModule/allRepos'
    }) */
  },

  methods: {
    deleteRepo(id: string) {
      vxm.repoModule.deleteRepo(id)
    },

    repoByID: function(id: string) {
      return vxm.repoModule.allRepos
    }
    // ...mapActions('repoModule', { deleteRepo: 'deleteRepo' })
  },

  mounted() {
    vxm.repoModule.fetchRepos()
    // this.$store.dispatch('repoModule/fetchRepos')
  }
})
</script>
