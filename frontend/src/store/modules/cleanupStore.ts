import { createModule, action } from 'vuex-class-component'
import { CleanupDimension, Dimension } from '@/store/types'
import axios from 'axios'
import { cleanupDimensionFromJson } from '@/util/json/CleanupJsonHelper'

const VxModule = createModule({
  namespaced: 'cleanup',
  strict: false
})

export class CleanupStore extends VxModule {
  dimensions: CleanupDimension[] | null = null

  @action
  async fetchDimensions(): Promise<CleanupDimension[]> {
    const response = await axios.get('/all-dimensions')
    this.dimensions = response.data.dimensions.map(cleanupDimensionFromJson)
    return this.dimensions!
  }

  @action
  async deleteDimensions(dimensions: Dimension[]): Promise<void> {
    const dimensionIds = dimensions.map(it => ({
      metric: it.metric,
      benchmark: it.benchmark
    }))

    await axios.delete(`/dimensions`, { data: dimensionIds })
  }
}
