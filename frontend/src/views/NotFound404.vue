<template>
  <v-container fluid class="wrapper">
    <v-card class="ma-5">
      <v-card-title class="centered">
        <span class="text-404">{{ title }}</span>
      </v-card-title>
      <v-card-subtitle class="centered">
        <span class="subtitle">{{ subtitle }}</span>
        <span class="subtitle wroom">*Wroooom*</span>
      </v-card-subtitle>
      <v-card-text class="mt-10">
        <v-row align="center" justify="center">
          <v-col cols="auto">
            <img src="@/assets/404.png" alt="404 - not found" />
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'

@Component
export default class NotFound404 extends Vue {
  @Prop({ default: '404' })
  private title!: string

  @Prop({ default: 'Not found :/' })
  private subtitle!: string
}
</script>

<style scoped>
.wrapper {
  flex: 1 1 auto;
}
.wrapper > * {
  height: calc(100% - 20 * 2px);
}

.centered {
  display: flex;
  justify-content: center;
  flex-direction: column;
}

.text-404 {
  font-size: 10rem;
  font-style: oblique;
  font-weight: lighter;
  line-height: 1;
}
.subtitle {
  text-align: center;
  font-size: 5rem;
  font-style: oblique;
  font-weight: lighter;
  line-height: 1;
}
.wroom {
  font-size: 3rem;
  margin-top: 16px;
  font-variant: small-caps;
  letter-spacing: 10px;
}
</style>

<style>
.v-content__wrap {
  display: flex;
  flex-direction: column;
}
</style>
