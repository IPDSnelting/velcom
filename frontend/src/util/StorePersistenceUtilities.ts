import { DetailDataPoint, Dimension } from '@/store/types'
import { CustomKeyEqualsMap } from '@/util/CustomKeyEqualsMap'
import {
  DetailGraphStore,
  DimensionDetailPoint
} from '@/store/modules/detailGraphStore'
import { ComparisonGraphStore } from '@/store/modules/comparisonGraphStore'

// <editor-fold desc="DIMENSION">
function hydrateDimension(it: Dimension) {
  return new Dimension(it.benchmark, it.metric, it.unit, it.interpretation)
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
    commitToCompare: serializeDimensionDetailPoint(store.commitToCompare)
  })
}
// </editor-fold>

// <editor-fold desc="COMPARISON GRAPH">
export function comparisonGraphStoreFromJson(json?: string): any {
  return json ? JSON.parse(json) : {}
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
    selectedBenchmark: store.selectedBenchmark
  })
}
// </editor-fold>
