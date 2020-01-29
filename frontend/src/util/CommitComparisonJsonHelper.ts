import {
  Difference,
  MeasurementID,
  Run,
  Measurement,
  Commit,
  CommitComparison,
  CommitInfo
} from '@/store/types'

/**
 * Parses a json commit to a Commit object.
 *
 * @export
 * @param {*} jsonCommit the json commit
 * @returns {Commit} the commit object
 */
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

export function commitDetailFromJson(jsonCommitInfo: any): CommitInfo {
  let comparison = comparisonFromJson(jsonCommitInfo.comparison)
  let nextCommit = jsonCommitInfo.next
    ? commitFromJson(jsonCommitInfo.next)
    : null
  return new CommitInfo(comparison, nextCommit)
}

export function comparisonFromJson(jsonComparison: any): CommitComparison {
  let firstRun: Run | null = jsonComparison.first_run
    ? runFromJson(jsonComparison.first_run)
    : null
  let secondRun: Run | null = jsonComparison.second_run
    ? runFromJson(jsonComparison.second_run)
    : null
  let differences: Difference[] = jsonComparison.differences.map((it: any) =>
    differenceFromJson(it)
  )
  let firstCommit: Commit | null = jsonComparison.first_commit
    ? commitFromJson(jsonComparison.first_commit)
    : null
  let secondCommit: Commit | null = commitFromJson(jsonComparison.second_commit)

  return new CommitComparison(
    firstRun,
    secondRun,
    firstCommit,
    secondCommit,
    differences
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
