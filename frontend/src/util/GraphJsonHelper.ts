import { DataPoint, ComparisonGraphDataPoint } from '@/store/types'

/**
 * Parses a data point json to a DataPoint object.
 *
 * @export
 * @param {*} json the json object
 * @returns {DataPoint} the data point object
 */
// eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
export function dataPointFromJson(json: any): DataPoint {
  return new DataPoint(
    json.hash,
    json.parents,
    json.author,
    json.author_date,
    json.summary,
    json.values
  )
}

/**
 * Parses a comparison graph data point json to a DataPoint object.
 *
 * @export
 * @param {*} json the json object
 * @returns {ComparisonGraphDataPoint} the data point object
 */
/* eslint-disable @typescript-eslint/explicit-module-boundary-types */
export function comparisonGraphDataPointFromJson(
  json: any
): ComparisonGraphDataPoint {
  return new ComparisonGraphDataPoint(
    json.hash,
    json.author,
    json.author_date,
    json.summary,
    json.values
  )
}
