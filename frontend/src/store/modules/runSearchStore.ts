import { action, createModule } from 'vuex-class-component'
import axios from 'axios'
import { RepoId, SearchItem } from '@/store/types'
import { searchItemsFromJson } from '@/util/json/RunSearchJsonHelper'

const VxModule = createModule({
  namespaced: 'runSearchModule',
  strict: false
})

export type RunQuery = {
  limit?: number
  repoId?: RepoId
  query: string
}

export class RunSearchStore extends VxModule {
  @action
  public async searchRun(query: RunQuery): Promise<SearchItem[]> {
    const response = await axios.get('/search', {
      hideFromSnackbar: true,
      params: {
        limit: query.limit || 100,
        repo_id: query.repoId,
        query: query.query
      }
    })

    return searchItemsFromJson(response.data)
  }
}
