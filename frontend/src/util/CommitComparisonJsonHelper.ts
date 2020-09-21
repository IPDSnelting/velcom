/* eslint-disable @typescript-eslint/explicit-module-boundary-types */
import {
  Run,
  RunResult,
  RunResultScriptError,
  RunResultVelcomError,
  RunResultSuccess,
  MeasurementError,
  MeasurementSuccess,
  Measurement,
  Commit,
  RunDescription,
  RunComparison,
  DimensionDifference
} from '@/store/types'
import {
  sourceFromJson,
  commitDescriptionFromJson
} from '@/util/QueueJsonHelper'
import { dimensionFromJson } from '@/util/RepoJsonHelper'

export function runFromJson(json: any): Run {
  return new Run(
    json.id,
    json.author,
    json.runner_name,
    json.runner_info,
    new Date(json.start_time * 1000),
    new Date(json.stop_time * 1000),
    sourceFromJson(json.source),
    resultFromJson(json.result)
  )
}

function resultFromJson(json: any): RunResult {
  if (json.bench_error !== undefined) {
    return new RunResultScriptError(json.bench_error)
  }
  if (json.velcom_error !== undefined) {
    return new RunResultVelcomError(json.velcom_error)
  }
  return new RunResultSuccess(json.measurements.map(measurementFromJson))
}

function measurementFromJson(json: any): Measurement {
  if (json.error !== undefined) {
    return new MeasurementError(dimensionFromJson(json.dimension), json.error)
  }
  return new MeasurementSuccess(
    dimensionFromJson(json.dimension),
    json.value,
    json.values
  )
}

export function runDescriptionFromJson(json: any): RunDescription {
  return new RunDescription(
    json.id,
    new Date(json.start_time * 1000),
    json.success,
    sourceFromJson(json.source)
  )
}

export function commitFromJson(json: any): Commit {
  return new Commit(
    json.repo_id,
    json.hash,
    json.author,
    new Date(json.author_date * 1000),
    json.committer,
    new Date(json.committer_date * 1000),
    json.message || '',
    json.summary,
    json.runs.map(runDescriptionFromJson),
    json.parents.map(commitDescriptionFromJson),
    json.children.map(commitDescriptionFromJson)
  )
}

export function differenceFromJson(json: any): DimensionDifference {
  return new DimensionDifference(
    dimensionFromJson(json.dimension),
    json.diff,
    json.reldiff,
    json.stddev
  )
}

export function comparisonFromJson(json: any): RunComparison {
  return new RunComparison(
    runFromJson(json.run1),
    runFromJson(json.run2),
    json.differences.map(differenceFromJson)
  )
}
