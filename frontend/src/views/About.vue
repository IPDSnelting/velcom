<template>
  <div class="about">
    <v-container>
      <v-row>
        <v-col>
          <v-card>
            <v-card-title>
              <v-toolbar color="toolbarColor" dark>
                <v-row no-gutters align="center" justify="space-between">
                  <v-col cols="12" md="auto"> About VelCom </v-col>
                  <v-col cols="auto">
                    <span class="small-text">Version: {{ gitHash }}</span>
                  </v-col>
                </v-row>
              </v-toolbar>
            </v-card-title>
            <v-card-text>
              <v-container fluid class="ma-0 pa-0">
                <v-row align="baseline" justify="center" no-gutters>
                  <v-col>
                    <v-row no-gutters align="baseline" justify="center">
                      <v-col class="shapeshifter-container" cols="auto">
                        <canvas
                          id="myCanvas"
                          @click="flutterByMyButterfly"
                        ></canvas>
                        <div
                          id="shapeshifter-logo"
                          class="shapeshifter play"
                          @click="flutterByMyButterfly"
                        ></div>
                      </v-col>
                    </v-row>
                    <v-row align="center" justify="center">
                      <p class="text-center">
                        "The name stands for 'Velocity Commit', and velocity is
                        related to speed and performance.
                        <br />It also sounds like a German saying 'welcome',
                        which is nice and friendly."
                      </p>
                    </v-row>
                    <v-row align="center" justify="center" class="mt-10">
                      <p class="text-center">
                        VelCom is a project which was realized as part of the
                        lecture "Software Engineering Practice" at the Karlsruhe
                        Institute of Technology (KIT) during winter semester
                        2019/20.
                        <br />The goal was to create a website that...
                      </p>
                    </v-row>
                    <v-row align="center" justify="center">
                      <ul>
                        <li>benchmarks software in the long term</li>
                        <li>
                          manages repositories and commits in an intelligent
                          manner
                        </li>
                        <li>provides appealing visualisations of benchmarks</li>
                        <li>is easy to set up</li>
                      </ul>
                    </v-row>
                    <v-row align="center" justify="center" class="mt-10">
                      <p>
                        If you are interested in learning more about the maniacs
                        behind VelCom, please visit
                        <a href="https://www.aaaaaaah.de/">AAAAAAAH!</a>
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
              <v-toolbar color="toolbarColor" dark>Impressum</v-toolbar>
            </v-card-title>
            <v-card-text>
              <v-container fluid>
                <v-row align="baseline" justify="center">
                  <div v-if="htmlImpressum" v-html="htmlImpressum"></div>
                  <div v-else class="title">
                    Hey, this is the default impressum. Not much to see here.
                    <p>
                      You can change me by providing a file called
                      "Impressum.html" at the root of this domain (which is
                      <a :href="impressumLocation">{{ impressumLocation }}</a
                      >).
                    </p>
                    <p>
                      This should be quite easy, as you can place it into the
                      "dist" folder this frontend is currently being served
                      from.
                    </p>
                    <p>
                      You can use
                      <a
                        href="https://vuetifyjs.com/en/styles/typography#typography"
                        >Vuetify html classes</a
                      >
                      for formatting me, or just plain HTML with style tags.
                    </p>
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
  private fluttering: boolean = false

  private relativeUrlToBase(path: string) {
    return document.location.protocol + '//' + document.location.host + path
  }

  private get gitHash() {
    const fullHash: string = process.env.__COMMIT_HASH__
    // Only show the first few characters on very small screens - they are probably unique enough
    if (this.$vuetify.breakpoint.xs) {
      return fullHash.substr(0, 10)
    }
    return fullHash
  }

  private get impressumLocation() {
    return this.relativeUrlToBase('/Impressum.html')
  }

  private flutterByMyButterfly() {
    const element = document.getElementById('shapeshifter-logo')!

    const canvas = document.getElementById('myCanvas')! as HTMLCanvasElement
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
    const context = canvas.getContext('2d')!
    context.fillStyle = '#989cff'

    if (this.fluttering) {
      element.classList.remove('flutterByMyButterfly')
      this.fluttering = false
      context.clearRect(0, 0, canvas.width, canvas.height)
      return
    }
    this.fluttering = true
    element.classList.add('flutterByMyButterfly')

    const elementWidthOffset = element.getBoundingClientRect().width / 2
    let elementHeightOffset = element.getBoundingClientRect().height / 2
    elementHeightOffset += elementHeightOffset / 4 + 25

    // https://en.wikipedia.org/wiki/Lemniscate_of_Bernoulli#Equations

    const { y: yMin } = this.evalLemniscate(Math.PI / 2, 0)

    const { y: yMax } = this.evalLemniscate(Math.PI / 2, Math.PI)

    const height = Math.abs(yMax - yMin)

    // let theta = 5.39317816
    let theta = Math.PI / 2
    let time = 4.94277244164
    const callback = () => {
      if (!element.classList.contains('flutterByMyButterfly')) {
        return
      }
      const xOffset = window.innerWidth / 2 - elementWidthOffset
      const yOffset = -elementHeightOffset / 2 + height / 2

      let { x, y } = this.evalLemniscate(theta, time)
      x += xOffset
      y += yOffset

      for (let i = 0; i < Math.PI / 4; i += (Math.PI * 2) / 20) {
        const { x, y } = this.evalLemniscate(theta - i / 2, time - i)
        context.fillRect(
          x + xOffset + elementWidthOffset,
          y + yOffset + elementHeightOffset,
          2,
          2
        )
      }

      element.style.top = y + 'px'
      element.style.left = x + 'px'
      requestAnimationFrame(callback)

      time += (Math.PI * 2) / 60 / 10
      theta += (Math.PI * 2) / 60 / 100
    }

    requestAnimationFrame(callback)
  }

  private evalLemniscate(
    theta: number,
    time: number
  ): { x: number; y: number } {
    const sqrt2 = Math.sqrt(2)

    const a = Math.min(window.innerWidth, window.innerHeight) / 4

    const sint = Math.sin(time)
    const cost = Math.cos(time)
    const denominator = sint * sint + 1
    let x = (a * sqrt2 * cost) / denominator
    let y = (a * sqrt2 * cost * sint) / denominator

    const baseAngle = Math.atan2(y, x)
    const baseDistance = Math.sqrt(x * x + y * y)

    x = Math.cos(baseAngle + theta) * baseDistance

    y = Math.sin(baseAngle + theta) * baseDistance

    return { x: x, y: y }
  }

  async mounted(): Promise<void> {
    axios
      .get(this.impressumLocation, { hideFromSnackbar: true })
      .then(response => {
        axios
          .get(
            this.relativeUrlToBase(
              '/hello I am a random url that is hopefully not mapped'
            ),
            { hideFromSnackbar: true }
          )
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

<style scoped>
@keyframes play72 {
  0% {
    background-position: 0 0;
  }
  100% {
    background-position: -15264px 0;
  }
}
.shapeshifter-container {
  width: 212px;
  height: 212px;
}
#myCanvas {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 50; /* NOTHING is more important! */
  pointer-events: none;
}
.shapeshifter {
  animation-duration: 1200ms;
  animation-timing-function: steps(72);
  width: 212px;
  height: 212px;
  background-image: url(../assets/flutter.svg);
  background-repeat: no-repeat;
}
.shapeshifter.play {
  animation-name: play72;
  animation-iteration-count: infinite;
  -moz-animation-iteration-count: infinite;
  -webkit-animation-iteration-count: infinite;
  -o-animation-iteration-count: infinite;
}
.flutterByMyButterfly {
  position: fixed;
  z-index: 100; /* NOTHING is more important! */
  animation-duration: 600ms;
}

.small-text {
  font-size: 1rem !important;
  font-weight: 400 !important;
  line-height: 1.5rem !important;
  letter-spacing: 0.03125em !important;
}
</style>
