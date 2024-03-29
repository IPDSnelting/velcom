/* eslint-disable @typescript-eslint/explicit-module-boundary-types */
import {
  SearchItem,
  SearchItemBranch,
  SearchItemCommit,
  SearchItemRun
} from '@/store/types'

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
    json.tar_description,
    new Date(json.start_time * 1000),
    new Date(json.stop_time * 1000)
  )
}

function searchItemBranchFromJson(json: any): SearchItemBranch {
  return new SearchItemBranch(
    json.repo_id,
    json.name,
    json.commit_hash,
    json.commit_summary,
    json.has_run
  )
}

/**
 * Converts the result of the search endpoint to a list of search search items.
 * @param json the json response
 * @return the extracted search items
 */
export function searchItemsFromJson(json: any): SearchItem[] {
  return json.branches
    .map(searchItemBranchFromJson)
    .concat(json.commits.map(searchItemCommitFromJson))
    .concat(json.runs.map(searchItemRunFromJson))
}
