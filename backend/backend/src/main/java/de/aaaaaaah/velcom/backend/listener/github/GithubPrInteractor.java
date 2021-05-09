package de.aaaaaaah.velcom.backend.listener.github;

import static java.util.stream.Collectors.toList;
import static org.jooq.codegen.db.tables.GithubCommand.GITHUB_COMMAND;
import static org.jooq.codegen.db.tables.Repo.REPO;
import static org.jooq.codegen.db.tables.Run.RUN;
import static org.jooq.codegen.db.tables.Task.TASK;
import static org.jooq.impl.DSL.select;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Repo.GithubInfo;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.taskaccess.entities.TaskPriority;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.storage.db.DBWriteAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriBuilder;
import org.jooq.codegen.db.tables.records.GithubCommandRecord;
import org.jooq.codegen.db.tables.records.RepoRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interact with the GitHub API to find new !bench commands and respond to existing ones.
 */
public class GithubPrInteractor {

	private static final Logger LOGGER = LoggerFactory.getLogger(GithubPrInteractor.class);

	private final Repo repo;
	private final GithubInfo ghInfo;
	private final DatabaseStorage databaseStorage;
	private final Queue queue;

	private final String frontendUrl;

	private final HttpClient client;
	private final JsonBodyHandler jsonBodyHandler;
	private final String basicAuthHeader;

	private GithubPrInteractor(Repo repo, GithubInfo ghInfo, DatabaseStorage databaseStorage,
		Queue queue, String frontendUrl) {

		this.repo = repo;
		this.ghInfo = ghInfo;
		this.databaseStorage = databaseStorage;
		this.queue = queue;

		this.frontendUrl = frontendUrl;

		client = HttpClient.newHttpClient();
		jsonBodyHandler = new JsonBodyHandler();

		String authInfo = ":" + ghInfo.getAccessToken();
		byte[] authInfoBytes = authInfo.getBytes(StandardCharsets.UTF_8);
		String authInfoBase64 = Base64.getEncoder().encodeToString(authInfoBytes);
		basicAuthHeader = "Basic " + authInfoBase64;
	}

	public static Optional<GithubPrInteractor> fromRepo(Repo repo, DatabaseStorage databaseStorage,
		Queue queue, String frontendUrl) {

		Optional<GithubInfo> ghInfoOpt = repo.getGithubInfo();
		return ghInfoOpt.map(githubInfo -> new GithubPrInteractor(
			repo,
			githubInfo,
			databaseStorage,
			queue,
			frontendUrl
		));
	}

	private static GithubCommand commandRecordToCommand(GithubCommandRecord record) {
		return new GithubCommand(
			RepoId.fromString(record.getRepoId()),
			record.getPr(),
			record.getComment(),
			new CommitHash(record.getCommitHash()),
			GithubCommandState.fromTextualRepresentation(record.getState()),
			record.getTriesLeft()
		);
	}

