/**
 * This package implements VelCom's GitHub PR interactions.
 * <p>
 * <h2>Activation</h2>
 * To activate these interactions for a repo, the repo must be assigned a GitHub OAuth token for the
 * account that will post the comments. To deactivate the interactions, the token must be removed.
 * When active, VelCom will only consider comments that have been posted after the token has been
 * set. This behaviour is relatively intuitive and prevents flooding the queue with tasks and the
 * PRs with comments for old commands after a database reset or prolonged period of inactivity.
 * However, if a repo is active and the VelCom server is not started for a while, flooding like this
 * may still occur.
 * <p>
 * <h2>General process</h2>
 * When a comment containing only "!bench" is posted in a PR, VelCom will react to the comment with
 * the eyes emoji to acknowledge it and start a task for the PR's current commit hash. After the
 * task has completed, VelCom will post a link to the run as a new comment in the same PR. If the
 * task could not be created because VelCom doesn't know the commit hash, or if the task was
 * cancelled/run was deleted, the comment will contain an error message instead of a link.
 * <p>
 * <h2>Implementation</h2>
 * The way this is implemented is to have the Listener look for new "!bench" commands in a repo
 * before it is updated, and to act upon the newly found commands after it is updated. This way, all
 * found PR hashes will be synced to the DB (if they're not already there) before we try to create
 * any tasks. For each repo with a GitHub token, these steps are executed:
 * <ol>
 *   <li>Look for new commands</li>
 *   <li>Do normal listener update</li>
 *   <li>Filter out commands already being worked on</li>
 *   <li>Filter out impossible commands (hash not found locally) and send error reply</li>
 *   <li>For each command:<ol>
 *     <li>Create task if task or run doesn't already exist</li>
 *     <li>Track command in DB</li>
 *     <li>Add reaction to command comment</li>
 *   </ol></li>
 *   <li>For each command in the DB:<ol>
 *     <li>If run for commit hash exists, post success reply</li>
 *     <li>If no run and no task for commit hash exists, post error reply</li>
 *     <li>Otherwise, do nothing</li>
 *   </ol></li>
 * </ol>
 * <h2>The Big Bad List of Edge Cases</h2>
 * <ul>
 *   <li>!bench command but the commit can't be found locally</li>
 *   <li>!bench command on a commit that was already benchmarked</li>
 *   <li>Multiple !bench commands in a single PR</li>
 *   <li>Old !bench commands</li>
 *   <li>!bench commands that were already answered previously</li>
 *   <li>Any time a GitHub API call fails</li>
 * </ul>
 */
package de.aaaaaaah.velcom.backend.listener.github;
