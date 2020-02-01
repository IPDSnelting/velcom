package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.benchmark.BenchmarkAccess;
import de.aaaaaaah.velcom.backend.access.benchmark.Run;
import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitAccess;
import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.data.commitcomparison.CommitComparer;
import de.aaaaaaah.velcom.backend.data.commitcomparison.CommitComparison;
import de.aaaaaaah.velcom.backend.data.linearlog.LinearLog;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonCommit;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonCommitComparison;
import de.aaaaaaah.velcom.backend.restapi.util.ErrorResponseUtil;
import de.aaaaaaah.velcom.backend.util.Pair;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

/**
 * The REST API endpoint comparing commits.
 */
@Path("/commit-compare")
@Produces(MediaType.APPLICATION_JSON)
public class CommitCompareEndpoint {

	private final BenchmarkAccess benchmarkAccess;
	private final CommitAccess commitAccess;
	private final CommitComparer commitComparer;
	private final LinearLog linearLog;

	public CommitCompareEndpoint(BenchmarkAccess benchmarkAccess, CommitAccess commitAccess,
		CommitComparer commitComparer, LinearLog linearLog) {

		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
		this.commitComparer = commitComparer;
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

		if (secondCommit.isEmpty()) {
			ErrorResponseUtil.throwErrorResponse(Status.NOT_FOUND,
				"No commit with hash " + secondHashString + " found");
		}

		final Pair<Optional<Commit>, Optional<Commit>> prevAndNext = linearLog.getPrevNextCommits(
			secondCommit.get());

		Optional<Commit> firstCommit = Optional.ofNullable(firstHashString)
			.map(CommitHash::new)
			.map(hash -> commitAccess.getCommit(repoId, hash))
			.or(prevAndNext::getFirst);

		Optional<Run> firstRun = firstCommit.flatMap(benchmarkAccess::getLatestRunOf);
		Optional<Run> secondRun = secondCommit.flatMap(benchmarkAccess::getLatestRunOf);

		CommitComparison comparison = commitComparer.compare(
			firstCommit.orElse(null), firstRun.orElse(null),
			secondCommit.get(), secondRun.orElse(null)
		);
		return new GetReply(comparison,
			prevAndNext.getSecond().orElse(null));
	}

	private static class GetReply {

		private final JsonCommitComparison comparison;
		@Nullable
		private final JsonCommit next;

		public GetReply(CommitComparison comparison, @Nullable Commit next) {

			this.comparison = new JsonCommitComparison(comparison);
			this.next = (next != null) ? new JsonCommit(next) : null;
		}

		public JsonCommitComparison getComparison() {
			return comparison;
		}

		@Nullable
		public JsonCommit getNext() {
			return next;
		}
	}

}
