<template>
  <div class="about">
    <v-container>
      <v-row>
        <v-col>
          <v-card>
            <v-card-title>
              <v-toolbar color="primary darken-1" dark>About VelCom</v-toolbar>
            </v-card-title>
            <v-card-text>
              <v-container fluid class="ma-0 pa-0">
                <v-row align="baseline" justify="center" no-gutters>
                  <v-col>
                    <v-row no-gutters align="baseline" justify="center">
                      <img src="@/assets/mini-logo.png" alt="logo" class="mb-5" />
                      <img src="@/assets/logo.png" alt="logo" class="mb-5" />
                    </v-row>
                    <v-row align="center" justify="center">
                      <p class="text-center">
                        "The name stands for 'Velocity Commit', and velocity is related to speed and
                        performance.
                        <br />It also sounds like a german saying 'welcome', which is
                        nice and friendly."
                      </p>
                    </v-row>
                    <v-row align="center" justify="center" class="mt-10">
                      <p class="text-center">
                        VelCom is a project which was realized as part of the lecture "Software Engineering Practice" at the Karlsruhe Institute of Technology (KIT) during winter semester 2019/20.
                        <br />The goal was to create a website that...
                      </p>
                    </v-row>
                    <v-row align="center" justify="center">
                      <ul>
                        <li>benchmarks software in the long term</li>
                        <li>manages repositories and commits in an intelligent manner</li>
                        <li>provides appealing visualisations of benchmarks</li>
                        <li>is easy to set up</li>
                      </ul>
                    </v-row>
                    <v-row align="center" justify="center" class="mt-10">
                      <p>
                        If you are interested in learning more about the maniacs behind VelCom, please visit
                        <a
                          href="https://www.aaaaaaah.de/"
                        >AAAAAAAH!</a>
                      </p>
                    </v-row>
                  </v-col>
                </v-row>
              </v-container>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
      <v-row>
        <v-col>
          <v-card>
            <v-card-title>
              <v-toolbar color="primary darken-1" dark>Impressum</v-toolbar>
            </v-card-title>
            <v-card-text>
              <v-container fluid>
                <v-row align="baseline" justify="center">
                  <div v-if="htmlImpressum" v-html="htmlImpressum"></div>
                  <div v-else class="title">
                    <strong>
                      Hey, this is the default impressum.
                      Not much to see here.
                      <p>
                        You can change me by providing a file called "Impressum.html"
                        at the root of this domain (which is
                        <a
                          :href="impressumLocation"
                        >{{ impressumLocation }}</a>).
                      </p>
                      <p>
                        This should be quite easy, as you can place it into the "dist" folder
                        this frontend is currently being served from.
                      </p>
                      <p>
                        You can use
                        <a
                          href="https://vuetifyjs.com/en/styles/typography#typography"
                        >Vuetify html classes</a> for formatting me, or just plain HTML with style tags.
                      </p>
                    </strong>
                  </div>
                </v-row>
              </v-container>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import axios from 'axios'

@Component
export default class About extends Vue {
  private htmlImpressum: string | null = null

  get impressumLocation() {
    return (
      document.location.protocol +
      '//' +
      document.location.host +
      '/Impressum.html'
    )
  }

  async mounted() {
    axios
      .get(this.impressumLocation, { hideFromSnackbar: true })
      .then(response => {
        axios
          .get(this.impressumLocation + 'random', { hideFromSnackbar: true })
          .then(randomResponse => {
            if (
              response.status === 200 &&
              response.data !== randomResponse.data
            ) {
              this.htmlImpressum = response.data
            }
          })
          .catch(() => (this.htmlImpressum = response.data))
      })
  }
}
</script>
