package de.aaaaaaah.velcom.backend.restapi.endpoints;

import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.aaaaaaah.velcom.backend.access.caches.AvailableDimensionsCache;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.DimensionReadAccess;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.repoaccess.RepoWriteAccess;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RemoteUrl;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.repoaccess.exceptions.FailedToAddRepoException;
import de.aaaaaaah.velcom.backend.access.repoaccess.exceptions.NoSuchRepoException;
import de.aaaaaaah.velcom.backend.listener.Listener;
import de.aaaaaaah.velcom.backend.restapi.authentication.Admin;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonBranch;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimension;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRepo;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import io.micrometer.core.annotation.Timed;
import java.util.List;
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

	private final DimensionReadAccess dimensionAccess;
	private final RepoWriteAccess repoAccess;
	private final AvailableDimensionsCache availableDimensionsCache;
	private final Listener listener;

	public RepoEndpoint(DimensionReadAccess dimensionAccess, RepoWriteAccess repoAccess,
		AvailableDimensionsCache availableDimensionsCache, Listener listener) {

		this.dimensionAccess = dimensionAccess;
		this.repoAccess = repoAccess;
		this.availableDimensionsCache = availableDimensionsCache;
		this.listener = listener;
	}

	private JsonRepo toJsonRepo(Repo repo) {
		List<JsonBranch> branches = repoAccess.getAllBranches(repo.getId()).stream()
			.map(JsonBranch::fromBranch)
			.collect(toList());

		Set<Dimension> dimensions = availableDimensionsCache
			.getAvailableDimensionsFor(dimensionAccess, repo.getId());
		List<JsonDimension> jsonDimensions = dimensionAccess.getDimensionInfos(dimensions).stream()
			.map(JsonDimension::fromDimensionInfo)
			.collect(toList());

		return new JsonRepo(
			repo.getIdAsUuid(),
			repo.getName(),
			repo.getRemoteUrlAsString(),
			branches,
			jsonDimensions
		);
	}

	@POST
	@Timed(histogram = true)
	public PostReply post(@Auth Admin admin, @NotNull PostRequest request)
		throws FailedToAddRepoException {

		RemoteUrl remoteUrl = new RemoteUrl(request.getRemoteUrl());
		Repo repo = repoAccess.addRepo(request.getName(), remoteUrl);

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

		@JsonCreator
		public PostRequest(
			@JsonProperty(required = true) String name,
			@JsonProperty(required = true) String remoteUrl
		) {
			this.name = name;
			this.remoteUrl = remoteUrl;
		}

		public String getName() {
			return name;
		}

		public String getRemoteUrl() {
			return remoteUrl;
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
		@Auth Admin admin,
		@PathParam("repoid") UUID repoUuid,
		@NotNull PatchRequest request
	) throws NoSuchRepoException {
		RepoId repoId = new RepoId(repoUuid);

		// Guards whether the repo exists (that's why it's so high up in the function)
		Repo repo = repoAccess.getRepo(repoId);

		repoAccess.updateRepo(
			repoId,
			request.getName().orElse(null),
			request.getRemoteUrl().map(RemoteUrl::new).orElse(null)
		);

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

		@JsonCreator
		public PatchRequest(@Nullable String name, @Nullable String remoteUrl,
			@Nullable List<String> trackedBranches) {

			this.name = name;
			this.remoteUrl = remoteUrl;
			this.trackedBranches = trackedBranches;
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
	}

	@DELETE
	@Path("{repoid}")
	@Timed(histogram = true)
	public void delete(@Auth Admin admin, @PathParam("repoid") UUID repoUuid)
		throws NoSuchRepoException {

		RepoId repoId = new RepoId(repoUuid);
		repoAccess.guardRepoExists(repoId);

		// Also deletes the repo from all tables in the db that have a foreign key on the repo table
		// since all (relevant) foreign key restraints are marked as ON DELETE CASCADE. This includes
		// the queue table.
		repoAccess.deleteRepo(repoId);

		// TODO: 04.10.20 Notify queue/dispatcher of this deletion
	}
}
