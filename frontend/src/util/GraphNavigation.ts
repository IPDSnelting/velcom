import { vxm } from '@/store'
import { CommitHash, Dimension, RepoId } from '@/store/types'
import { VueRouter } from 'vue-router/types/router'

/**
 * Shows a given commit in the detail graph.
 *
 * @param dimension the dimension to highlight
 * @param repoId the id of the repo to show
 * @param hash the hash of the commit to show
 * @param authorDate the authorDate of the commit to show
 * @param router the router to use for navigation
 */
export async function showCommitInDetailGraph(
  dimension: Dimension,
  repoId: RepoId,
  hash: CommitHash,
  authorDate: Date,
  router: VueRouter
): Promise<void> {
  vxm.detailGraphModule.startTime = new Date(
    authorDate.getTime() - 1000 * 60 * 60 * 24 * 4
  )
  vxm.detailGraphModule.endTime = new Date(
    authorDate.getTime() + 1000 * 60 * 60 * 24 * 4
  )
  vxm.detailGraphModule.selectedRepoId = repoId
  vxm.detailGraphModule.selectedDimensions = [dimension]
  const allDataPoints = await vxm.detailGraphModule.fetchDetailGraph()

  const detailPoint = allDataPoints.find(it => it.hash === hash)

  if (detailPoint) {
    vxm.detailGraphModule.referenceDatapoint = {
      dataPoint: detailPoint,
      dimension: dimension
    }
  }

  await router.push({
    name: 'repo-detail',
    params: { id: repoId }
  })
}
