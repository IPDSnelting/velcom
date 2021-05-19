package de.aaaaaaah.velcom.backend.restapi.endpoints;

import static java.util.stream.Collectors.toList;

import de.aaaaaaah.velcom.backend.access.caches.AvailableDimensionsCache;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.DimensionReadAccess;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.access.repoaccess.RepoReadAccess;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Repo.GithubInfo;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonBranch;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimension;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRepo;
import io.micrometer.core.annotation.Timed;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Endpoint for retrieving a list of all current repos.
 */
@Path("/all-repos")
@Produces(MediaType.APPLICATION_JSON)
public class AllReposEndpoint {

	private final DimensionReadAccess dimensionAccess;
	private final RepoReadAccess repoAccess;
	private final AvailableDimensionsCache availableDimensionsCache;

	public AllReposEndpoint(DimensionReadAccess dimensionAccess, RepoReadAccess repoAccess,
		AvailableDimensionsCache availableDimensionsCache) {

		this.dimensionAccess = dimensionAccess;
		this.repoAccess = repoAccess;
		this.availableDimensionsCache = availableDimensionsCache;
	}

	@GET
	@Timed(histogram = true)
	public GetReply get() {
		Collection<Repo> repos = repoAccess.getAllRepos();
		List<RepoId> repoIds = repos.stream().map(Repo::getId).collect(toList());
		Map<RepoId, Set<Dimension>> allDimensions = availableDimensionsCache
			.getAvailableDimensions(dimensionAccess, repoIds);
		Map<Dimension, DimensionInfo> dimensionInfos = dimensionAccess.getDimensionInfoMap(
			allDimensions.values().stream()
				.flatMap(Set::stream)
				.collect(Collectors.toSet())
		);

		List<JsonRepo> jsonRepos = repos.stream()
			.map(repo -> {
				RepoId repoId = repo.getId();

				List<JsonBranch> branches = repoAccess.getAllBranches(repoId).stream()
					.map(JsonBranch::fromBranch)
					.collect(toList());

				List<JsonDimension> dimensions = allDimensions.get(repoId).stream()
					.map(dimension -> JsonDimension.fromDimensionInfo(dimensionInfos.get(dimension)))
					.collect(toList());

				return new JsonRepo(
					repoId.getId(),
					repo.getName(),
					repo.getRemoteUrl().getUrl(),
					branches,
					dimensions,
					repo.getGithubInfo()
						.map(GithubInfo::getCommentCutoff)
						.map(Instant::getEpochSecond)
						.orElse(null)
				);
			})
			.collect(toList());

		return new GetReply(jsonRepos);
	}

	private static class GetReply {

		public final List<JsonRepo> repos;

		public GetReply(List<JsonRepo> repos) {
			this.repos = repos;
		}
	}
}
