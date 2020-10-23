package de.aaaaaaah.velcom.backend.restapi.endpoints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.TokenWriteAccess;
import de.aaaaaaah.velcom.backend.access.entities.AuthToken;
import de.aaaaaaah.velcom.backend.access.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.listener.Listener;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.RepoWriteAccess;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.Branch;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RemoteUrl;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.exceptions.FailedToAddRepoException;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.exceptions.NoSuchRepoException;
import de.aaaaaaah.velcom.backend.restapi.authentication.RepoUser;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimension;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRepo;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/repo")
@Produces(MediaType.APPLICATION_JSON)
public class RepoEndpoint {
	// Most of the logic found here was copied pretty much directly from the old repo endpoint.

	private static final Logger LOGGER = LoggerFactory.getLogger(RepoEndpoint.class);

	private final RepoWriteAccess repoAccess;
	private final TokenWriteAccess tokenAccess;
	private final BenchmarkReadAccess benchmarkAccess;
	private final Listener listener;

	public RepoEndpoint(RepoWriteAccess repoAccess, TokenWriteAccess tokenAccess,
		BenchmarkReadAccess benchmarkAccess, Listener listener) {

		this.repoAccess = repoAccess;
		this.tokenAccess = tokenAccess;
		this.benchmarkAccess = benchmarkAccess;
		this.listener = listener;
	}

	private JsonRepo toJsonRepo(Repo repo) {
		List<Branch> allBranches = repoAccess.getAllBranches(repo.getId());
		List<String> trackedNames = allBranches.stream()
			.filter(Branch::isTracked)
			.map(Branch::getName)
			.map(BranchName::getName)
			.collect(Collectors.toList());
		List<String> untrackedNames = allBranches.stream()
			.filter(branch -> !branch.isTracked())
			.map(Branch::getName)
			.map(BranchName::getName)
			.collect(Collectors.toList());

		Set<Dimension> dimensions = benchmarkAccess.getAvailableDimensions(repo.getId());
		Map<Dimension, DimensionInfo> dimensionInfos = benchmarkAccess.getDimensionInfos(dimensions);
		List<JsonDimension> jsonDimensions = dimensionInfos.values().stream()
			.map(JsonDimension::fromDimensionInfo)
			.collect(Collectors.toList());

		return new JsonRepo(
			repo.getIdAsUuid(),
			repo.getName(),
			repo.getRemoteUrlAsString(),
			untrackedNames,
			trackedNames,
			tokenAccess.hasToken(repo.getId()),
			jsonDimensions
		);
	}

	@POST
	@Timed(histogram = true)
	public PostReply post(@Auth RepoUser user, @NotNull PostRequest request)
		throws FailedToAddRepoException {

		user.guardAdminAccess();

		RemoteUrl remoteUrl = new RemoteUrl(request.getRemoteUrl());
		Repo repo = repoAccess.addRepo(request.getName(), remoteUrl);

		request.getToken()
			.map(AuthToken::new)
			.ifPresent(token -> tokenAccess.setToken(repo.getId(), token));

		if (listener.updateRepo(repo)) {
			return new PostReply(toJsonRepo(repo));
		} else {
			repoAccess.deleteRepo(repo.getId());
			throw new WebApplicationException("Repo could not be cloned, invalid remote url",
				Status.BAD_REQUEST);
		}
	}

	private static class PostRequest {

		private final String name;
		private final String remoteUrl;
		@Nullable
		private final String token;

		@JsonCreator
		public PostRequest(
			@JsonProperty(required = true) String name,
			@JsonProperty(required = true) String remoteUrl,
			@JsonProperty @Nullable String token
		) {
			this.name = name;
			this.remoteUrl = remoteUrl;
			this.token = token;
		}

		public String getName() {
			return name;
		}

		public String getRemoteUrl() {
			return remoteUrl;
		}

		public Optional<String> getToken() {
			return Optional.ofNullable(token);
		}
	}

	private static class PostReply {

		private final JsonRepo repo;

		public PostReply(JsonRepo repo) {
			this.repo = repo;
		}

		public JsonRepo getRepo() {
			return repo;
		}
	}

	@GET
	@Path("{repoid}")
	@Timed(histogram = true)
	public GetReply get(@PathParam("repoid") UUID repoUuid) throws NoSuchRepoException {
		RepoId repoId = new RepoId(repoUuid);
		Repo repo = repoAccess.getRepo(repoId);

		return new GetReply(toJsonRepo(repo));
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

	@PATCH
	@Path("{repoid}")
	@Timed(histogram = true)
	public void patch(
		@Auth RepoUser user,
		@PathParam("repoid") UUID repoUuid,
		@NotNull PatchRequest request
	) throws NoSuchRepoException {

		RepoId repoId = new RepoId(repoUuid);
		user.guardRepoAccess(repoId);

		// Guards whether the repo exists
		Repo repo = repoAccess.getRepo(repoId);

		repoAccess.updateRepo(
			repoId,
			request.getName().orElse(null),
			request.getRemoteUrl().map(RemoteUrl::new).orElse(null)
		);

		request.getToken().ifPresent(token -> {
			if (token.isEmpty()) {
				tokenAccess.setToken(repoId, null);
			} else {
				tokenAccess.setToken(repoId, new AuthToken(token));
			}
		});

		request.getTrackedBranches().ifPresent(trackedBranches -> {
			Set<BranchName> trackedBranchNames = trackedBranches.stream()
				.map(BranchName::fromName)
				.collect(Collectors.toSet());
			repoAccess.setTrackedBranches(repoId, trackedBranchNames);

			if (!listener.updateRepo(repo)) {
				LOGGER.warn("Failed to update repo {} successfully", repoId);
			}
		});
	}

	private static class PatchRequest {

		@Nullable
		private final String name;
		@Nullable
		private final String remoteUrl;
		@Nullable
		private final List<String> trackedBranches;
		@Nullable
		private final String token;

		@JsonCreator
		public PatchRequest(@Nullable String name, @Nullable String remoteUrl,
			@Nullable List<String> trackedBranches, @Nullable String token) {

			this.name = name;
			this.remoteUrl = remoteUrl;
			this.trackedBranches = trackedBranches;
			this.token = token;
		}

		public Optional<String> getName() {
			return Optional.ofNullable(name);
		}

		public Optional<String> getRemoteUrl() {
			return Optional.ofNullable(remoteUrl);
		}

		public Optional<List<String>> getTrackedBranches() {
			return Optional.ofNullable(trackedBranches);
		}

		public Optional<String> getToken() {
			return Optional.ofNullable(token);
		}
	}

	@DELETE
	@Path("{repoid}")
	@Timed(histogram = true)
	public void delete(@Auth RepoUser user, @PathParam("repoid") UUID repoUuid)
		throws NoSuchRepoException {

		RepoId repoId = new RepoId(repoUuid);
		user.guardRepoAccess(repoId);

		repoAccess.guardRepoExists(repoId);

		// Also deletes the repo from all tables in the db that have a foreign key on the repo table
		// since all (relevant) foreign key restraints are marked as ON DELETE CASCADE. This includes
		// the queue table.
		repoAccess.deleteRepo(repoId);

		// TODO: 04.10.20 Notify queue/dispatcher of this deletion
	}
}
