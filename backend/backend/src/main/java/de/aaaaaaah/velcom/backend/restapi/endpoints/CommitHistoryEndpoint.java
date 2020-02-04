package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.benchmark.BenchmarkAccess;
import de.aaaaaaah.velcom.backend.access.benchmark.Run;
import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.Repo;
import de.aaaaaaah.velcom.backend.access.repo.RepoAccess;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.data.commitcomparison.CommitComparer;
import de.aaaaaaah.velcom.backend.data.commitcomparison.CommitComparison;
import de.aaaaaaah.velcom.backend.data.linearlog.LinearLog;
import de.aaaaaaah.velcom.backend.data.linearlog.LinearLogException;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonCommitComparison;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

	private final BenchmarkAccess benchmarkAccess;
	private final RepoAccess repoAccess;
	private final LinearLog linearLog;
	private final CommitComparer comparer;

	public CommitHistoryEndpoint(BenchmarkAccess benchmarkAccess, RepoAccess repoAccess,
		LinearLog linearLog, CommitComparer comparer) {

		this.benchmarkAccess = benchmarkAccess;
		this.repoAccess = repoAccess;
		this.linearLog = linearLog;
		this.comparer = comparer;
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

		final RepoId repoId = new RepoId(repoUuid);
		final Repo repo = repoAccess.getRepo(repoId);

		try (Stream<Commit> stream = linearLog.walkBranches(repo, repo.getTrackedBranches())) {
			final List<Commit> commits = stream
				.skip(skip)
				.limit(amount + 1)
				.collect(Collectors.toUnmodifiableList());

			final List<CommitHash> commitHashes = commits.stream()
				.map(Commit::getHash)
				.collect(Collectors.toUnmodifiableList());

			final Map<CommitHash, Run> runs = benchmarkAccess.getRuns(
				benchmarkAccess.getLatestRunIds(repoId, commitHashes)).stream()
				.collect(Collectors.toMap(
					Run::getCommitHash,
					run -> run
				));

			List<CommitComparison> commitComparisons = new ArrayList<>();

			for (int i = 0; i < commits.size() - 1; i++) {
				Commit secondCommit = commits.get(i);
				Commit firstCommit = commits.get(i + 1);

				commitComparisons.add(comparer.compare(
					firstCommit,
					runs.get(firstCommit.getHash()),
					secondCommit,
					runs.get(secondCommit.getHash())
				));
			}

			// Usually, we get (amount + 1) commits. But if we get less than that, we're probably at
			// the end of the repo and thus missing commits. In that case, we need to add the last
			// commit manually.
			if (!commits.isEmpty() && commits.size() < amount + 1) {
				final Commit firstCommit = commits.get(commits.size() - 1);
				commitComparisons.add(comparer.compare(
					null,
					null,
					firstCommit,
					runs.get(firstCommit.getHash())
				));
			}

			return new GetReply(commitComparisons);
		}
	}

	private static class GetReply {

		private final List<JsonCommitComparison> commits;

		public GetReply(List<CommitComparison> commits) {
			this.commits = commits.stream()
				.map(JsonCommitComparison::new)
				.collect(Collectors.toUnmodifiableList());
		}

		public List<JsonCommitComparison> getCommits() {
			return commits;
		}

	}

}
