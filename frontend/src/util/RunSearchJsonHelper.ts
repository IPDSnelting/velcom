/* eslint-disable @typescript-eslint/explicit-module-boundary-types */
import {
  SearchItem,
  SearchItemCommit,
  SearchItemRun,
  ShortRunDescription
} from '@/store/types'

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

function searchItemCommitFromJson(json: any): SearchItemCommit {
  return new SearchItemCommit(
    json.repo_id,
    json.hash,
    json.author,
    new Date(json.author_date * 1000),
    json.committer,
    new Date(json.committer_date * 1000),
    json.summary,
    json.has_run
  )
}

function searchItemRunFromJson(json: any): SearchItemRun {
  return new SearchItemRun(
    json.id,
    json.repo_id,
    json.commit_hash,
    json.commit_summary,
    json.tar_description
  )
}

/**
 * Converts the result of the search endpoint to a list of search search items.
 * @param json the json response
 * @return the extracted search items
 */
export function searchItemsFromJson(json: any): SearchItem[] {
  return json.commits
    .map(searchItemCommitFromJson)
    .concat(json.runs.map(searchItemRunFromJson))
}
