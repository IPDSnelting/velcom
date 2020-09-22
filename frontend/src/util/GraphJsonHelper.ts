/* eslint-disable @typescript-eslint/explicit-module-boundary-types */
import {
  DetailDataPoint,
  ComparisonDataPoint,
  RepoId,
  DimensionId,
  DetailDataPointValue
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
    DetailDataPointValue
  > = new CustomKeyEqualsMap(
    [],
    (first, second) =>
      first.benchmark === second.benchmark && first.metric === second.metric
  )
  for (let i = 0; i < dimensions.length; i++) {
    map.set(dimensions[i], detailDataPointValueFromJson(json.values[i]))
  }
  return new DetailDataPoint(
    json.hash,
    json.parents,
    json.author,
    new Date(json.author_date),
    json.summary,
    map
  )
}

function detailDataPointValueFromJson(
  jsonValue: string | number
): DetailDataPointValue {
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

/**
 * Parses a comparison graph data point json to a DataPoint object.
 *
 * @export
 * @param {*} json the json object
 * @param repoId the id of the repo the datapoint is from
 * @returns {ComparisonDataPoint} the data point object
 */
export function comparisonDataPointFromJson(
  json: any,
  repoId: RepoId
): ComparisonDataPoint {
  return new ComparisonDataPoint(
    json.hash,
    json.author,
    new Date(json.author_date * 1000),
    json.summary,
    json.value,
    repoId
  )
}
