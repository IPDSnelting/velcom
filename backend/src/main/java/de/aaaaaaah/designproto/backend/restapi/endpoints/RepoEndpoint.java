package de.aaaaaaah.designproto.backend.restapi.endpoints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.aaaaaaah.designproto.backend.access.repo.Repo;
import de.aaaaaaah.designproto.backend.access.repo.RepoAccess;
import de.aaaaaaah.designproto.backend.access.repo.RepoId;
import de.aaaaaaah.designproto.backend.access.token.AuthToken;
import de.aaaaaaah.designproto.backend.access.token.TokenAccess;
import de.aaaaaaah.designproto.backend.restapi.jsonobjects.JsonRepo;
import io.dropwizard.jersey.PATCH;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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

/**
 * The REST API endpoint allowing to query and modify repos.
 */
@Path("/repo")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RepoEndpoint {

	private final RepoAccess repoAccess;
	private final TokenAccess tokenAccess;

	public RepoEndpoint(RepoAccess repoAccess, TokenAccess tokenAccess) {

		this.repoAccess = repoAccess;
		this.tokenAccess = tokenAccess;
	}

	/**
	 * Returns the repo with the given id.
	 *
	 * @param repoUuid the id of the repo
	 * @return the repo
	 */
	@GET
	public GetReply get(
		@NotNull @QueryParam("repo_id") UUID repoUuid) {

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
	public GetReply post(@NotNull PostRequest request) {
		Repo repo = repoAccess.addRepo(request.getName(), request.getRemoteUrl());

		request.getToken()
			.map(AuthToken::new)
			.ifPresent(token -> tokenAccess.setToken(repo.getId(), token));

		return new GetReply(new JsonRepo(repo));
	}

	/**
	 * Changes an existing.
	 *
	 * @param request the change request
	 */
	@PATCH
	public void patch(@NotNull PatchRequest request) {
		RepoId repoId = new RepoId(request.getRepoId());

		request.getName().ifPresent(name -> repoAccess.setName(repoId, name));

		request.getRemoteUrl().ifPresent(remoteUrl -> repoAccess.setRemoteUrl(repoId, remoteUrl));

		request.getToken().ifPresent(string -> {
			if (string.isEmpty()) {
				tokenAccess.setToken(repoId, null);
			} else {
				tokenAccess.setToken(repoId, new AuthToken(string));
			}
		});
	}

	/**
	 * Deletes a repo.
	 *
	 * @param repoUuid the id of the repo to delete
	 */
	@DELETE
	public void delete(
		@NotNull @QueryParam("repo_id") UUID repoUuid) {

		RepoId repoId = new RepoId(repoUuid);
		Repo repo = repoAccess.getRepo(repoId);

		repoAccess.deleteRepo(repoId);
		tokenAccess.deleteAllUnused();
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
		private final URI remoteUrl;
		@Nullable
		private final String token;

		@JsonCreator
		public PostRequest(
			@JsonProperty(value = "name", required = true) String name,
			@JsonProperty(value = "remote_url", required = true) URI remoteUrl,
			@Nullable @JsonProperty("token") String token) {

			this.name = Objects.requireNonNull(name);
			this.remoteUrl = Objects.requireNonNull(remoteUrl);
			this.token = token;
		}

		public String getName() {
			return name;
		}

		public URI getRemoteUrl() {
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
		private final URI remoteUrl;
		@Nullable
		private final String token;

		@JsonCreator
		public PatchRequest(
			@JsonProperty(value = "repo_id", required = true) UUID repoId,
			@Nullable @JsonProperty("name") String name,
			@Nullable @JsonProperty("remote_url") URI remoteUrl,
			@Nullable @JsonProperty("token") String token) {

			this.repoId = Objects.requireNonNull(repoId);
			this.name = name;
			this.remoteUrl = remoteUrl;
			this.token = token;
		}

		public UUID getRepoId() {
			return repoId;
		}

		public Optional<String> getName() {
			return Optional.ofNullable(name);
		}

		public Optional<URI> getRemoteUrl() {
			return Optional.ofNullable(remoteUrl);
		}

		public Optional<String> getToken() {
			return Optional.ofNullable(token);
		}

	}

}

