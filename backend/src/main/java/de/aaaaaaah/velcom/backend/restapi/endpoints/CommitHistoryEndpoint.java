package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.repo.Repo;
import de.aaaaaaah.velcom.backend.access.repo.RepoAccess;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.data.linearlog.LinearLog;
import de.aaaaaaah.velcom.backend.data.linearlog.LinearLogException;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonCommit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * The REST API endpoint providing a history of commits in a repository.
 */
@Path("/commit-history")
@Produces(MediaType.APPLICATION_JSON)
public class CommitHistoryEndpoint {

	public static final int DEFAULT_AMOUNT = Integer.MAX_VALUE; // TODO choose better default amount?
	public static final int DEFAULT_SKIP = 0;

	private final RepoAccess repoAccess;
	private final LinearLog linearLog;

	public CommitHistoryEndpoint(RepoAccess repoAccess, LinearLog linearLog) {
		this.repoAccess = repoAccess;
		this.linearLog = linearLog;
	}

	/**
	 * Returns a list of the selected commits in the given repo.
	 *
	 * @param repoUuid the id of the repository
	 * @param amount the amount of commits to return
	 * @param skip the amount of commits to skip
	 * @return the selected commits
	 * @throws LinearLogException if the repository could not be brought into a linear shape for
	 * 	some reason
	 */
	@GET
	public GetReply get(
		@NotNull @QueryParam("repo_id") UUID repoUuid,
		@Min(0) @DefaultValue("" + DEFAULT_AMOUNT) @QueryParam("amount") int amount,
		@Min(0) @DefaultValue("" + DEFAULT_SKIP) @QueryParam("skip") int skip)
		throws LinearLogException {

		Repo repo = repoAccess.getRepo(new RepoId(repoUuid));
		List<JsonCommit> commits = linearLog.walkBranches(repo, repo.getTrackedBranches())
			.skip(skip)
			.limit(amount)
			.map(JsonCommit::new)
			.collect(Collectors.toUnmodifiableList());

		return new GetReply(commits);
	}

	private static class GetReply {

		private final List<JsonCommit> commits;

		public GetReply(List<JsonCommit> commits) {
			this.commits = commits;
		}

		public List<JsonCommit> getCommits() {
			return commits;
		}

	}

}
