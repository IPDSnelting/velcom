import {
  Difference,
  MeasurementID,
  Run,
  Measurement,
  Commit
} from '@/store/types'

export function commitFromJson(jsonCommit: any): Commit {
  return new Commit(
    jsonCommit.repo_id,
    jsonCommit.hash,
    jsonCommit.author,
    jsonCommit.author_date,
    jsonCommit.committer,
    jsonCommit.committer_date,
    jsonCommit.message,
    jsonCommit.parents
  )
}

/**
 * Converts a run (comparison.first or comparison.second) json object to a Run object.
 *
 * @private
 * @param {*} jsonRun the received json Run
 * @returns {Run} the built run
 * @memberof RepoDetailStore
 */
export function runFromJson(jsonRun: any): Run {
  return new Run(
    commitFromJson(jsonRun.commit),
    jsonRun.start_time,
    jsonRun.stop_time,
    jsonRun.measurements
      ? measurementsFromJson(jsonRun.measurements)
      : undefined,
    jsonRun.error_message
  )
}

export function measurementsFromJson(jsonMeasurements: any[]): Measurement[] {
  return jsonMeasurements.reduce((accumulated: Measurement[], next) => {
    let id: MeasurementID = new MeasurementID(next.benchmark, next.metric)
    accumulated.push(
      new Measurement(
        id,
        next.unit,
        next.interpretation,
        next.values,
        next.value,
        next.error_message
      )
    )

    return accumulated
  }, [])
}

export function differenceFromJson(jsonDifference: any): Difference {
  return new Difference(
    new MeasurementID(jsonDifference.benchmark, jsonDifference.metric),
    jsonDifference.difference
  )
}
