/* eslint-disable @typescript-eslint/explicit-module-boundary-types */
import { ShortRunDescription } from '@/store/types'

/**
 * Converts a short run description from json to a proper object.
 * @param json the json form
 * @return the run short description
 */
export function shortRunDescriptionFromJson(json: any): ShortRunDescription {
  return new ShortRunDescription(
    json.id,
    json.commit_summary,
    json.tar_description
  )
}
