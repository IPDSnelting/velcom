package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.data.commitcomparison.CommitComparer;
import de.aaaaaaah.velcom.backend.data.commitcomparison.CommitComparison;
import de.aaaaaaah.velcom.backend.data.linearlog.LinearLog;
import de.aaaaaaah.velcom.backend.data.linearlog.LinearLogException;
import de.aaaaaaah.velcom.backend.newaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.RepoReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.newaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.entities.Run;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonCommitComparison;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

	public static final int DEFAULT_BEFORE = 10;
	public static final int DEFAULT_AFTER = 10;

	private final BenchmarkReadAccess benchmarkAccess;
	private final RepoReadAccess repoAccess;
	private final LinearLog linearLog;
	private final CommitComparer comparer;

	public CommitHistoryEndpoint(BenchmarkReadAccess benchmarkAccess, RepoReadAccess repoAccess,
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
	 * @param before how many (older) commits before the current commit to return
	 * @param after how many (newer) commits after the current commit to return
	 * @param relativeToString commits are returned relative to this commit
	 * @return the selected commits
	 * @throws LinearLogException if the repository could not be brought into a linear shape for
	 * 	some reason
	 */
	@GET
	public GetReply get(
		@NotNull @QueryParam("repo_id") UUID repoUuid,
		@Min(0) @DefaultValue("" + DEFAULT_BEFORE) @QueryParam("before") int before,
		@Min(0) @DefaultValue("" + DEFAULT_AFTER) @QueryParam("after") int after,
		@QueryParam("relative_to") String relativeToString)
		throws LinearLogException {

		System.out.println(
			"before: " + before + ", after: " + after + ", relative_to: " + relativeToString);

		final RepoId repoId = new RepoId(repoUuid);
		final Repo repo = repoAccess.getRepo(repoId);
		final Optional<CommitHash> relativeTo = Optional.ofNullable(relativeToString)
			.map(CommitHash::new);

		Deque<Commit> commits = new ArrayDeque<>();

		try (Stream<Commit> stream = linearLog.walkBranches(repoId, repo.getTrackedBranches())) {
			Iterator<Commit> iterator = stream.iterator();

			// This loop keeps "after + 1" elements in the "commits" deque.
			while (iterator.hasNext()) {
				Commit commit = iterator.next();
				commits.addLast(commit);

				// The "+ 1" is because the "relativeTo" commit does not count towards the "after"
				// or "before" numbers.
				if (commits.size() > after + 1) {
					commits.removeFirst();
				}

				// If no "relativeTo" commit is given, it defaults to the newest commit.
				if (relativeTo.map(commit.getHash()::equals).orElse(true)) {
					break;
				}
			}

			// This loop then fills in the "before" commits
			for (int i = 0; i < before; i++) {
				if (iterator.hasNext()) {
					commits.addLast(iterator.next());
				} else {
					break;
				}
			}

			// We usually need one more commit, for the CommitComparison of the last commit
			boolean omitLastCommit = false;
			if (iterator.hasNext()) {
				omitLastCommit = true;
				commits.addLast(iterator.next());
			}

			// Now, get all the runs.
			List<CommitHash> commitHashes = commits.stream()
				.map(Commit::getHash)
				.collect(Collectors.toUnmodifiableList());
			Map<CommitHash, Run> runs = benchmarkAccess.getLatestRuns(repoId, commitHashes);

			// And compile the commit comparisons.
			List<Commit> commitList = new ArrayList<>(commits);
			List<CommitComparison> commitComparisons = new ArrayList<>();
			for (int i = 0; i < commitList.size() - 1; i++) {
				Commit from = commitList.get(i);
				Commit to = commitList.get(i + 1);
				CommitComparison comparison = comparer.compare(from, runs.get(from.getHash()), to,
					runs.get(to.getHash()));
				commitComparisons.add(comparison);
			}

			if (!omitLastCommit) { // There is at least one commit in the list
				Commit to = commitList.get(commitList.size() - 1);
				CommitComparison comparison = comparer.compare(null, null, to,
					runs.get(to.getHash()));
				commitComparisons.add(comparison);
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
