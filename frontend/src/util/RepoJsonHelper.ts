import { RepoBranch, Repo, Dimension } from '@/store/types'

/**
 * Parses a repo json to a Repo object.
 *
 * @export
 * @param {*} json the json object
 * @returns {Repo} the repo object
 */
export function repoFromJson(json: any): Repo {
  const untracked: string[] = json.untracked_branches
  const tracked: string[] = json.tracked_branches

  const branches: RepoBranch[] = untracked
    .map(it => new RepoBranch(it, false))
    .concat(tracked.map(it => new RepoBranch(it, true)))

  return new Repo(
    json.id,
    json.name,
    branches,
    json.dimensions.map((it: any) => dimensionFromJson(it)),
    json.remote_url,
    json.has_token
  )
}

export function dimensionFromJson(json: any): Dimension {
  return new Dimension(
    json.benchmark,
    json.metric,
    json.unit,
    json.interpretation
  )
}
