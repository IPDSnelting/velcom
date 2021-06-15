import { createModule, action } from 'vuex-class-component'
import {
  RunId,
  CommitHash,
  RepoId,
  Commit,
  RunComparison,
  RunWithDifferences
} from '@/store/types'
import axios from 'axios'
import {
  commitFromJson,
  comparisonFromJson,
  runWithDifferencesFromJson
} from '@/util/json/CommitComparisonJsonHelper'

export class NotFoundError extends Error {}

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
   * @throws NotFoundError if the server returns a 404
   */
  @action
  async fetchRun(runId: RunId): Promise<RunWithDifferences> {
    try {
      const response = await axios.get(`/run/${runId}`, {
        params: {
          all_values: true,
          diff_prev: true
        }
      })

      return runWithDifferencesFromJson(response.data)
    } catch (e) {
      if (e.response && e.response.status === 404) {
        throw new NotFoundError()
      }
      throw e
    }
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
   * @throws NotFoundError if the server returns a 404
   */
  @action
  async fetchCommit(payload: {
    repoId: RepoId
    commitHash: CommitHash
  }): Promise<Commit> {
    try {
      const response = await axios.get(
        `/commit/${payload.repoId}/${payload.commitHash}`
      )

      return commitFromJson(response.data.commit)
    } catch (e) {
      if (e.response && e.response.status === 404) {
        throw new NotFoundError()
      }
      throw e
    }
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
   * @throws NotFoundError if the server returns a 404
   */
  @action
  async fetchComparison(payload: {
    first: RepoId | RunId
    second: RepoId | RunId
    hash1: string | undefined
    hash2: string | undefined
  }): Promise<RunComparison> {
    try {
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
    } catch (e) {
      if (e.response && e.response.status === 404) {
        throw new NotFoundError()
      }
      throw e
    }
  }
}
