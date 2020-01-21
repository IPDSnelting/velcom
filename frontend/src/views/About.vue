<template>
  <div class="about">
    <v-container>
      <v-row>
        <v-col>
          <v-card>
            <v-card-title>
              <v-toolbar color="primary" dark>About VelCom</v-toolbar>
            </v-card-title>
            <v-card-text>
              <v-container fluid>
                <v-row align="baseline" justify="center">
                  <v-col>
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
                        <li>benchmarks software in the long trem</li>
                        <li>manages repositories and commits in an intelligent manner</li>
                        <li>provides appealing visualisations of benchmarks</li>
                        <li>is easy to set up</li>
                      </ul>
                    </v-row>
                    <v-row align="center" justify="center" class="mt-10">
                      <p>
                        If you are interested in learning more about the people behind VelCom, please visit
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
              <v-toolbar color="primary" dark>Impressum</v-toolbar>
            </v-card-title>
            <v-card-text>
              <v-container fluid>
                <v-row align="baseline" justify="center">...</v-row>
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

@Component
export default class About extends Vue {}
</script>
