package de.aaaaaaah.velcom.backend.listener.github;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.jooq.codegen.db.tables.GithubCommand.GITHUB_COMMAND;
import static org.jooq.codegen.db.tables.Repo.REPO;
import static org.jooq.codegen.db.tables.Run.RUN;
import static org.jooq.codegen.db.tables.Task.TASK;
import static org.jooq.impl.DSL.select;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Run;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.DimensionReadAccess;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Interpretation;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Repo.GithubInfo;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.taskaccess.entities.TaskPriority;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.data.runcomparison.DimensionDifference;
import de.aaaaaaah.velcom.backend.data.significance.SignificanceDetector;
import de.aaaaaaah.velcom.backend.data.significance.SignificanceReasons;
import de.aaaaaaah.velcom.backend.storage.db.DBWriteAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.shared.util.Pair;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
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
	private final BenchmarkReadAccess benchmarkAccess;
	private final CommitReadAccess commitAccess;
	private final DimensionReadAccess dimensionAccess;
	private final SignificanceDetector significanceDetector;
	private final Queue queue;

	private final String frontendUrl;

	private final HttpClient client;
	private final JsonBodyHandler jsonBodyHandler;
	private final String basicAuthHeader;

	private GithubPrInteractor(Repo repo, GithubInfo ghInfo, DatabaseStorage databaseStorage,
		BenchmarkReadAccess benchmarkAccess, CommitReadAccess commitAccess,
		DimensionReadAccess dimensionAccess, SignificanceDetector significanceDetector, Queue queue,
		String frontendUrl) {

		this.repo = repo;
		this.ghInfo = ghInfo;
		this.databaseStorage = databaseStorage;
		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
		this.dimensionAccess = dimensionAccess;
		this.significanceDetector = significanceDetector;
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
		BenchmarkReadAccess benchmarkAccess, CommitReadAccess commitAccess,
		DimensionReadAccess dimensionAccess, SignificanceDetector significanceDetector, Queue queue,
		String frontendUrl) {

		Optional<GithubInfo> ghInfoOpt = repo.getGithubInfo();
		return ghInfoOpt.map(githubInfo -> new GithubPrInteractor(
			repo,
			githubInfo,
			databaseStorage,
			benchmarkAccess,
			commitAccess,
			dimensionAccess,
			significanceDetector,
			queue,
			frontendUrl
		));
	}

	private static GithubCommand commandRecordToCommand(GithubCommandRecord record) {
		return new GithubCommand(
			RepoId.fromString(record.getRepoId()),
			record.getPr(),
			BranchName.fromName(record.getTargetBranch()),
			record.getComment(),
			new CommitHash(record.getCommitHash()),
			GithubCommandState.fromTextualRepresentation(record.getState()),
			record.getTriesLeft()
		);
	}

	private void checkResponse(HttpResponse<JsonNode> response) throws GithubApiError {
		if (response.statusCode() < 100 || response.statusCode() >= 300) {
			JsonNode message = response.body().get("message");
			if (message == null) {
				throw new GithubApiError("Something went wrong", response.uri());
			} else {
				throw new GithubApiError(message.toString(), response.uri());
			}
		}
	}

	private JsonNode getResourceFromUrl(String issueUrl)
		throws URISyntaxException, IOException, InterruptedException, GithubApiError {

		URI uri = new URI(issueUrl);
		HttpRequest request = HttpRequest.newBuilder(uri)
			.header(HttpHeaders.AUTHORIZATION, basicAuthHeader)
			.header(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
			.build();
		HttpResponse<JsonNode> response = client.send(request, jsonBodyHandler);
		checkResponse(response);
		return response.body();
	}

	private JsonNode getCommentPage(int page)
		throws IOException, InterruptedException, GithubApiError {

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
		checkResponse(response);
		return response.body();
	}

	private Optional<Instant> findNewCommandCandidates(
		List<Pair<GithubCommand, String>> commandCandidates)
		throws IOException, InterruptedException, URISyntaxException, GithubApiError {

		Set<Long> seen = new HashSet<>();
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
				BranchName targetBranch = BranchName.fromName(pr.get("base").get("ref").asText());
				CommitHash commitHash = new CommitHash(pr.get("head").get("sha").asText());
				GithubCommand command = new GithubCommand(repo.getId(), prNumber, targetBranch, commentId,
					commitHash);
				String username = comment.get("user").get("login").asText();
				commandCandidates.add(new Pair<>(command, username));
				LOGGER.debug("Found command for pr #{} ({}, by {})", prNumber, commentBody, username);
			}
		}

		return newCommentCutoff;
	}

	private List<GithubCommand> keepOnlyAuthorizedCandidates(
		List<Pair<GithubCommand, String>> commandCandidates)
		throws IOException, InterruptedException, GithubApiError {

		Set<String> usernames = commandCandidates.stream()
			.map(Pair::getSecond)
			.collect(toSet());

		Set<String> usersWithWritePermission = new HashSet<>();
		for (String username : usernames) {
			LOGGER.debug("Checking if {} has write permissions", username);
			URI url = UriBuilder.fromUri("https://api.github.com/")
				.path("repos")
				.path(ghInfo.getRepoName())
				.path("collaborators")
				.path(username)
				.path("permission")
				.build();
			HttpRequest request = HttpRequest.newBuilder(url)
				.header(HttpHeaders.AUTHORIZATION, basicAuthHeader)
				.header(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
				.build();
			HttpResponse<JsonNode> response = client.send(request, jsonBodyHandler);
			checkResponse(response);
			if (response.statusCode() != 200) {
				LOGGER.debug("User {} doesn't have write permissions (not a collaborator)", username);
				continue;
			}
			String permission = response.body().get("permission").asText();
			if (permission.equals("admin") || permission.equals("write")) {
				LOGGER.debug("User {} has write permissions ({})", username, permission);
				usersWithWritePermission.add(username);
			} else {
				LOGGER.debug("User {} doesn't have write permissions ({})", username, permission);
			}
		}

		return commandCandidates.stream()
			.filter(pair -> usersWithWritePermission.contains(pair.getSecond()))
			.map(Pair::getFirst)
			.collect(toList());
	}

	public void searchForNewPrCommands()
		throws IOException, InterruptedException, URISyntaxException, GithubApiError {

		LOGGER.debug("Searching for new PR commands");

		List<Pair<GithubCommand, String>> commandCandidates = new ArrayList<>();
		Optional<Instant> newCommentCutoff = findNewCommandCandidates(commandCandidates);
		List<GithubCommand> commands = keepOnlyAuthorizedCandidates(commandCandidates);

		if (newCommentCutoff.isEmpty() || commands.isEmpty()) {
			// If we don't have a new cutoff, definitely haven't found any new commands either
			return;
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
					command.getTargetBranch().getName(),
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
				.put("content", "rocket");
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
				LOGGER.warn("Failed to mark: {}", response.body());
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

	private HttpResponse<String> createPrComment(long pr, String body)
		throws IOException, InterruptedException {

		ObjectNode requestBody = new ObjectMapper().createObjectNode()
			.put("body", body);
		URI uri = UriBuilder.fromUri("https://api.github.com/")
			.path("repos")
			.path(ghInfo.getRepoName())
			.path("issues")
			.path(Long.toString(pr))
			.path("comments")
			.build();
		HttpRequest request = HttpRequest.newBuilder(uri)
			.POST(BodyPublishers.ofString(requestBody.toString()))
			.header(HttpHeaders.AUTHORIZATION, basicAuthHeader)
			.header(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
			.build();
		return client.send(request, BodyHandlers.ofString());
	}

	public void replyToFinishedPrCommands() throws IOException, InterruptedException {
		LOGGER.debug("Replying to finished commands");
		String repoIdStr = repo.getIdAsString();

		List<FinishedGithubCommand> replies = databaseStorage.acquireReadTransaction(db -> {
			return db.dsl()
				.selectDistinct(GITHUB_COMMAND.PR, GITHUB_COMMAND.TARGET_BRANCH, GITHUB_COMMAND.COMMIT_HASH,
					RUN.ID)
				.from(GITHUB_COMMAND)
				.join(RUN)
				.on(RUN.COMMIT_HASH.eq(GITHUB_COMMAND.COMMIT_HASH))
				.where(GITHUB_COMMAND.REPO_ID.eq(repoIdStr))
				.and(RUN.REPO_ID.eq(repoIdStr))
				.stream()
				.map(record -> new FinishedGithubCommand(
					record.value1(),
					BranchName.fromName(record.value2()),
					new CommitHash(record.value3()),
					RunId.fromString(record.value4())
				))
				.collect(toList());
		});

		for (FinishedGithubCommand reply : replies) {
			LOGGER
				.debug("Replying to PR #{} and hash {}", reply.getPr(), reply.getCommitHash().getHash());

			// Get run
			RunId runId = reply.getRunId();
			Run run = benchmarkAccess.getRun(runId);

			// Get commits and runs to compare to
			List<CommitHash> compareToHash = commitAccess
				.getFirstParentsOfBranch(repo.getId(), reply.getTargetBranch(), reply.getCommitHash());
			Map<CommitHash, RunId> runIds = benchmarkAccess.getLatestRunIds(repo.getId(), compareToHash);
			List<Run> compareToRuns = benchmarkAccess.getRuns(runIds.values());

			// Determine Significance of run
			Set<Dimension> significantDimensions = dimensionAccess.getSignificantDimensions();
			Optional<SignificanceReasons> reasons = significanceDetector
				.getSignificance(run, compareToRuns, significantDimensions);
			Set<Dimension> dimensions = reasons.stream()
				.map(SignificanceReasons::getDimensions)
				.flatMap(Collection::stream)
				.collect(toSet());
			Map<Dimension, DimensionInfo> infos = dimensionAccess.getDimensionInfoMap(dimensions);

			String message = buildFinishedPrReply(reply, runId, compareToRuns, reasons.orElse(null),
				infos);
			HttpResponse<String> response = createPrComment(reply.getPr(), message);

			if (response.statusCode() >= 200 && response.statusCode() < 300) {
				databaseStorage.acquireWriteTransaction(db -> {
					db.dsl()
						.deleteFrom(GITHUB_COMMAND)
						.where(GITHUB_COMMAND.REPO_ID.eq(repoIdStr))
						.and(GITHUB_COMMAND.PR.eq(reply.getPr()))
						.and(GITHUB_COMMAND.COMMIT_HASH.eq(reply.getCommitHash().getHash()))
						.execute();
				});
			} else {
				LOGGER.warn("Failed to reply: {}", response.body());
			}
		}
	}

	private String buildFinishedPrReply(FinishedGithubCommand reply, RunId runId,
		List<Run> compareToRuns, @Nullable SignificanceReasons reasons,
		Map<Dimension, DimensionInfo> infos) {

		StringBuilder builder = new StringBuilder();

		builder
			.append("Here are the [benchmark results](")
			.append(frontendUrl)
			.append("run-detail/")
			.append(runId.getIdAsString())
			.append(") for commit ")
			.append(reply.getCommitHash().getHash())
			.append(".");

		if (reasons == null) {
			builder.append("\nThere were no significant changes.");
			return builder.toString();
		}

		if (reasons.isEntireRunFailed()) {
			builder.append("\nThe entire run failed.");
		}

		if (!reasons.getSignificantFailedDimensions().isEmpty()) {
			builder.append("\nThese dimensions failed:");
			buildSignificanceDiff(builder, List.of(), reasons.getSignificantFailedDimensions(), infos);
		}

		if (compareToRuns.isEmpty()) {
			builder.append("\nFound no runs to compare against.");
		} else if (reasons.getSignificantDifferences().isEmpty()) {
			builder.append("\nFound no significant differences.");
		} else {
			Map<RunId, List<DimensionDifference>> diffsByRun = reasons.getSignificantDifferences()
				.stream()
				.collect(groupingBy(DimensionDifference::getOldRunId));
			for (Run run : compareToRuns) {
				List<DimensionDifference> diffs = diffsByRun.get(run.getId());
				if (diffs == null || diffs.isEmpty()) {
					continue;
				}
				if (run.getSource().getLeft().isEmpty()) {
					// This should never happen since all runs are obtained from commits
					continue;
				}

				builder
					.append("\nThere were [significant changes](")
					.append(frontendUrl)
					.append("compare/")
					.append(run.getId().getIdAsString())
					.append("/to/")
					.append(runId.getIdAsString())
					.append(") against commit ")
					.append(run.getSource().getLeft().get().getHash())
					.append(":");
				buildSignificanceDiff(builder, diffs, List.of(), infos);
			}
		}

		return builder.toString();
	}

	private static class TableLine {

		// Left-aligned
		public final String prefix;
		public final String benchmark;
		public final String metric;
		// Right-aligned
		public final String percentage;
		@Nullable
		public final String stddev;

		public TableLine(String prefix, String benchmark, String metric, String percentage,
			@Nullable String stddev) {
			this.prefix = prefix;
			this.benchmark = benchmark;
			this.metric = metric;
			this.percentage = percentage;
			this.stddev = stddev;
		}

		public Optional<String> getStddev() {
			return Optional.ofNullable(stddev);
		}
	}

	private static String ljust(int width, String string) {
		return String.format("%-" + width + "s", string);
	}

	private static String rjust(int width, String string) {
		return String.format("%" + width + "s", string);
	}

	public static void buildSignificanceDiff(StringBuilder builder,
		List<DimensionDifference> differences, List<Dimension> failed,
		Map<Dimension, DimensionInfo> infos) {

		List<TableLine> lines = new ArrayList<>();
		differences.stream()
			.map(diff -> {
				DimensionInfo info = infos.get(diff.getDimension());
				boolean less_is_better = info.getInterpretation() == Interpretation.LESS_IS_BETTER;
				boolean more_is_better = info.getInterpretation() == Interpretation.MORE_IS_BETTER;
				boolean less = diff.getDiff() < 0;
				boolean more = diff.getDiff() > 0;
				final String prefix;
				if ((less_is_better && less) || (more_is_better && more)) {
					prefix = "+";
				} else if ((less_is_better && more) || (more_is_better && less)) {
					prefix = "-";
				} else {
					prefix = " ";
				}

				return new TableLine(
					prefix,
					diff.getDimension().getBenchmark(),
					diff.getDimension().getMetric(),
					diff.getReldiff().map(d -> String.format(Locale.ROOT, "%.0f%%", d * 100)).orElse("-"),
					diff.getStddevDiff().map(d -> String.format(Locale.ROOT, "(%.1f σ)", d)).orElse(null)
				);
			})
			.forEach(lines::add);
		failed.stream()
			.map(dim -> new TableLine("-", dim.getBenchmark(), dim.getMetric(), "failed", null))
			.forEach(lines::add);
		lines.sort(comparing(line -> new Dimension(line.benchmark, line.metric)));

		TableLine legend = new TableLine(" ", "Benchmark", "Metric", "Change", null);
		int maxBenchWidth = Stream.concat(Stream.of(legend), lines.stream())
			.mapToInt(line -> line.benchmark.length())
			.max()
			.orElse(0);
		int maxMetricWidth = Stream.concat(Stream.of(legend), lines.stream())
			.mapToInt(line -> line.metric.length())
			.max()
			.orElse(0);
		int maxChangeWidth = Stream.concat(Stream.of(legend), lines.stream())
			.mapToInt(line -> line.percentage.length())
			.max()
			.orElse(0);
		int maxStddevWidth = Stream.concat(Stream.of(legend), lines.stream())
			.flatMap(line -> line.getStddev().stream())
			.mapToInt(String::length)
			.max()
			.orElse(0);
		int totalWidthWithoutPrefix = maxBenchWidth + 3 + maxMetricWidth + 3 + maxChangeWidth;
		if (maxStddevWidth > 0) {
			totalWidthWithoutPrefix += 1 + maxStddevWidth;
		}

		builder.append("\n```diff");

		// Legend
		builder
			.append("\n")
			.append(legend.prefix)
			.append(" ")
			.append(ljust(maxBenchWidth, legend.benchmark))
			.append("   ")
			.append(ljust(maxMetricWidth, legend.metric))
			.append("   ")
			.append(rjust(maxChangeWidth, legend.percentage));
		// Separator
		builder
			.append("\n  ")
			.append("=".repeat(totalWidthWithoutPrefix));
		// Actual lines
		for (TableLine line : lines) {
			builder
				.append("\n")
				.append(line.prefix)
				.append(" ")
				.append(ljust(maxBenchWidth, line.benchmark))
				.append("   ")
				.append(ljust(maxMetricWidth, line.metric))
				.append("   ")
				.append(rjust(maxChangeWidth, line.percentage));

			if (line.stddev != null) {
				builder
					.append(" ")
					.append(rjust(maxStddevWidth, line.stddev));
			}
		}

		builder.append("\n```");
	}

	public void replyToErroredPrCommands() throws IOException, InterruptedException {
		LOGGER.debug("Replying to errored commands");
		String repoId = repo.getIdAsString();

		List<Pair<Long, CommitHash>> replies = databaseStorage.acquireReadTransaction(db -> {
			return db.dsl()
				.selectDistinct(GITHUB_COMMAND.PR, GITHUB_COMMAND.COMMIT_HASH)
				.from(GITHUB_COMMAND)
				.where(GITHUB_COMMAND.STATE.eq(GithubCommandState.ERROR.getTextualRepresentation()))
				.stream()
				.map(record -> new Pair<>(
					record.value1(),
					new CommitHash(record.value2())
				))
				.collect(toList());
		});

		for (Pair<Long, CommitHash> reply : replies) {
			LOGGER.debug("Replying to PR #{} and hash {}", reply.getFirst(), reply.getSecond().getHash());

			String body = "Failed to benchmark commit "
				+ reply.getSecond().getHash()
				+ " after multiple tries.";
			HttpResponse<String> response = createPrComment(reply.getFirst(), body);

			if (response.statusCode() >= 200 && response.statusCode() < 300) {
				databaseStorage.acquireWriteTransaction(db -> {
					db.dsl()
						.deleteFrom(GITHUB_COMMAND)
						.where(GITHUB_COMMAND.REPO_ID.eq(repoId))
						.and(GITHUB_COMMAND.PR.eq(reply.getFirst()))
						.and(GITHUB_COMMAND.COMMIT_HASH.eq(reply.getSecond().getHash()))
						.execute();
				});
			} else {
				LOGGER.warn("Failed to reply: {}", response.body());
			}
		}
	}
}
