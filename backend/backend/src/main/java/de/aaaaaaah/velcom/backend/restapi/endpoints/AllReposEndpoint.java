package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.RepoReadAccess;
import de.aaaaaaah.velcom.backend.access.TokenReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Branch;
import de.aaaaaaah.velcom.backend.access.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.entities.Interpretation;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimension;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRepo;
import java.util.HashSet;
import java.util.List;
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
	public GetReply get() {
		List<JsonRepo> repos = repoAccess.getAllRepos().stream()
			.map(repo -> {
				RepoId repoId = repo.getRepoId();

				Set<Branch> branches = new HashSet<>(repoAccess.getBranches(repoId));
				Set<Branch> trackedBranches = new HashSet<>(branches);
				trackedBranches.retainAll(repo.getTrackedBranches());
				Set<Branch> untrackedBranches = new HashSet<>(branches);
				untrackedBranches.removeAll(trackedBranches);

				List<String> trackedNames = trackedBranches.stream()
					.map(Branch::getName)
					.map(BranchName::getName)
					.collect(Collectors.toList());
				List<String> untrackedNames = untrackedBranches.stream()
					.map(Branch::getName)
					.map(BranchName::getName)
					.collect(Collectors.toList());

				// TODO get correct unit and interpretation instead of placeholder values
				List<JsonDimension> dimensions = benchmarkAccess.getAvailableMeasurements(repoId).stream()
					.map(dimension -> new JsonDimension(
						dimension.getBenchmark(),
						dimension.getMetric(),
						"",
						Interpretation.NEUTRAL)
					)
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

		return new GetReply(repos);
	}

	private static class GetReply {

		private final List<JsonRepo> repos;

		public GetReply(List<JsonRepo> repos) {
			this.repos = repos;
		}

		public List<JsonRepo> getRepos() {
			return repos;
		}
	}
}
