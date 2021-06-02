import { action, createModule, mutation } from 'vuex-class-component'
import {
  Dimension,
  dimensionIdToString,
  RepoId,
  StatusComparisonPoint
} from '@/store/types'
import axios from 'axios'
import { statusComparisonPointFromJson } from '@/util/json/StatusComparisonJsonHelper'
import { formatDimensions, formatRepos } from '@/util/Texts'
import Vue from 'vue'
import { PermanentLinkOptions } from '@/store/modules/detailGraphStore'
import router from '@/router'
import { respectOptions } from '@/util/LinkUtils'
import { Route } from 'vue-router'
import { vxm } from '@/store'

const VxModule = createModule({
  namespaced: 'statusComparisonModule',
  strict: false
})

export class StatusComparisonStore extends VxModule {
  private selectedBranches: { [repoId: string]: string[] } = {}

  graph: StatusComparisonPoint[] = []
  baselineRepoId: string | null = null
  selectedDimensions: Dimension[] = []
  selectedDimensionSelector: 'tree' | 'matrix' = 'matrix'

  @action
  async fetch(): Promise<StatusComparisonPoint[]> {
    const repos = this.selectedBranchesMap

    // Nothing selected
    if (repos.size === 0) {
      this.graph = []
      return []
    }

    const response = await axios.get('/graph/status-comparison', {
      params: {
        repos: formatRepos(repos)
      }
    })

    this.graph = response.data.runs.map(statusComparisonPointFromJson)

    return this.graph
  }

  /**
   * Adjusts this store to the values defined in the permanent link.
   *
   * @param link the link to adjust to
   */
  @action
  async adjustToPermanentLink(link: Route): Promise<void> {
    if (link.query.dimensions && typeof link.query.dimensions === 'string') {
      const dimensionMap = new Map(
        vxm.repoModule
          .occuringDimensions(vxm.repoModule.allRepos.map(it => it.id))
          .map(dimension => [dimension.toString(), dimension])
      )

      const dimensionParts = link.query.dimensions.split('::')
      this.selectedDimensions = dimensionParts
        .flatMap(dimensionPart => {
          const [benchmark, ...metrics] = dimensionPart.split(':')

          return metrics.map(metric =>
            dimensionMap.get(dimensionIdToString({ metric, benchmark }))
          )
        })
        .filter(it => it)
        .map(it => it!)
    }

    if (link.query.repos && typeof link.query.repos === 'string') {
      const fullString = link.query.repos
      const repoParts = fullString.split('::')
      repoParts.forEach(repoPart => {
        const [repoId, ...branches] = repoPart.split(':')
        this.setSelectedBranchesForRepo({ repoId, branches })
      })
    }

    if (link.query.baseline && typeof link.query.baseline === 'string') {
      this.baselineRepoId = link.query.baseline
    }
  }

  @mutation
  setSelectedBranchesForRepo(payload: {
    repoId: RepoId
    branches: string[]
  }): void {
    Vue.set(this.selectedBranches, payload.repoId, payload.branches)
  }

  @mutation
  toggleRepoBranch(payload: { repoId: RepoId; branch: string }): void {
    let branches: string[] = this.selectedBranches[payload.repoId] || []

    if (branches.includes(payload.branch)) {
      branches = branches.filter(it => it !== payload.branch)
    } else {
      branches.push(payload.branch)
    }

    Vue.set(this.selectedBranches, payload.repoId, branches)
  }

  get selectedBranchesMap(): Map<RepoId, string[]> {
    const repos = new Map<RepoId, string[]>()

    Object.entries(this.selectedBranches).forEach(([repoId, branches]) => {
      if (branches.length > 0) {
        repos.set(repoId, branches)
      }
    })

    return repos
  }

  /**
   * Returns a permanent link to the current status comparison graph state
   */
  get permanentLink(): (options?: PermanentLinkOptions) => string {
    return options => {
      const route = router.resolve({
        name: 'repo-dimension-comparison',
        query: {
          repos: respectOptions(
            options,
            'includeDataRestrictions',
            formatRepos(this.selectedBranchesMap)
          ),
          baseline: respectOptions(
            options,
            'includeDataRestrictions',
            this.baselineRepoId || undefined
          ),
          dimensions: respectOptions(
            options,
            'includeDataRestrictions',
            formatDimensions(this.selectedDimensions)
          )
        }
      })

      return location.origin + route.href
    }
  }

  /**
   * Converts a given store to a pure object that can be serialized.
   *
   * @param store the store to convert
   */
  static toPlainObject(store: StatusComparisonStore): unknown {
    return {
      selectedBranches: store.selectedBranches,
      baselineRepoId: store.baselineRepoId,
      selectedDimensions: store.selectedDimensions,
      selectedDimensionSelector: store.selectedDimensionSelector
    }
  }
}
