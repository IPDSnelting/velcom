/* eslint-disable @typescript-eslint/explicit-module-boundary-types */
import {
  DataPoint,
  ComparisonGraphDataPoint,
  RepoId,
  DimensionId
} from '@/store/types'

/**
 * Parses a data point json to a DataPoint object.
 *
 * @export
 * @param {*} json the json object
 * @param {*} dimensions the requetsed dimensions in the same order they appear in the values array of the datapoint
 * @returns {DataPoint} the data point object
 */
export function dataPointFromJson(
  json: any,
  dimensions: DimensionId[]
): DataPoint {
  const map: Map<DimensionId, number | null> = new Map()
  for (let i = 0; i < dimensions.length; i++) {
    map.set(dimensions[i], json.values[i])
  }
  return new DataPoint(
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
 * @returns {ComparisonGraphDataPoint} the data point object
 */
export function comparisonGraphDataPointFromJson(
  json: any,
  repoId: RepoId
): ComparisonGraphDataPoint {
  return new ComparisonGraphDataPoint(
    json.hash,
    json.author,
    new Date(json.author_date * 1000),
    json.summary,
    json.value,
    repoId
  )
}
