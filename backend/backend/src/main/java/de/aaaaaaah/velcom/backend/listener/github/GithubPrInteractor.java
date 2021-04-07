package de.aaaaaaah.velcom.backend.listener.github;

import static org.jooq.codegen.db.tables.GithubPrs.GITHUB_PRS;

import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Repo.GithubInfo;
import de.aaaaaaah.velcom.backend.storage.db.DBReadAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jooq.codegen.db.tables.records.GithubPrsRecord;
import org.kohsuke.github.GHDirection;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestQueryBuilder.Sort;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedIterable;

public class GithubPrInteractor {

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
		PagedIterable<GHPullRequest> pullRequests = ghRepo.queryPullRequests()
			.state(GHIssueState.ALL)
			.sort(Sort.UPDATED)
			.direction(GHDirection.DESC)
			.list();

		Map<Long, GithubPrsRecord> knownPrRecords;
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			knownPrRecords = db.dsl()
				.selectFrom(GITHUB_PRS)
				.where(GITHUB_PRS.REPO_ID.eq(repo.getIdAsString()))
				.fetchMap(GITHUB_PRS.PR);
		}

		Optional<Instant> latestUpdateTime = Optional.empty();

		for (GHPullRequest pullRequest : pullRequests) {
			if (pullRequest.getUpdatedAt().toInstant().isBefore(ghInfo.getCommentCutoff())) {
				break;
			}

			if (latestUpdateTime.isEmpty()) {
				latestUpdateTime = Optional.of(pullRequest.getUpdatedAt().toInstant());
			}

			List<GHIssueComment> comments = pullRequest.getComments();
			if (comments.isEmpty()) {
				continue;
			}



			// TODO: 31.03.21 Check with DB and potentially update it
		}
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
