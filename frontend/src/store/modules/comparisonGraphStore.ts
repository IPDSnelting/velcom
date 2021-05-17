import { action, createModule, mutation } from 'vuex-class-component'
import {
  AttributedDatapoint,
  ComparisonDataPoint,
  Dimension,
  RepoId
} from '@/store/types'
import Vue from 'vue'
import axios from 'axios'
import { comparisonDatapointFromJson } from '@/util/json/GraphJsonHelper'
import { vxm } from '@/store'
import router from '@/router'
import { PermanentLinkOptions } from '@/store/modules/detailGraphStore'
import {
  orElse,
  orUndefined,
  parseAndSetZoomAndDateRange,
  respectOptions
} from '@/util/LinkUtils'
import { Route } from 'vue-router'
import { roundDateDown, roundDateUp } from '@/util/TimeUtil'

const VxModule = createModule({
  namespaced: 'comparisonGraphModule',
  strict: false
})

function defaultStartDate() {
  // FIXME: 7 not 14
  // One week in the past
  return new Date(
    new Date(new Date().setDate(new Date().getDate() - 14)).setHours(0, 0, 0, 0)
  )
}

function defaultEndDate() {
  // Today at midnight / start of tomorrow
  return new Date(new Date().setHours(24, 0, 0, 0))
}

function formatRepos(repos: Map<RepoId, string[]>): string {
  return Array.from(repos.entries())
    .filter(([, branches]) => branches.length > 0)
    .map(([repoId, branches]) => {
      return repoId + ':' + branches.join(':')
    })
    .join('::')
}

export class ComparisonGraphStore extends VxModule {
  private _selectedBranches: { [id: string]: string[] } = {}

  startTime: Date = defaultStartDate()
  endTime: Date = defaultEndDate()

  selectedDimension: Dimension | null = null
  dayEquidistantGraphSelected: boolean = true
  beginYAtZero: boolean = false

  zoomXStartValue: number | null = null
  zoomXEndValue: number | null = null
  zoomYStartValue: number | null = null
  zoomYEndValue: number | null = null

  referenceDatapoint: AttributedDatapoint | null = null

  @action
  async fetchComparisonGraph(): Promise<ComparisonDataPoint[]> {
    if (this.selectedDimension === null) {
      return []
    }

    const adjustedStartDate = roundDateDown(this.startTime).getTime() / 1000
    const adjustedEndDate = roundDateUp(this.endTime).getTime() / 1000

    const response = await axios.get('/graph/comparison', {
      params: {
        start_time: adjustedStartDate,
        end_time: adjustedEndDate,
        dimension:
          this.selectedDimension.benchmark +
          ':' +
          this.selectedDimension.metric,
        repos: formatRepos(this.selectedBranches)
      }
    })

    return response.data.repos.flatMap((repo: any) =>
      comparisonDatapointFromJson(this.selectedDimension!, repo)
    )
  }

  /**
   * Adjusts this store to the values defined in the permanent link.
   *
   * @param link the link to adjust to
   */
  @action
  async adjustToPermanentLink(link: Route): Promise<void> {
    parseAndSetZoomAndDateRange(link, this)

    if (link.query.dayEquidistant === 'true') {
      this.dayEquidistantGraphSelected = true
    }

    if (link.query.dimension && typeof link.query.dimension === 'string') {
      const [benchmark, metric] = link.query.dimension.split(':')
      const possibleDimensions = vxm.repoModule.allRepos
        .flatMap(it => it.dimensions)
        .filter(it => it.benchmark === benchmark && it.metric === metric)

      if (possibleDimensions.length > 0) {
        this.selectedDimension = possibleDimensions[0]
      }
    }

    if (link.query.repos && typeof link.query.repos === 'string') {
      const fullString = link.query.repos
      const repoParts = fullString.split('::')
      repoParts.forEach(repoPart => {
        const [repoId, ...branches] = repoPart.split(':')
        this.setSelectedBranchesForRepo({ repoId, branches })
      })
    }
  }

  @mutation
  setSelectedBranchesForRepo(payload: {
    repoId: RepoId
    branches: string[]
  }): void {
    Vue.set(this._selectedBranches, payload.repoId, payload.branches)
  }

  @mutation
  toggleRepoBranch(payload: { repoId: RepoId; branch: string }): void {
    let branches: string[] = this._selectedBranches[payload.repoId] || []

    if (branches.includes(payload.branch)) {
      branches = branches.filter(it => it !== payload.branch)
    } else {
      branches.push(payload.branch)
    }

    Vue.set(this._selectedBranches, payload.repoId, branches)
  }

  /**
   * Returns a permanent link to the current detail graph state
   */
  get permanentLink(): (options?: PermanentLinkOptions) => string {
    return options => {
      const route = router.resolve({
        name: 'repo-comparison',
        query: {
          zoomYStart: respectOptions(
            options,
            'includeYZoom',
            orUndefined(this.zoomYStartValue)
          ),
          zoomYEnd: respectOptions(
            options,
            'includeYZoom',
            orUndefined(this.zoomYEndValue)
          ),
          zoomXStart:
            options && options.includeXZoom
              ? orElse(this.zoomXStartValue, this.startTime.getTime())
              : orUndefined(this.startTime.getTime()),
          zoomXEnd:
            options && options.includeXZoom
              ? orElse(this.zoomXEndValue, this.endTime.getTime())
              : orUndefined(this.endTime.getTime()),
          repos: respectOptions(
            options,
            'includeDataRestrictions',
            formatRepos(this.selectedBranches)
          ),
          dimension: respectOptions(
            options,
            'includeDataRestrictions',
            this.selectedDimension
              ? this.selectedDimension.benchmark +
                  ':' +
                  this.selectedDimension.metric
              : undefined
          ),
          dayEquidistant: this.dayEquidistantGraphSelected ? 'true' : undefined
        }
      })

      return location.origin + route.href
    }
  }

  get selectedBranchesForRepo(): (repoId: RepoId) => string[] {
    return repoId => this._selectedBranches[repoId] || []
  }

  get selectedBranches(): Map<RepoId, string[]> {
    const map: Map<RepoId, string[]> = new Map()
    Object.keys(this._selectedBranches)
      // Repos might not exist anymore, as the selected branches are persisted
      .filter(id => vxm.repoModule.repoById(id) !== undefined)
      .forEach(repoId => {
        const branches = this._selectedBranches[repoId]
        if (branches && branches.length > 0) {
          map.set(repoId, branches)
        }
      })
    return map
  }

  /**
   * Converts a given store to a pure object that can be serialized.
   *
   * @param store the store to convert
   */
  static toPlainObject(store: ComparisonGraphStore): unknown {
    return {
      _selectedBranches: store._selectedBranches,
      startTime: store.startTime,
      endTime: store.endTime,
      selectedDimension: store.selectedDimension,
      zoomXStartValue: store.zoomXStartValue,
      zoomXEndValue: store.zoomXEndValue,
      zoomYStartValue: store.zoomYStartValue,
      zoomYEndValue: store.zoomYEndValue,
      referenceDatapoint: store.referenceDatapoint,
      beginYAtZero: store.beginYAtZero,
      dayEquidistantGraphSelected: store.dayEquidistantGraphSelected
    }
  }
}