	private JsonNode getResourceFromUrl(String issueUrl)
		throws URISyntaxException, IOException, InterruptedException {

		HttpRequest request = HttpRequest.newBuilder(new URI(issueUrl))
			.header(HttpHeaders.AUTHORIZATION, basicAuthHeader)
			.header(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
			.build();
		HttpResponse<JsonNode> response = client.send(request, jsonBodyHandler);
		return response.body();
	}

	private JsonNode getCommentPage(int page) throws IOException, InterruptedException {
		URI uri = UriBuilder.fromUri("https://api.github.com/")
			.path("repos")
			.path(ghInfo.getRepoName())
			.path("issues/comments")
			.queryParam("since", ghInfo.getCommentCutoff().toString())
			.queryParam("sort", "created")
			// The direction is important! If we used "asc" instead, we might lose older comments if
			// comments are updated in-between fetching different pages. With "desc", we instead see some
			// comments multiple times in the same scenario (and we'll be able to get the comments we
			// missed on the next run).
			.queryParam("direction", "desc")
			.queryParam("page", page)
			.build();
		HttpRequest request = HttpRequest.newBuilder(uri)
			.header(HttpHeaders.AUTHORIZATION, basicAuthHeader)
			.header(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
			.build();
		HttpResponse<JsonNode> response = client.send(request, jsonBodyHandler);
		return response.body();
	}

	public void searchForNewPrCommands()
		throws IOException, InterruptedException, URISyntaxException {

		LOGGER.debug("Searching for new PR commands");

		Set<Long> seen = new HashSet<>();
		List<GithubCommand> commands = new ArrayList<>();
		Instant commentCutoff = ghInfo.getCommentCutoff();
		Optional<Instant> newCommentCutoff = Optional.empty();

		for (int page = 1; true; page++) {
			LOGGER.debug("Looking at comment page {}", page);
			JsonNode comments = getCommentPage(page);
			if (comments.isEmpty()) {
				LOGGER.debug("Page {} is empty", page);
				break; // We've hit the last page
			}

			for (JsonNode comment : comments) {
				Instant createdAt = Instant.parse(comment.get("created_at").asText());
				if (createdAt.isBefore(commentCutoff) || createdAt.equals(commentCutoff)) {
					// The "since" query parameter only cuts off comments that were updated before the
					// specified cutoff. However, we don't want to look at any comments *created* before the
					// cutoff, so we need to do a bit of extra filtering.
					LOGGER.debug("Ignoring comment created before comment cutoff");
					continue;
				}

				if (newCommentCutoff.isEmpty()) {
					// We know that this comment is the first comment returned by the GitHub API. Since the
					// comments are sorted by creation time in descending order, we know that this is the
					// newest comment.
					newCommentCutoff = Optional.of(createdAt);
				}

				long commentId = comment.get("id").asLong();
				if (seen.contains(commentId)) {
					// New comments may have appeared since we requested the previous page, leading to
					// comments we've already seen being pushed to this (next) page.
					LOGGER.debug("Ignoring comment we've already seen");
					continue;
				}
				seen.add(commentId);

				String commentBody = comment.get("body").asText();
				if (!commentBody.strip().equals("!bench")) {
					LOGGER.debug("Ignoring comment without command ({})", commentBody);
					continue; // Not a command, just a normal comment
				}

				JsonNode issue = getResourceFromUrl(comment.get("issue_url").asText());
				if (!issue.has("pull_request")) {
					LOGGER.debug("Ignoring issue comment ({})", commentBody);
					continue; // Not a PR, just a normal issue
				}

				JsonNode pr = getResourceFromUrl(issue.get("pull_request").get("url").asText());

				long prNumber = pr.get("number").asLong();
				CommitHash commitHash = new CommitHash(pr.get("head").get("sha").asText());
				commands.add(new GithubCommand(repo.getId(), prNumber, commentId, commitHash));
				LOGGER.debug("Found command for pr #{} ({})", prNumber, commentBody);
			}
		}

		if (newCommentCutoff.isEmpty()) {
			return; // We definitely haven't found any new commands either
		}
		Instant newCommentCutoffValue = newCommentCutoff.get();

		databaseStorage.acquireWriteTransaction(db -> {
			RepoRecord repoRecord = db.dsl()
				.selectFrom(REPO)
				.where(REPO.ID.eq(repo.getIdAsString()))
				.fetchSingle();
			if (repoRecord.getGithubCommentCutoff() == null) {
				return; // While we were crawling, GitHub integration has been turned off
			}
			repoRecord.setGithubCommentCutoff(newCommentCutoffValue);
			db.dsl().batchUpdate(repoRecord).execute();

			List<GithubCommandRecord> commandRecords = commands.stream()
				.map(command -> new GithubCommandRecord(
					command.getRepoId().getIdAsString(),
					command.getPr(),
					command.getComment(),
					command.getCommitHash().getHash(),
					command.getState().getTextualRepresentation(),
					command.getTriesLeft()
				))
				.collect(toList());
			db.dsl().batchInsert(commandRecords).execute();
		});
	}

	public void markNewPrCommandsAsSeen() throws IOException, InterruptedException {
		LOGGER.debug("Marking new commands as seen");

		List<GithubCommand> newCommands = databaseStorage.acquireReadTransaction(db -> {
			return db.dsl()
				.selectFrom(GITHUB_COMMAND)
				.where(GITHUB_COMMAND.REPO_ID.eq(repo.getIdAsString()))
				.and(GITHUB_COMMAND.STATE.eq(GithubCommandState.NEW.getTextualRepresentation()))
				.stream()
				.map(GithubPrInteractor::commandRecordToCommand)
				.collect(toList());
		});

		for (GithubCommand command : newCommands) {
			LOGGER.debug("Marking command {} in pr #{}", command.getComment(), command.getPr());

			ObjectNode requestBody = new ObjectMapper().createObjectNode()
				.put("content", "+1");
			URI uri = UriBuilder.fromUri("https://api.github.com/")
				.path("repos")
				.path(ghInfo.getRepoName())
				.path("issues/comments")
				.path(Long.toString(command.getComment()))
				.path("reactions")
				.build();
			HttpRequest request = HttpRequest.newBuilder(uri)
				.POST(BodyPublishers.ofString(requestBody.toString()))
				.header(HttpHeaders.AUTHORIZATION, basicAuthHeader)
				.header(HttpHeaders.ACCEPT, "application/vnd.github.squirrel-girl-preview")
				.build();
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

			if (response.statusCode() == 200 || response.statusCode() == 201) {
				databaseStorage.acquireWriteTransaction(db -> {
					db.dsl()
						.update(GITHUB_COMMAND)
						.set(GITHUB_COMMAND.STATE, GithubCommandState.MARKED_SEEN.getTextualRepresentation())
						.where(GITHUB_COMMAND.REPO_ID.eq(repo.getIdAsString()))
						.and(GITHUB_COMMAND.COMMENT.eq(command.getComment()))
						.execute();
				});
			} else {
				LOGGER.debug("Failed to mark: {}", response.body());
			}
		}
	}

	private void markCommandsAsQueued(DBWriteAccess db) {
		String repoId = repo.getIdAsString();
		db.dsl()
			.update(GITHUB_COMMAND)
			.set(GITHUB_COMMAND.STATE, GithubCommandState.QUEUED.getTextualRepresentation())
			.where(GITHUB_COMMAND.REPO_ID.eq(repoId))
			.and(GITHUB_COMMAND.COMMIT_HASH.in(
				select(TASK.COMMIT_HASH)
					.from(TASK)
					.where(TASK.REPO_ID.eq(repoId))
					.and(TASK.COMMIT_HASH.isNotNull())
				).or(GITHUB_COMMAND.COMMIT_HASH.in(
				select(RUN.COMMIT_HASH)
					.from(RUN)
					.where(RUN.REPO_ID.eq(repoId))
					.and(RUN.COMMIT_HASH.isNotNull())
				))
			)
			.execute();
	}

	public void addNewPrCommandsToQueue() {
		LOGGER.debug("Adding commits for new commands to queue");

		// 1. Advance all commands for which a task or run exists to QUEUED
		List<CommitHash> toBeQueued = databaseStorage.acquireWriteTransaction(db -> {
			markCommandsAsQueued(db);

			return db.dsl()
				.selectDistinct(GITHUB_COMMAND.COMMIT_HASH)
				.from(GITHUB_COMMAND)
				.where(GITHUB_COMMAND.REPO_ID.eq(repo.getIdAsString()))
				.and(GITHUB_COMMAND.STATE.eq(GithubCommandState.MARKED_SEEN.getTextualRepresentation()))
				.stream()
				.map(record -> new CommitHash(record.value1()))
				.collect(toList());
		});

		for (CommitHash commitHash : toBeQueued) {
			LOGGER.debug("Commit {} will be queued", commitHash.getHash());
		}

		// 2. Try to queue all left-over commands
		queue.addCommits("GitHub PR command", repo.getId(), toBeQueued, TaskPriority.USER_CREATED);

		// In theory, new commands might be added between steps 1 and 2 or between steps 2 and 3. In
		// practice however, the only way that might happen is through the listener. Since the listener
		// ensures that only one update process is running at a time, this should never happen.
		//
		// Should the code ever be changed such that this is possible, the worst that could happen is
		// that new commands' tries are decremented once. That shouldn't really matter since they'll
		// still have some tries left over. The tries are meant to absorb various kinds of small
		// failures, which is exactly what would happen.

		databaseStorage.acquireWriteTransaction(db -> {
			// 3. Same as 1.
			markCommandsAsQueued(db);

			// 4. Cancel all left-over commands with only 1 try left
			db.dsl()
				.update(GITHUB_COMMAND)
				.set(GITHUB_COMMAND.STATE, GithubCommandState.ERROR.getTextualRepresentation())
				.where(GITHUB_COMMAND.REPO_ID.eq(repo.getIdAsString()))
				.and(GITHUB_COMMAND.STATE.eq(GithubCommandState.MARKED_SEEN.getTextualRepresentation()))
				.and(GITHUB_COMMAND.TRIES_LEFT.le(1))
				.execute();

			// 5. Subtract 1 from the tries of all left-over commands
			db.dsl()
				.update(GITHUB_COMMAND)
				.set(GITHUB_COMMAND.TRIES_LEFT, GITHUB_COMMAND.TRIES_LEFT.minus(1))
				.where(GITHUB_COMMAND.REPO_ID.eq(repo.getIdAsString()))
				.and(GITHUB_COMMAND.STATE.eq(GithubCommandState.MARKED_SEEN.getTextualRepresentation()))
				.execute();
		});
	}

	public void replyToFinishedPrCommands() throws IOException, InterruptedException {
		List<FinishedGithubCommand> replies = databaseStorage.acquireReadTransaction(db -> {

//			CommonTableExpression<Record2<Long, String>> commitsPerPr = name("commits_per_pr")
//				.fields("pr", "commit_hash")
//				.as(selectDistinct(GITHUB_COMMAND.PR, GITHUB_COMMAND.COMMIT_HASH)
//					.from(GITHUB_COMMAND)
//					.where(GITHUB_COMMAND.REPO_ID.eq(repoId))
//					.and(GITHUB_COMMAND.STATE.eq(GithubCommandState.QUEUED.getTextualRepresentation())));

			return db.dsl()
				.selectDistinct(GITHUB_COMMAND.PR, GITHUB_COMMAND.COMMIT_HASH, RUN.ID)
				.from(GITHUB_COMMAND)
				.join(RUN)
				.on(RUN.COMMIT_HASH.eq(GITHUB_COMMAND.COMMIT_HASH))
				.where(GITHUB_COMMAND.REPO_ID.eq(repo.getIdAsString()))
				.and(RUN.REPO_ID.eq(repo.getIdAsString()))
				.stream()
				.map(record -> new FinishedGithubCommand(
					record.value1(),
					new CommitHash(record.value2()),
					RunId.fromString(record.value3())
				))
				.collect(toList());
		});

		for (FinishedGithubCommand reply : replies) {
			LOGGER
				.debug("Replying to PR #{} and hash {}", reply.getPr(), reply.getCommitHash().getHash());

			String requestBodyText = "Benchmark of commit "
				+ reply.getCommitHash().getHash()
				+ " complete.\n\n"
				+ frontendUrl
				+ "run-detail/"
				+ reply.getRunId().getIdAsString();
			ObjectNode requestBody = new ObjectMapper().createObjectNode()
				.put("body", requestBodyText);
			URI uri = UriBuilder.fromUri("https://api.github.com/")
				.path("repos")
				.path(ghInfo.getRepoName())
				.path("issues")
				.path(Long.toString(reply.getPr()))
				.path("comments")
				.build();
			HttpRequest request = HttpRequest.newBuilder(uri)
				.POST(BodyPublishers.ofString(requestBody.toString()))
				.header(HttpHeaders.AUTHORIZATION, basicAuthHeader)
				.header(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
				.build();
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

			if (response.statusCode() >= 200 && response.statusCode() < 300) {
				databaseStorage.acquireWriteTransaction(db -> {
					db.dsl()
						.deleteFrom(GITHUB_COMMAND)
						.where(GITHUB_COMMAND.REPO_ID.eq(repo.getIdAsString()))
						.and(GITHUB_COMMAND.PR.eq(reply.getPr()))
						.and(GITHUB_COMMAND.COMMIT_HASH.eq(reply.getCommitHash().getHash()))
						.execute();
				});
			} else {
				LOGGER.debug("Failed to reply: {}", response.body());
			}
		}
	}

	public void replyToErroredPrCommands() {
		// TODO: 31.03.21 Implement
	}
}
