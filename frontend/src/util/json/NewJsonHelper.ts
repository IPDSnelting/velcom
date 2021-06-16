/* eslint-disable @typescript-eslint/explicit-module-boundary-types */
import { RunDescriptionWithDifferences } from '@/store/types'
import {
  differenceFromJson,
  runDescriptionFromJson
} from '@/util/json/CommitComparisonJsonHelper'
import { dimensionFromJson } from '@/util/json/RepoJsonHelper'

export function runDescriptionWithDifferencesFromJson(
  json: any
): RunDescriptionWithDifferences {
  return new RunDescriptionWithDifferences(
    runDescriptionFromJson(json.run),
    (json.significant_differences || []).map(differenceFromJson),
    (json.significant_failed_dimensions || []).map(dimensionFromJson)
  )
}
