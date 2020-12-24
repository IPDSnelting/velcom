/* eslint-disable @typescript-eslint/explicit-module-boundary-types */
import {
  Commit,
  DimensionDifference,
  Measurement,
  MeasurementError,
  MeasurementSuccess,
  Run,
  RunComparison,
  RunDescription,
  RunResult,
  RunResultScriptError,
  RunResultSuccess,
  RunResultVelcomError,
  TrackedCommitDescription
} from '@/store/types'
import {
  commitDescriptionFromJson,
  sourceFromJson
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
    json.values,
    json.stddev,
    json.stddev_percent
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
  const toCommitDescription = (tracked: boolean) => {
    return (it: any) =>
      new TrackedCommitDescription(tracked, commitDescriptionFromJson(it))
  }

  const trackedChildren = json.tracked_children.map(toCommitDescription(true))
  const untrackedChildren = json.untracked_children.map(
    toCommitDescription(false)
  )
  const trackedParents = json.tracked_parents.map(toCommitDescription(true))
  const untrackedParents = json.untracked_parents.map(
    toCommitDescription(false)
  )

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
    trackedParents.concat(untrackedParents),
    trackedChildren.concat(untrackedChildren)
  )
}

export function differenceFromJson(json: any): DimensionDifference {
  return new DimensionDifference(
    dimensionFromJson(json.dimension),
    json.old_run_id,
    json.diff,
    json.reldiff,
    json.stddev_diff
  )
}

export function comparisonFromJson(json: any): RunComparison {
  return new RunComparison(
    runFromJson(json.run1),
    runFromJson(json.run2),
    json.differences.map(differenceFromJson)
  )
}
