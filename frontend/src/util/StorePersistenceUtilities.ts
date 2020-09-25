import {
  ComparisonDataPoint,
  DetailDataPoint,
  Dimension,
  DimensionId,
  dimensionIdEqual,
  Repo,
  RepoBranch
} from '@/store/types'
import { CustomKeyEqualsMap } from '@/util/CustomKeyEqualsMap'
import {
  DetailGraphStore,
  DimensionDetailPoint
} from '@/store/modules/detailGraphStore'
import { ComparisonGraphStore } from '@/store/modules/comparisonGraphStore'
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
  return new RepoBranch(it.name, it.tracked)
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
function serializeDimensionDetailPoint(point: DimensionDetailPoint | null) {
  if (!point) {
    return null
  }
  const persistablePoint: any = Object.assign({}, point.dataPoint)
  persistablePoint.values = Array.from(persistablePoint.values.entries())
  return {
    dataPoint: persistablePoint,
    dimension: point.dimension
  }
}

function deserializeDimensionDetailPoint(
  point: DimensionDetailPoint | null
): DimensionDetailPoint | null {
  if (!point) {
    return null
  }
  return {
    dimension: hydrateDimension(point.dimension),
    dataPoint: hydrateDetailPoint(point.dataPoint)
  }
}
// </editor-fold>

// <editor-fold desc="DETAIL DATA POINT">
function hydrateDetailPoint(it: DetailDataPoint) {
  return new DetailDataPoint(
    it.hash,
    it.parents,
    it.author,
    new Date(it.authorDate),
    it.summary,
    new CustomKeyEqualsMap(
      it.values,
      (first, second) =>
        first.benchmark === second.benchmark && first.metric === second.metric
    )
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
    zoomStartValue: store.zoomStartValue,
    zoomEndValue: store.zoomEndValue,
    firstFreeColorIndex: (store as any).firstFreeColorIndex,
    colorIndexMap: Array.from((store as any).colorIndexMap.entries()),
    commitToCompare: serializeDimensionDetailPoint(store.commitToCompare)
  })
}
// </editor-fold>

// <editor-fold desc="COMPARISON DATA POINT">
function comparisonDataPointToJson(point: ComparisonDataPoint | null) {
  if (!point) {
    return undefined
  }
  return JSON.stringify(point)
}

function hydrateComparisonDataPoint(point: ComparisonDataPoint) {
  return new ComparisonDataPoint(
    point.hash,
    point.author,
    new Date(point.authorDate),
    point.summary,
    point.value,
    point.repoId
  )
}
// </editor-fold>

// <editor-fold desc="COMPARISON GRAPH">
export function comparisonGraphStoreFromJson(json?: string): any {
  if (!json) {
    return {}
  }
  const parsed = JSON.parse(json)

  if (parsed.referenceCommit) {
    parsed.referenceCommit = hydrateComparisonDataPoint(parsed.referenceCommit)
  }

  return parsed
}

export function comparisonGraphStoreToJson(
  store: ComparisonGraphStore
): string {
  return JSON.stringify({
    _selectedRepos: (store as any)._selectedRepos,
    _selectedBranchesByRepoId: (store as any)._selectedBranchesByRepoId,
    startTime: (store as any).startTime,
    stopTime: (store as any).stopTime,
    selectedMetric: store.selectedMetric,
    selectedBenchmark: store.selectedBenchmark,
    referenceCommit: comparisonDataPointToJson(store.referenceCommit)
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
