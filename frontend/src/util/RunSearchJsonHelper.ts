/* eslint-disable @typescript-eslint/explicit-module-boundary-types */
import { SearchItem, ShortRunDescription } from '@/store/types'
import { commitDescriptionFromJson } from '@/util/QueueJsonHelper'

/**
 * Converts a short run description from json to a proper object.
 * @param json the json form
 * @return the run short description
 */
export function shortRunDescriptionFromJson(json: any): ShortRunDescription {
  return new ShortRunDescription(
    json.id,
    json.commit_summary,
    json.tar_description,
    json.commit_hash
  )
}

/**
 * Converts the result of the search endpoint to a list of search search items.
 * @param json the json response
 * @return the extracted search items
 */
export function searchItemsFromJson(json: any): SearchItem[] {
  return json.commits
    .map(commitDescriptionFromJson)
    .concat(json.runs.map(shortRunDescriptionFromJson))
}
