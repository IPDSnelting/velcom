import { action, createModule } from 'vuex-class-component'
import { StatusComparisonPoint } from '@/store/types'
import axios from 'axios'
import { statusComparisonPointFromJson } from '@/util/json/StatusComparisonJsonHelper'

const VxModule = createModule({
  namespaced: 'statusComparisonModule',
  strict: false
})

export class StatusComparisonStore extends VxModule {
  graph: StatusComparisonPoint[] = []

  baselineRepoId: string | null = null

  @action
  async fetch(): Promise<StatusComparisonPoint[]> {
    const response = await axios.get('/graph/status-comparison', {
      params: {}
    })

    this.graph = response.data.runs.map(statusComparisonPointFromJson)

    return this.graph
  }
}
