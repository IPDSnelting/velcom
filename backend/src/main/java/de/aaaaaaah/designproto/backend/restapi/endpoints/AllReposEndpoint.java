package de.aaaaaaah.designproto.backend.restapi.endpoints;

import de.aaaaaaah.designproto.backend.access.repo.RepoAccess;
import de.aaaaaaah.designproto.backend.restapi.jsonobjects.JsonRepo;
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

	private final RepoAccess repoAccess;

	public AllReposEndpoint(RepoAccess repoAccess) {
		this.repoAccess = repoAccess;
	}

	/**
	 * Returns all known repositories.
	 *
	 * @return all repositories
	 */
	@GET
	public GetReply get() {
		List<JsonRepo> repos = repoAccess.getAllRepos().stream()
			.map(JsonRepo::new)
			.collect(Collectors.toUnmodifiableList());
		return new GetReply(repos);
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
