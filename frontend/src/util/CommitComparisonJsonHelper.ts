import {
  Run,
  RunResult,
  RunResultScriptError,
  RunResultVelcomError,
  RunResultSuccess,
  MeasurementError,
  MeasurementSuccess,
  Measurement,
  Commit
} from '@/store/types'
import { sourceFromJson } from '@/util/QueueJsonHelper'
import { dimensionFromJson } from '@/util/RepoJsonHelper'

export function runFromJson(json: any): Run {
  return new Run(
    json.id,
    json.author,
    json.runnerName,
    json.runnerInfo,
    new Date(json.startTime * 1000),
    new Date(json.stopTime * 1000),
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

export function commitFromJson(json: any): Commit {
  return new Commit(
    json.repo_id,
    json.hash,
    json.author,
    new Date(json.author_date * 1000),
    json.committer,
    new Date(json.committer_date * 1000),
    json.message,
    json.summary,
    json.runs.map(runFromJson)
  )
}
