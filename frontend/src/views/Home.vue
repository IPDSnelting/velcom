<template>
  <div class="home">
    <v-container>
      <v-row align="baseline" justify="center">
        <h1>Home</h1>
      </v-row>
      <v-row>
        <v-col>
          <v-card>
            <v-card-title>
              <v-toolbar color="primary darken-1" dark>Recent Significant Commits</v-toolbar>
            </v-card-title>
            <v-card-text>
              <v-container fluid>
                <v-row align="baseline" justify="center">
                  <multiple-run-overview :runs="recentSignificant"></multiple-run-overview>
                </v-row>
                <v-row align="baseline" justify="end">
                  <v-btn text color="primary" @click="recentSignificantAmount+=5">load more</v-btn>
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
              <v-toolbar color="primary darken-1" dark>Recently Benchmarked Commits</v-toolbar>
            </v-card-title>
            <v-card-text>
              <v-container fluid>
                <v-row align="baseline" justify="center">
                  <multiple-run-overview :runs="recent"></multiple-run-overview>
                </v-row>
                <v-row align="baseline" justify="end">
                  <v-btn text color="primary" @click="recentAmount+=10">load more</v-btn>
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
import { vxm } from '../store/index'
import { Run, Commit } from '@/store/types'
import MultipleRunOverview, {
  AttributedRun
} from '../components/overviews/MultipleRunOverview.vue'

@Component({
  components: {
    'multiple-run-overview': MultipleRunOverview
  }
})
export default class Home extends Vue {
  private recentAmount: number = 5
  private recentSignificantAmount = 10

  get recent(): AttributedRun[] {
    return vxm.newsModule.recentRuns
      .filter(it => it.second)
      .map(({ second, secondCommit }) => {
        return new AttributedRun(second!, secondCommit)
      })
  }

  get recentSignificant(): AttributedRun[] {
    return vxm.newsModule.recentSignificantRuns
      .filter(it => it.second)
      .map(({ second, secondCommit }) => {
        return new AttributedRun(second!, secondCommit)
      })
  }

  @Watch('recentAmount')
  fetchRecent() {
    vxm.newsModule.fetchRecentRuns(this.recentAmount)
  }

  @Watch('recentSignificantAmount')
  fetchRecentSignificant() {
    vxm.newsModule.fetchRecentSignificantRuns(this.recentSignificantAmount)
  }

  created() {
    this.fetchRecent()
    this.fetchRecentSignificant()
  }
}
</script>
