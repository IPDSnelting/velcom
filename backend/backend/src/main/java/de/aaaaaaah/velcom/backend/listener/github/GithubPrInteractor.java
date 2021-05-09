package de.aaaaaaah.velcom.backend.listener.github;

import static java.util.stream.Collectors.toList;
import static org.jooq.codegen.db.tables.GithubPr.GITHUB_PR;
import static org.jooq.codegen.db.tables.Repo.REPO;

import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Repo.GithubInfo;
import de.aaaaaaah.velcom.backend.storage.db.DBReadAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.jooq.codegen.db.tables.records.GithubCommandRecord;
import org.jooq.codegen.db.tables.records.GithubPrRecord;
import org.jooq.codegen.db.tables.records.RepoRecord;
import org.kohsuke.github.GHDirection;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestQueryBuilder.Sort;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interact with the GitHub API to find new !bench commands and respond to existing ones.
 * <p>
 * Since the kohsuke GitHub API doesn't seem to support GitHub's issue comment endpoint [1], we fall
 * back to a more complicated solution.
 * <p>
 * First of all, every repo with an auth token also stores a value named "github_comment_cutoff".
 * All comments created before this point in time should be ignored. Initially, this value is set to
 * the time when the auth token was initially set. However, to avoid unnecessary API calls, it is
 * also advanced whenever we're certain that we've already seen all older comments.
 * <p>
 * For every known PR, another value named "last_comment" is stored. It is the ID of the PR's latest
 * known comment. While "github_comment_cutoff" is used mainly for optimization purposes, this value
 * is essential to avoid replying to the same comment multiple times. If new commands are found in a
 * PR and added to the DB, this "last_comment" value should be updated in the same transaction.
 * <p>
 * [1]: https://docs.github.com/en/rest/reference/issues#list-issue-comments-for-a-repository
 */
public class GithubPrInteractor {

	private static final Logger LOGGER = LoggerFactory.getLogger(GithubPrInteractor.class);
	private static final int TRIES = 5;

	private final Repo repo;
	private final GithubInfo ghInfo;
	private final DatabaseStorage databaseStorage;

	private final GitHub github;
	private final GHRepository ghRepo;

	private GithubPrInteractor(Repo repo, GithubInfo ghInfo, DatabaseStorage databaseStorage)
		throws IOException {

		this.repo = repo;
		this.ghInfo = ghInfo;

		github = new GitHubBuilder()
			.withOAuthToken(ghInfo.getAccessToken())
			.build();

		ghRepo = github.getRepository(ghInfo.getRepoName());
		this.databaseStorage = databaseStorage;
	}

	public static Optional<GithubPrInteractor> fromRepo(Repo repo, DatabaseStorage databaseStorage)
		throws IOException {

		Optional<GithubInfo> ghInfoOpt = repo.getGithubInfo();
		if (ghInfoOpt.isPresent()) {
			return Optional.of(new GithubPrInteractor(repo, ghInfoOpt.get(), databaseStorage));
		} else {
			return Optional.empty();
		}
	}

	public void searchForNewPrCommands() throws IOException {
		LOGGER.debug("Searching for new PR commands");

		PagedIterable<GHPullRequest> pullRequests = ghRepo.queryPullRequests()
			.state(GHIssueState.ALL)
			.sort(Sort.UPDATED)
			.direction(GHDirection.DESC)
			.list();

		Map<Long, GithubPrRecord> knownPrRecords;
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			knownPrRecords = db.dsl()
				.selectFrom(GITHUB_PR)
				.where(GITHUB_PR.REPO_ID.eq(repo.getIdAsString()))
				.fetchMap(GITHUB_PR.PR);
		}

		Instant commentCutoff = ghInfo.getCommentCutoff();
		Optional<Instant> newCommentCutoff = Optional.empty();

