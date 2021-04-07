/* eslint-disable @typescript-eslint/explicit-module-boundary-types */
import {
  DetailDataPoint,
  GraphDataPointValue,
  RepoId,
  ComparisonDataPoint,
  Dimension,
  SeriesId
} from '@/store/types'

/**
 * Parses a data point json to a DataPoint object.
 *
 * @export
 * @param json the json object
 * @param seriesIds the requested series ids in the same order they appear in the values array of the datapoint
 * @param repoId the id of the repo the point belongs to
 * @returns {DetailDataPoint} the data point object
 */
export function detailDataPointFromJson(
  json: any,
  seriesIds: SeriesId[],
  repoId: RepoId
): DetailDataPoint {
  const map: Map<SeriesId, GraphDataPointValue> = new Map()
  for (let i = 0; i < seriesIds.length; i++) {
    map.set(seriesIds[i], graphDataPointValueFromJson(json.values[i]))
  }

  const committerDate = new Date(json.committer_date * 1000)
  return new DetailDataPoint(
    repoId,
    json.hash,
    json.parents.map((hash: any) => repoId + hash),
    json.author,
    committerDate,
    committerDate,
    json.summary,
    map
  )
}

function graphDataPointValueFromJson(
  jsonValue: string | number
): GraphDataPointValue {
  if (typeof jsonValue === 'number') {
    return jsonValue
  }
  switch (jsonValue) {
    case 'N':
      return 'NO_RUN'
    case 'O':
      return 'NO_MEASUREMENT'
    case 'R':
      return 'RUN_FAILED'
    case 'M':
      return 'MEASUREMENT_FAILED'
  }
  throw new Error(`Illegal type received: ${jsonValue}`)
}

export function comparisonDatapointFromJson(
  dimension: Dimension,
  repoJson: any
): ComparisonDataPoint[] {
  const repoId: RepoId = repoJson.repo_id

  return repoJson.commits.map((commit: any) => {
    const valueMap = new Map()
    valueMap.set(repoId, graphDataPointValueFromJson(commit.value))
    return new ComparisonDataPoint(
      new Date(commit.committer_date * 1000),
      new Date(commit.committer_date * 1000),
      commit.hash,
      repoId,
      valueMap,
      commit.parents.map((hash: string) => repoId + hash),
      commit.summary,
      commit.author
    )
  })
}
