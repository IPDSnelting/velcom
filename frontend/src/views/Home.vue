<template>
  <div class="home">
    <v-container>
      <v-row>
        <v-col>
          <v-card>
            <v-card-title>
              <v-toolbar color="toolbarColor" dark>
                Recent Significant Runs
              </v-toolbar>
            </v-card-title>
            <v-card-text>
              <v-container fluid>
                <v-row align="baseline" justify="center">
                  <v-col class="mx-0 px-0">
                    <multiple-run-overview
                      :runs="recentSignificant"
                    ></multiple-run-overview>
                  </v-col>
                </v-row>
                <v-row align="baseline" justify="end">
                  <v-col cols="auto">
                    <v-btn
                      text
                      color="primary"
                      @click="recentSignificantAmount += 5"
                    >
                      load more
                    </v-btn>
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
              <v-toolbar color="toolbarColor" dark>Recent Runs</v-toolbar>
            </v-card-title>
            <v-card-text>
              <v-container fluid>
                <v-row align="baseline" justify="center">
                  <v-col class="mx-0 px-0">
                    <multiple-run-overview
                      :runs="recent"
                    ></multiple-run-overview>
                  </v-col>
                </v-row>
                <v-row align="baseline" justify="end">
                  <v-col cols="auto">
                    <v-btn text color="primary" @click="recentAmount += 10">
                      load more
                    </v-btn>
                  </v-col>
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
import { Watch } from 'vue-property-decorator'
import { vxm } from '@/store'
import { RunDescriptionWithDifferences, RunDescription } from '@/store/types'
import MultipleRunOverview from '@/components/overviews/MultipleRunOverview.vue'

@Component({
  components: {
    'multiple-run-overview': MultipleRunOverview
  }
})
export default class Home extends Vue {
  private recentAmount: number = 10
  private recentSignificantAmount: number = 10

  get recent(): RunDescription[] {
    return vxm.newsModule.recentRuns
  }

  get recentSignificant(): RunDescriptionWithDifferences[] {
    return vxm.newsModule.recentSignificantRuns
  }

  @Watch('recentAmount')
  private fetchRecent() {
    vxm.newsModule.fetchRecentRuns(this.recentAmount)
  }

  @Watch('recentSignificantAmount')
  private fetchRecentSignificant() {
    vxm.newsModule.fetchRecentSignificantRuns(this.recentSignificantAmount)
  }

  created(): void {
    this.fetchRecent()
    this.fetchRecentSignificant()
  }
}
</script>