		for (GHPullRequest pullRequest : pullRequests) {
			Instant updatedAt = pullRequest.getUpdatedAt().toInstant();

			if (updatedAt.isBefore(commentCutoff) || updatedAt.equals(commentCutoff)) {
				// We've reached a PR that was last updated before our comment cutoff and thus can't contain
				// any newer commands. Since we sort the PRs by their updated time in descending order, all
				// following PRs will also not have any newer commands.
				LOGGER.debug("PR {} is before comment cutoff, breaking", pullRequest.getId());
				break;
			}

			if (newCommentCutoff.isEmpty()) {
				// The new comment cutoff should be the updated time of the first PR we look at. If we
				// manage to get through this and all following PRs, we know that we've seen all comments
				// before this point in time. This still holds true if any PR gains new commands while we
				// are still searching ("last_comment" will prevent us from looking at those twice).
				newCommentCutoff = Optional.of(updatedAt);
			}

			GithubPrRecord record = knownPrRecords.get(pullRequest.getId());
			findAndMarkNewCommands(pullRequest, record);
		}

		if (newCommentCutoff.isEmpty()) {
			LOGGER.debug("Nothing new across all PRs");
			return;
		}
		Instant newCommentCutoffInstant = newCommentCutoff.get();

		databaseStorage.acquireWriteTransaction(db -> {
			RepoRecord repoRecord = db.dsl()
				.selectFrom(REPO)
				.where(REPO.ID.eq(repo.getIdAsString()))
				.fetchSingle();

			if (repoRecord.getGithubCommentCutoff() == null) {
				return;
			}

			repoRecord.setGithubCommentCutoff(newCommentCutoffInstant);
			repoRecord.update(REPO.GITHUB_COMMENT_CUTOFF);
		});
	}

	private void findAndMarkNewCommands(GHPullRequest pr, @Nullable GithubPrRecord record)
		throws IOException {

		LOGGER.debug("Looking at PR {}", pr.getId());

		Optional<Long> lastComment = Optional.ofNullable(record).map(GithubPrRecord::getLastComment);
		Optional<Long> newLastComment = Optional.empty();
		List<Long> newCommands = new ArrayList<>();

		// This loop does not assume that the comments are ordered. The GitHub API probably returns
		// commits in some order however, meaning that this code can likely be optimized a bit.
		for (GHIssueComment comment : pr.getComments()) {
			long id = comment.getId();
			if (lastComment.isPresent() && id <= lastComment.get()) {
				continue;
			}
			if (newLastComment.isEmpty() || id > newLastComment.get()) {
				newLastComment = Optional.of(id);
			}
			LOGGER.debug("Comment {} might be a command", comment.getId());
			if (comment.getBody().strip().equals("!bench")) {
				LOGGER.debug("New command found!");
				newCommands.add(id);
			}
		}

		if (newLastComment.isEmpty()) {
			// If newLastComment is empty, newCommands is guaranteed to be empty as well.
			return;
		}

		Long newLastCommentId = newLastComment.get();
		String commitHash = pr.getHead().getSha();

		databaseStorage.acquireWriteTransaction(db -> {
			// Update the "last_comment" value
			GithubPrRecord prRecord = db.dsl()
				.selectFrom(GITHUB_PR)
				.where(GITHUB_PR.REPO_ID.eq(repo.getIdAsString()))
				.and(GITHUB_PR.PR.eq(pr.getId()))
				.fetchOne();
			if (prRecord == null) {
				prRecord = new GithubPrRecord(repo.getIdAsString(), pr.getId(), newLastCommentId);
			} else {
				prRecord.setLastComment(newLastCommentId);
			}
			db.dsl().batchUpdate(prRecord);

			// And add all newly found commands
			List<GithubCommandRecord> newCommandRecords = newCommands.stream()
				.map(commentId -> new GithubCommandRecord(
					repo.getIdAsString(),
					pr.getId(),
					commentId,
					commitHash,
					CommandState.NEW.getTextualRepresentation(),
					TRIES
				))
				.collect(toList());
			db.dsl().batchInsert(newCommandRecords).execute();
		});
	}

	public void markNewPrCommandsAsSeen() {
		// TODO: 31.03.21 Implement
	}

	public void addNewPrCommandsToQueue() {
		// TODO: 31.03.21 Implement
	}

	public void replyToFinishedPrCommands() {
		// TODO: 31.03.21 Implement
	}

	public void markFinishedPrCommandsAsComplete() {
		// TODO: 31.03.21 Implement
	}

	public void replyToErroredPrCommands() {
		// TODO: 31.03.21 Implement
	}
}
