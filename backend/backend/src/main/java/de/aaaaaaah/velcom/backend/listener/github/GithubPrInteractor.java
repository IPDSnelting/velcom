package de.aaaaaaah.velcom.backend.listener.github;

import static java.util.stream.Collectors.toList;
import static org.jooq.codegen.db.tables.Repo.REPO;

import com.fasterxml.jackson.databind.JsonNode;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Repo.GithubInfo;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
	private static final int TRIES = 5;

	private final Repo repo;
	private final GithubInfo ghInfo;
	private final DatabaseStorage databaseStorage;

	private final HttpClient client;
	private final JsonBodyHandler jsonBodyHandler;
	private final String basicAuthHeader;

	private GithubPrInteractor(Repo repo, GithubInfo ghInfo, DatabaseStorage databaseStorage) {
		this.repo = repo;
		this.ghInfo = ghInfo;
		this.databaseStorage = databaseStorage;

		client = HttpClient.newHttpClient();
		jsonBodyHandler = new JsonBodyHandler();

		String authInfo = ":" + ghInfo.getAccessToken();
		byte[] authInfoBytes = authInfo.getBytes(StandardCharsets.UTF_8);
		String authInfoBase64 = Base64.getEncoder().encodeToString(authInfoBytes);
		basicAuthHeader = "Basic " + authInfoBase64;
	}

	public static Optional<GithubPrInteractor> fromRepo(Repo repo, DatabaseStorage databaseStorage) {
		Optional<GithubInfo> ghInfoOpt = repo.getGithubInfo();
		return ghInfoOpt.map(githubInfo -> new GithubPrInteractor(repo, githubInfo, databaseStorage));
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
