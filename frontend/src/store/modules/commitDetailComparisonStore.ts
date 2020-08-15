import { createModule, mutation, action } from 'vuex-class-component'
import {
  RunId,
  Run,
  CommitHash,
  RepoId,
  Commit,
  RunComparison
} from '@/store/types'
import axios from 'axios'
import {
  runFromJson,
  commitFromJson,
  comparisonFromJson
} from '@/util/CommitComparisonJsonHelper'

const VxModule = createModule({
  namespaced: 'commitDetailComparisonModule',
  strict: false
})

export class CommitDetailComparisonStore extends VxModule {
  /**
   * Fetches a run.
   *
   * @param {RunId} runId the id of the run
   * @returns {Promise<Run>} the run
   * @memberof CommitDetailComparisonStore
   */
  @action
  async fetchRun(runId: RunId): Promise<Run> {
    const response = await axios.get(`/run/${runId}`, {
      params: {
        all_values: true
      }
    })

    return runFromJson(response.data.run)
  }

  /**
   * Fetches a commit.
   *
   * @param {{
   *     repoId: RepoId
   *     commitHash: CommitHash
   *   }} payload the repoId and hash of the commit to fetch
   * @returns {Promise<Commit>} the commit
   * @memberof CommitDetailComparisonStore
   */
  @action
  async fetchCommit(payload: {
    repoId: RepoId
    commitHash: CommitHash
  }): Promise<Commit> {
    const response = await axios.get(
      `/commit/${payload.repoId}/${payload.commitHash}`
    )

    return commitFromJson(response.data.commit)
  }

  /**
   * Fetches a run comparison.
   *
   * @param {({
   *     first: RepoId | RunId
   *     second: RepoId | RunId
   *     hash1: string | undefined
   *     hash2: string | undefined
   *   })} payload the first, second hash1 and hash2
   * @returns {Promise<RunComparison>} the run comparison
   * @memberof CommitDetailComparisonStore
   */
  @action
  async fetchComparison(payload: {
    first: RepoId | RunId
    second: RepoId | RunId
    hash1: string | undefined
    hash2: string | undefined
  }): Promise<RunComparison> {
    const response = await axios.get(
      `/compare/${payload.first}/to/${payload.second}`,
      {
        params: {
          hash1: payload.hash1,
          hash2: payload.hash2,
          all_values: true
        }
      }
    )

    return comparisonFromJson(response.data)
  }
}
