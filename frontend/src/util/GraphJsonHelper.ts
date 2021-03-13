/* eslint-disable @typescript-eslint/explicit-module-boundary-types */
import {
  DetailDataPoint,
  DimensionId,
  GraphDataPointValue,
  dimensionIdEqual,
  RepoId,
  ComparisonDataPoint,
  Dimension
} from '@/store/types'
import { CustomKeyEqualsMap } from '@/util/CustomKeyEqualsMap'

/**
 * Parses a data point json to a DataPoint object.
 *
 * @export
 * @param {*} json the json object
 * @param {*} dimensions the requested dimensions in the same order they appear in the values array of the datapoint
 * @returns {DetailDataPoint} the data point object
 */
export function detailDataPointFromJson(
  json: any,
  dimensions: DimensionId[]
): DetailDataPoint {
  const map: CustomKeyEqualsMap<
    DimensionId,
    GraphDataPointValue
  > = new CustomKeyEqualsMap([], dimensionIdEqual)
  for (let i = 0; i < dimensions.length; i++) {
    map.set(dimensions[i], graphDataPointValueFromJson(json.values[i]))
  }

  const committerDate = new Date(json.committer_date * 1000)
  return new DetailDataPoint(
    json.hash,
    json.parents,
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
  json: any
): ComparisonDataPoint[] {
  const repoId: RepoId = json.repo_id

  return json.commits.map((commit: any) => {
    const valueMap = new Map()
    valueMap.set(repoId, graphDataPointValueFromJson(commit.value))
    return new ComparisonDataPoint(
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
