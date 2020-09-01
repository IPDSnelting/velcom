import { DataPoint } from '@/store/types'

/**
 * Parses a data point json to a DataPoint object.
 *
 * @export
 * @param {*} json the json object
 * @returns {DataPoint} the data point object
 */
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
