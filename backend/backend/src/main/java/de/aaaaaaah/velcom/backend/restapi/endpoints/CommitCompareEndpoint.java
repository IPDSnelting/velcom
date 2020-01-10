package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.benchmark.BenchmarkAccess;
import de.aaaaaaah.velcom.backend.access.benchmark.Run;
import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitAccess;
import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.data.commitcomparison.CommitComparison;
import de.aaaaaaah.velcom.backend.data.linearlog.LinearLog;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonCommitComparison;
import java.util.Optional;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * The REST API endpoint comparing commits.
 */
@Path("/commit-compare")
@Produces(MediaType.APPLICATION_JSON)
public class CommitCompareEndpoint {

	private final BenchmarkAccess benchmarkAccess;
	private final CommitAccess commitAccess;
	private final LinearLog linearLog;

	public CommitCompareEndpoint(BenchmarkAccess benchmarkAccess, CommitAccess commitAccess,
		LinearLog linearLog) {

		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
		this.linearLog = linearLog;
	}

	/**
	 * Compares two given commits in the same repository.
	 *
	 * @param repoUuid the id of the repository the commits are on
	 * @param firstHashString the hash of the first commit to compare
	 * @param secondHashString the hash of the second commit to compare
	 * @return the comparison
	 */
	@GET
	public GetReply get(
		@NotNull @QueryParam("repo_id") UUID repoUuid,
		@QueryParam("first_commit_hash") String firstHashString,
		@QueryParam("second_commit_hash") String secondHashString) {

		RepoId repoId = new RepoId(repoUuid);

		Optional<Commit> secondCommit = Optional.ofNullable(secondHashString)
			.map(CommitHash::new)
			.map(hash -> commitAccess.getCommit(repoId, hash));

		Optional<Commit> firstCommit = Optional.ofNullable(firstHashString)
			.map(CommitHash::new)
			.map(hash -> commitAccess.getCommit(repoId, hash))
			.or(() -> secondCommit.flatMap(linearLog::getPreviousCommit));

		Optional<Run> first = firstCommit.flatMap(benchmarkAccess::getLatestRunOf);
		Optional<Run> second = secondCommit.flatMap(benchmarkAccess::getLatestRunOf);

		CommitComparison comparison = new CommitComparison(first.orElse(null), second.orElse(null));
		return new GetReply(new JsonCommitComparison(comparison));
	}

	private static class GetReply {

		private final JsonCommitComparison comparison;

		public GetReply(JsonCommitComparison comparison) {
			this.comparison = comparison;
		}

		public JsonCommitComparison getComparison() {
			return comparison;
		}

	}

}
