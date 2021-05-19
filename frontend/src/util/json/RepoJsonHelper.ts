/* eslint-disable @typescript-eslint/explicit-module-boundary-types */
import { RepoBranch, Repo, Dimension } from '@/store/types'

/**
 * Parses a repo json to a Repo object.
 *
 * @export
 * @param {*} json the json object
 * @returns {Repo} the repo object
 */
export function repoFromJson(json: any): Repo {
  let lastGithubUpdate: Date | undefined
  if (json.last_github_update !== undefined) {
    lastGithubUpdate = new Date(json.last_github_update * 1000)
  }
  return new Repo(
    json.id,
    json.name,
    json.branches.map(repoBranchFromJson),
    json.dimensions.map((it: any) => dimensionFromJson(it)),
    json.remote_url,
    lastGithubUpdate
  )
}

export function repoBranchFromJson(json: any): RepoBranch {
  return new RepoBranch(json.name, json.tracked, json.latest_commit)
}

export function dimensionFromJson(json: any): Dimension {
  return new Dimension(
    json.benchmark,
    json.metric,
    json.unit,
    json.interpretation
  )
}
