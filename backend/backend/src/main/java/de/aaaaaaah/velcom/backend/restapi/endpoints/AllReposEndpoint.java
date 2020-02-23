package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.newaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.RepoReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.TokenReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.entities.Branch;
import de.aaaaaaah.velcom.backend.newaccess.entities.MeasurementName;
import de.aaaaaaah.velcom.backend.newaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRepo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
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
