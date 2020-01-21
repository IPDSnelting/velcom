package de.aaaaaaah.velcom.backend.restapi.endpoints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.aaaaaaah.velcom.backend.access.repo.BranchName;
import de.aaaaaaah.velcom.backend.access.repo.RemoteUrl;
import de.aaaaaaah.velcom.backend.access.repo.Repo;
import de.aaaaaaah.velcom.backend.access.repo.RepoAccess;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.access.token.AuthToken;
import de.aaaaaaah.velcom.backend.access.token.TokenAccess;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.listener.CommitSearchException;
import de.aaaaaaah.velcom.backend.listener.Listener;
import de.aaaaaaah.velcom.backend.restapi.RepoUser;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRepo;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The REST API endpoint allowing to query and modify repos.
 */
@Path("/repo")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RepoEndpoint {

	private static final Logger LOGGER = LoggerFactory.getLogger(RepoEndpoint.class);

	private final RepoAccess repoAccess;
	private final TokenAccess tokenAccess;

	private final Queue queue;
	private final Listener listener;

	public RepoEndpoint(RepoAccess repoAccess, TokenAccess tokenAccess, Queue queue,
		Listener listener) {

		this.repoAccess = repoAccess;
		this.tokenAccess = tokenAccess;

		this.queue = queue;
		this.listener = listener;
	}

	/**
	 * Returns the repo with the given id.
	 *
	 * @param repoUuid the id of the repo
	 * @return the repo
	 */
	@GET
	public GetReply get(@NotNull @QueryParam("repo_id") UUID repoUuid) {
		RepoId repoId = new RepoId(repoUuid);
		Repo repo = repoAccess.getRepo(repoId);
		return new GetReply(new JsonRepo(repo));
	}

	/**
	 * Adds a new repo.
	 *
	 * @param request the repo add metadata
	 * @return the added repo
	 */
	@POST
	public GetReply post(@Auth RepoUser user, @NotNull PostRequest request) {
		user.guardAdminAccess();

		Repo repo = repoAccess.addRepo(request.getName(), request.getRemoteUrl());

		request.getToken()
			.map(AuthToken::new)
			.ifPresent(token -> tokenAccess.setToken(repo.getId(), token));

		// Run listener on this repo on a separate thread
		new Thread(() -> {
			try {
				listener.checkForUnknownCommits(repo.getId());
			} catch (CommitSearchException e) {
				LOGGER.warn("Failed to run listener for new repo: " + repo, e);
			}
		}, "ListenerThreadFor" + repo.getId().getId().toString()).start();

		return new GetReply(new JsonRepo(repo));
	}

	/**
	 * Changes an existing repo.
	 *
	 * @param request the change request
	 */
	@PATCH
	public void patch(@Auth RepoUser user, @NotNull PatchRequest request) {
		RepoId repoId = new RepoId(request.getRepoId());
		user.guardRepoAccess(repoId);

		request.getName().ifPresent(name -> repoAccess.setName(repoId, name));

		request.getRemoteUrl().ifPresent(remoteUrl -> repoAccess.setRemoteUrl(repoId, remoteUrl));

		request.getToken().ifPresent(string -> {
			if (string.isEmpty()) {
				tokenAccess.setToken(repoId, null);
			} else {
				tokenAccess.setToken(repoId, new AuthToken(string));
			}
		});

		request.getTrackedBranches().ifPresent(trackedBranches -> {
			Set<BranchName> trackedBranchNames = trackedBranches.stream()
				.map(BranchName::fromName)
				.collect(Collectors.toUnmodifiableSet());
			repoAccess.setTrackedBranches(repoId, trackedBranchNames);
		});
	}

	/**
	 * Deletes a repo.
	 *
	 * @param repoUuid the id of the repo to delete
	 */
	@DELETE
	public void delete(@Auth RepoUser user, @NotNull @QueryParam("repo_id") UUID repoUuid) {
		RepoId repoId = new RepoId(repoUuid);
		user.guardRepoAccess(repoId);

		Repo repo = repoAccess.getRepo(repoId);

		repoAccess.deleteRepo(repoId);
		tokenAccess.deleteAllUnused();
		queue.abortAllTasksOfRepo(repoId);
	}

	private static class GetReply {

		private final JsonRepo repo;

		public GetReply(JsonRepo repo) {
			this.repo = repo;
		}

		public JsonRepo getRepo() {
			return repo;
		}
	}

	private static class PostRequest {

		private final String name;
		private final RemoteUrl remoteUrl;
		@Nullable
		private final String token;

		@JsonCreator
		public PostRequest(
			@JsonProperty(value = "name", required = true) String name,
			@JsonProperty(value = "remote_url", required = true) String remoteUrl,
			@Nullable @JsonProperty("token") String token) {

			this.name = Objects.requireNonNull(name);
			this.remoteUrl = new RemoteUrl(remoteUrl);
			this.token = token;
		}

		public String getName() {
			return name;
		}

		public RemoteUrl getRemoteUrl() {
			return remoteUrl;
		}

		public Optional<String> getToken() {
			return Optional.ofNullable(token);
		}
	}

	private static class PatchRequest {

		private final UUID repoId;
		@Nullable
		private final String name;
		@Nullable
		private final RemoteUrl remoteUrl;
		@Nullable
		private final String token;
		@Nullable
		private final Collection<String> trackedBranches;

		@JsonCreator
		public PatchRequest(
			@JsonProperty(value = "repo_id", required = true) UUID repoId,
			@Nullable @JsonProperty("name") String name,
			@Nullable @JsonProperty("remote_url") String remoteUrl,
			@Nullable @JsonProperty("token") String token,
			@Nullable @JsonProperty("tracked_branches") Collection<String> trackedBranches
		) {
			this.repoId = Objects.requireNonNull(repoId);
			this.name = name;
			this.remoteUrl = (remoteUrl == null) ? null : new RemoteUrl(remoteUrl);
			this.token = token;
			this.trackedBranches = trackedBranches;
		}

		public UUID getRepoId() {
			return repoId;
		}

		public Optional<String> getName() {
			return Optional.ofNullable(name);
		}

		public Optional<RemoteUrl> getRemoteUrl() {
			return Optional.ofNullable(remoteUrl);
		}

		public Optional<String> getToken() {
			return Optional.ofNullable(token);
		}

		public Optional<Collection<String>> getTrackedBranches() {
			return Optional.ofNullable(trackedBranches);
		}
	}

}

