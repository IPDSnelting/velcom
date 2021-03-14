import {
  AttributedDatapoint,
  DetailDataPoint,
  Dimension,
  DimensionId,
  dimensionIdEqual,
  Repo,
  RepoBranch
} from '@/store/types'
import { CustomKeyEqualsMap } from '@/util/CustomKeyEqualsMap'
import { DetailGraphStore } from '@/store/modules/detailGraphStore'
import { RepoStore } from '@/store/modules/repoStore'

// <editor-fold desc="DIMENSION">
function hydrateDimension(it: Dimension) {
  return new Dimension(it.benchmark, it.metric, it.unit, it.interpretation)
}
function hydrateDimensionId(it: DimensionId | Dimension) {
  if ((it as any).unit) {
    return hydrateDimension(it as Dimension)
  }
  return { metric: it.metric, benchmark: it.benchmark }
}
// </editor-fold>

// <editor-fold desc="REPO BRANCH">
function hydrateRepoBranch(it: RepoBranch) {
  return new RepoBranch(it.name, it.tracked, it.lastCommit)
}
// </editor-fold>

// <editor-fold desc="REPO">
function hydrateRepo(repo: Repo) {
  return new Repo(
    repo.id,
    repo.name,
    repo.branches.map(hydrateRepoBranch),
    repo.dimensions.map(hydrateDimension),
    repo.remoteURL,
    repo.hasToken
  )
}
// </editor-fold>

// <editor-fold desc="DIMENSION DETAIL POINT">
function serializeDimensionDetailPoint(point: AttributedDatapoint | null) {
  if (!point) {
    return null
  }
  const persistablePoint: any = Object.assign({}, point.datapoint)
  persistablePoint.values = Array.from(persistablePoint.values.entries())
  return {
    datapoint: persistablePoint,
    seriesId: point.seriesId
  } as AttributedDatapoint
}

function deserializeDimensionDetailPoint(
  point: AttributedDatapoint | null
): AttributedDatapoint | null {
  if (!point) {
    return null
  }
  return {
    seriesId: point.seriesId,
    datapoint: hydrateDetailPoint(point.datapoint as DetailDataPoint)
  }
}
// </editor-fold>

// <editor-fold desc="DETAIL DATA POINT">
function hydrateDetailPoint(it: DetailDataPoint) {
  return new DetailDataPoint(
    it.repoId,
    it.hash,
    it.parentUids,
    it.author,
    new Date(it.committerDate),
    new Date(it.committerDate),
    it.summary,
    new Map(it.values.entries())
  )
}
// </editor-fold>

// <editor-fold desc="DETAIL GRAPH">
export function detailGraphStoreFromJson(json?: string): any {
  if (!json) {
    return {}
  }

  const parsedUnsafe = JSON.parse(json)
  const parsed: DetailGraphStore = parsedUnsafe as DetailGraphStore
  // Convert flat json to real object
  parsedUnsafe._selectedDimensions = parsedUnsafe._selectedDimensions.map(
    hydrateDimension
  )
  parsed.referenceDatapoint = deserializeDimensionDetailPoint(
    parsed.referenceDatapoint
  )
  parsed.commitToCompare = deserializeDimensionDetailPoint(
    parsed.commitToCompare
  )
  parsed.startTime = new Date(parsed.startTime)
  parsed.endTime = new Date(parsed.endTime)
  parsedUnsafe.colorIndexMap = new CustomKeyEqualsMap(
    parsedUnsafe.colorIndexMap.map((entry: [DimensionId, number]) => [
      hydrateDimensionId(entry[0]),
      entry[1]
    ]),
    dimensionIdEqual
  )

  return parsed
}

export function detailGraphStoreToJson(store: DetailGraphStore): string {
  return JSON.stringify({
    _selectedRepoId: (store as any)._selectedRepoId,
    _selectedDimensions: (store as any)._selectedDimensions,
    referenceDatapoint: serializeDimensionDetailPoint(store.referenceDatapoint),
    startTime: store.startTime,
    endTime: store.endTime,
    duration: store.duration,
    zoomXStartValue: store.zoomXStartValue,
    zoomXEndValue: store.zoomXEndValue,
    zoomYStartValue: store.zoomYStartValue,
    zoomYEndValue: store.zoomYEndValue,
    firstFreeColorIndex: (store as any).firstFreeColorIndex,
    colorIndexMap: Array.from((store as any).colorIndexMap.entries()),
    commitToCompare: serializeDimensionDetailPoint(store.commitToCompare),
    beginYScaleAtZero: store.beginYScaleAtZero,
    dayEquidistantGraph: store.dayEquidistantGraph,
    selectedTab: store.selectedTab,
    selectedDimensionSelector: store.selectedDimensionSelector
  })
}
// </editor-fold>

// <editor-fold desc="REPO STORE">
export function repoStoreToJson(store: RepoStore): string {
  // I want to be a friend so I can see the private field :/
  // Or file-private visibility
  return JSON.stringify({
    repoIndex: (store as any).currentRepoIndex,
    repoIndices: (store as any).repoIndices,
    repos: (store as any).repos
  })
}

export function repoStoreFromJson(json?: string): any {
  if (!json) {
    return {}
  }
  const parsed = JSON.parse(json) as RepoStore
  const parsedUnsafe = parsed as any

  const repos: { [id: string]: Repo } = parsedUnsafe.repos

  Object.values(repos).forEach(
    repo => (parsedUnsafe.repos[repo.id] = hydrateRepo(repo))
  )

  return parsed
}
// </editor-fold>
