import { createModule, mutation, action } from 'vuex-class-component'
import { RunId, Run, CommitHash, RepoId, Commit } from '@/store/types'
import axios from 'axios'
import Vue from 'vue'
import { runFromJson, commitFromJson } from '@/util/CommitComparisonJsonHelper'

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
   * Fetches the commit comparison for two commits in a repo
   * from the server.
   *
   * @param {{
   *     repoId: string
   *     first: string
   *     second: string | undefined
   *   }} payload the playload to fetch
   * @returns {Promise<CommitInfo>} a promise resolving to
   * the comparison
   * @memberof CommitDetailComparisonStore
   */
  @action
  async fetchCommitInfo(payload: {
    repoId: string
    first: string | undefined
    second: string | undefined
  }): Promise<CommitInfo> {
    const response = await axios.get('/commit-compare', {
      snackbarTag: 'commit-comparison',
      params: {
        repo_id: payload.repoId,
        first_commit_hash: payload.first,
        second_commit_hash: payload.second
      }
    })
    return commitInfoFromJson(response.data)
  }
}
