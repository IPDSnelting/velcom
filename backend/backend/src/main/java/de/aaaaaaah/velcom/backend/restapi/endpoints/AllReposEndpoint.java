package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.RepoReadAccess;
import de.aaaaaaah.velcom.backend.access.TokenReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Branch;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementName;
import de.aaaaaaah.velcom.backend.access.entities.Repo;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRepo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * The REST API endpoint providing all repos to the frontend.
 */
@Path("/all-repos")
@Produces(MediaType.APPLICATION_JSON)
public class AllReposEndpoint {

	private final RepoReadAccess repoAccess;
	private final BenchmarkReadAccess benchmarkAccess;
	private final TokenReadAccess tokenAccess;

	public AllReposEndpoint(RepoReadAccess repoAccess, BenchmarkReadAccess benchmarkAccess,
		TokenReadAccess tokenAccess) {
		this.repoAccess = repoAccess;
		this.benchmarkAccess = benchmarkAccess;
		this.tokenAccess = tokenAccess;
	}

	/**
	 * Returns all known repositories.
	 *
	 * @return all repositories
	 */
	@GET
	public GetReply get() {
		Collection<Repo> repos = repoAccess.getAllRepos();
		List<JsonRepo> jsonRepos = new ArrayList<>(repos.size());

		for (Repo repo : repos) {
			Collection<Branch> branches = repoAccess.getBranches(repo.getRepoId());

			Collection<MeasurementName> measurements = benchmarkAccess.getAvailableMeasurements(
				repo.getRepoId()
			);

			boolean hasToken = tokenAccess.hasToken(repo.getRepoId());

			jsonRepos.add(new JsonRepo(repo, branches, measurements, hasToken));
		}

		return new GetReply(jsonRepos);
	}

	private static class GetReply {

		private final Collection<JsonRepo> repos;

		public GetReply(Collection<JsonRepo> repos) {
			this.repos = repos;
		}

		public Collection<JsonRepo> getRepos() {
			return repos;
		}

	}

}
