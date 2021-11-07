/* eslint-disable @typescript-eslint/explicit-module-boundary-types */
import { CleanupDimension } from '@/store/types'
import { dimensionFromJson } from '@/util/json/RepoJsonHelper'

export function cleanupDimensionFromJson(cleanupDimension: any) {
  return new CleanupDimension(
    dimensionFromJson(cleanupDimension.dimension),
    cleanupDimension.runs,
    cleanupDimension.untracked_runs,
    cleanupDimension.unreachable_runs
  )
}
