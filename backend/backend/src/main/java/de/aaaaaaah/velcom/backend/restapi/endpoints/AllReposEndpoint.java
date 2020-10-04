package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.TokenReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.RepoReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.Branch;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimension;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRepo;
import io.micrometer.core.annotation.Timed;
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

	private final RepoReadAccess repoAccess;
	private final BenchmarkReadAccess benchmarkAccess;
	private final TokenReadAccess tokenAccess;

	public AllReposEndpoint(RepoReadAccess repoAccess,
		BenchmarkReadAccess benchmarkAccess,
		TokenReadAccess tokenAccess) {

		this.repoAccess = repoAccess;
		this.benchmarkAccess = benchmarkAccess;
		this.tokenAccess = tokenAccess;
	}

	@GET
	@Timed(histogram = true)
	public GetReply get() {
		Collection<Repo> repos = repoAccess.getAllRepos();
		List<RepoId> repoIds = repos.stream().map(Repo::getRepoId).collect(Collectors.toList());
		Map<RepoId, Set<Dimension>> allDimensions = benchmarkAccess.getAvailableDimensions(repoIds);
		Map<Dimension, DimensionInfo> dimensionInfos = benchmarkAccess.getDimensionInfos(
			allDimensions.values().stream()
				.flatMap(Set::stream)
				.collect(Collectors.toSet())
		);

		List<JsonRepo> jsonRepos = repos.stream()
			.map(repo -> {
				RepoId repoId = repo.getRepoId();

				List<Branch> allBranches = repoAccess.getAllBranches(repoId);
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

				List<JsonDimension> dimensions = allDimensions.get(repoId).stream()
					.map(dimension -> JsonDimension.fromDimensionInfo(dimensionInfos.get(dimension)))
					.collect(Collectors.toList());

				return new JsonRepo(
					repoId.getId(),
					repo.getName(),
					repo.getRemoteUrl().getUrl(),
					untrackedNames,
					trackedNames,
					tokenAccess.hasToken(repoId),
					dimensions
				);
			})
			.collect(Collectors.toList());

		return new GetReply(jsonRepos);
	}

	private static class GetReply {

		public final List<JsonRepo> repos;

		public GetReply(List<JsonRepo> repos) {
			this.repos = repos;
		}
	}
}
