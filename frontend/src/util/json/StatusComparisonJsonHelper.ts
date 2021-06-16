/* eslint-disable @typescript-eslint/explicit-module-boundary-types */
import { StatusComparisonPoint } from '@/store/types'
import { runFromJson } from '@/util/json/CommitComparisonJsonHelper'

export function statusComparisonPointFromJson(
  json: any
): StatusComparisonPoint {
  return new StatusComparisonPoint(
    json.repo_id,
    json.run ? runFromJson(json.run) : undefined,
    json.commit_hash
  )
}
