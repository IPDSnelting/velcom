<template>
  <v-container fluid>
    <v-row align="center" justify="center">
      <v-col>
        <div id="chart" :style="{ height: '500px' }"></div>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import { Prop, Watch } from 'vue-property-decorator'
import Dygraph from 'dygraphs'
import {
  Commit,
  CommitComparison,
  Datapoint,
  Measurement,
  MeasurementID
} from '../../store/types'
import { crosshairIcon } from '../graphs/crosshairIcon'
import { vxm } from '../../store'
import { formatDateUTC } from '../../util/TimeUtil'
import 'dygraphs/dist/dygraph.css'

type CommitInfo = {
  commit: Commit
  comparison: CommitComparison
  measurementId: MeasurementID
}

@Component({})
export default class DytailGraph extends Vue {
  @Prop({})
  measurements!: MeasurementID[]

  @Prop({ default: true })
  beginYAtZero!: boolean

  private g!: Dygraph

  private height: number = 500

  private get datapoints(): CommitInfo[] {
    return vxm.detailGraphModule.repoHistory
      .slice()
      .reverse()
      .flatMap(it => {
        return this.measurements.map(measurementId => {
          return {
            commit: it.commit,
            comparison: it.comparison,
            measurementId: measurementId
          }
        })
      })
  }

  // https://stackoverflow.com/questions/14446511/most-efficient-method-to-groupby-on-an-array-of-objects
  private groupBy<K, V>(list: K[], keyGetter: (key: K) => V) {
    const map = new Map()
    list.forEach(item => {
      const key = keyGetter(item)
      const collection = map.get(key)
      if (!collection) {
        map.set(key, [item])
      } else {
        collection.push(item)
      }
    })
    return map
  }
  private get groupedByMeasurement(): Map<string, CommitInfo[]> {
    return this.groupBy(this.datapoints, it => it.measurementId.toString())
  }

  // prettier-ignore
  private get wantedMeasurementForDatapoint(): (comparison: CommitComparison, measurementId: MeasurementID) => Measurement | undefined {
    return (comparison: CommitComparison, measurementId: MeasurementID) => {
      if (
        comparison.second &&
        comparison.second.measurements
      ) {
        let wantedMeasurement: Measurement | undefined =
          comparison.second.measurements.find(it => it.id.equals(measurementId))
        return wantedMeasurement
      }
      return undefined
    }
  }

  private datapointValue(datapoint: {
    commit: Commit
    comparison: CommitComparison
    measurementId: MeasurementID
  }): number | null {
    let wantedMeasurement = this.wantedMeasurementForDatapoint(
      datapoint.comparison,
      datapoint.measurementId
    )
    if (wantedMeasurement !== undefined && wantedMeasurement.value !== null) {
      return wantedMeasurement.value
    }
    return null
  }

  // retrieving and interpreting datapoints
  private get amount(): number {
    return Number.parseInt(vxm.detailGraphModule.selectedFetchAmount)
  }

  private unit(measurementId: MeasurementID): string | null {
    for (const datapoint of this.datapoints) {
      let wantedMeasurement = this.wantedMeasurementForDatapoint(
        datapoint.comparison,
        measurementId
      )
      if (wantedMeasurement !== undefined && wantedMeasurement.unit) {
        return wantedMeasurement.unit
      }
    }
    return null
  }

  private firstSuccessful(measurementId: MeasurementID): number {
    for (const datapoint of this.datapoints) {
      let wantedMeasurement = this.wantedMeasurementForDatapoint(
        datapoint.comparison,
        measurementId
      )
      if (
        wantedMeasurement !== undefined &&
        wantedMeasurement.successful &&
        wantedMeasurement.value
      ) {
        return wantedMeasurement.value
      }
    }
    return this.height / 2
  }

  private get yLabel(): string {
    if (this.measurements.length === 1) {
      return this.unit(this.measurements[0])
        ? this.measurements[0].metric + ' in ' + this.unit(this.measurements[0])
        : this.measurements[0].metric
    }
    return ''
  }

  @Watch('datapoints')
  private up() {
    let data: any[] = []
    for (let i = 0; i < this.amount; i++) {
      data.push([i])
    }
    for (let [n, info] of this.groupedByMeasurement.entries()) {
      let lastValue = this.firstSuccessful(info[0].measurementId)
      info.forEach((p, index) => {
        let value =
          this.datapointValue(p) === null ? lastValue : this.datapointValue(p)
        lastValue = value!
        data[index].push(value)
      })
    }

    this.g.updateOptions({
      file: data,
      labels: ['x', ...Array.from(this.groupedByMeasurement.keys())]
    })
  }

  private mounted() {
    // empty string because div is not expected to be NULL
    let chartDiv = document.getElementById('chart') || ''

    this.g = new Dygraph(
      // containing div
      chartDiv,

      [[1, 1]],

      {
        axes: {
          x: {
            axisLabelFormatter: function(x: number | Date) {
              return x instanceof Date || x % 1 === 0 ? x : ''
            }
          }
        },
        ylabel: this.yLabel,
        legend: 'follow',
        labelsSeparateLines: true,
        connectSeparatedPoints: true,
        drawPoints: false,
        animatedZooms: true,
        panEdgeFraction: 0.00001
      }
    )

    this.up()
  }
}
</script>
