/* eslint-disable @typescript-eslint/explicit-module-boundary-types */
import {
  DetailDataPoint,
  ComparisonDataPoint,
  RepoId,
  DimensionId
} from '@/store/types'

/**
 * Parses a data point json to a DataPoint object.
 *
 * @export
 * @param {*} json the json object
 * @param {*} dimensions the requetsed dimensions in the same order they appear in the values array of the datapoint
 * @returns {DetailDataPoint} the data point object
 */
export function detailDataPointFromJson(
  json: any,
  dimensions: DimensionId[]
): DetailDataPoint {
  const map: Map<DimensionId, number | null> = new Map()
  for (let i = 0; i < dimensions.length; i++) {
    map.set(dimensions[i], json.values[i])
  }
  return new DetailDataPoint(
    json.hash,
    json.parents,
    json.author,
    json.author_date,
    json.summary,
    map
  )
}

/**
 * Parses a comparison graph data point json to a DataPoint object.
 *
 * @export
 * @param {*} json the json object
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
