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
import java.util.ArrayList;
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

	public static final int DEFAULT_AMOUNT = 100;
	public static final int DEFAULT_SKIP = 0;

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
	 * @param amount how many commits to return
	 * @param skip how many commits to skip before beginning to return commits
	 * @param relativeToString commits are returned relative to this commit
	 * @return the selected commits
	 * @throws LinearLogException if the repository could not be brought into a linear shape for
	 * 	some reason
	 */
	@GET
	public GetReply get(
		@NotNull @QueryParam("repo_id") UUID repoUuid,
		@Min(0) @DefaultValue("" + DEFAULT_AMOUNT) @QueryParam("amount") int amount,
		@Min(0) @DefaultValue("" + DEFAULT_SKIP) @QueryParam("skip") int skip,
		@QueryParam("relative_to") String relativeToString)
		throws LinearLogException {

		final RepoId repoId = new RepoId(repoUuid);
		final Repo repo = repoAccess.getRepo(repoId);
		final Optional<CommitHash> relativeTo = Optional.ofNullable(relativeToString)
			.map(CommitHash::new);

		try (Stream<Commit> stream = linearLog.walkBranches(repoId, repo.getTrackedBranches())) {
			Iterator<Commit> iterator = stream.iterator();

			// 'offset' stores the offset of 'currentCommit' and is increased each time
			// 'iterator.next()' is called.
			Commit currentCommit = null;
			int offset = -1;
			boolean foundRelativeTo = false;

			// 1. Find the commit that relative_to refers to
			while (iterator.hasNext()) {
				currentCommit = iterator.next();
				offset++;
				if (relativeTo.map(currentCommit.getHash()::equals).orElse(true)) {
					foundRelativeTo = true;
					break;
				}
			}
			if (!foundRelativeTo) {
				return new GetReply(List.of(), 0);
			}
			// We can be sure that 'currentCommit' is not null at this point, because it contains
			// the commit that relativeTo refers to, and 'offset' contains its offset.

			// 2. Skip as many commits as necessary
			for (int i = 0; i < skip; i++) {
				if (iterator.hasNext()) {
					currentCommit = iterator.next();
					offset++;
				} else {
					return new GetReply(List.of(), 0);
				}
			}
			// Now, 'currentCommit' contains the first commit to be returned, and 'offset' contains
			// its offset.

			// 3. Take 'amount' commits. From this point on, 'currentCommit' is not used any more.
			List<Commit> commits = new ArrayList<>();
			commits.add(currentCommit);
			for (int i = 0; i < amount - 1; i++) {
				if (iterator.hasNext()) {
					commits.add(iterator.next());
				}
			}

			// 4. We usually need one more commit, for the CommitComparison of the last commit
			boolean omitLastCommit = false;
			if (iterator.hasNext()) {
				omitLastCommit = true;
				commits.add(iterator.next());
			}

			// 5. Get all the runs.
			List<CommitHash> commitHashes = commits.stream()
				.map(Commit::getHash)
				.collect(Collectors.toUnmodifiableList());
			Map<CommitHash, Run> runs = benchmarkAccess.getLatestRuns(repoId, commitHashes);

			// 6. Compile the commit comparisons.
			List<Commit> commitList = new ArrayList<>(commits);
			List<CommitComparison> commitComparisons = new ArrayList<>();
			for (int i = 0; i < commitList.size() - 1; i++) {
				Commit from = commitList.get(i + 1);
				Commit to = commitList.get(i);
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

			// And the deed is done. Iterators are not nice to work with, but java streams don't
			// easily support zipping, and for a one-off like this, it was easier to just use
			// iterators.
			return new GetReply(commitComparisons, offset);
		}
	}

	private static class GetReply {

		private final List<JsonCommitComparison> commits;
		private final int offset;

		public GetReply(List<CommitComparison> commits, int offset) {
			this.commits = commits.stream()
				.map(JsonCommitComparison::new)
				.collect(Collectors.toUnmodifiableList());
			this.offset = offset;
		}

		public List<JsonCommitComparison> getCommits() {
			return commits;
		}

		public int getOffset() {
			return offset;
		}
	}

}
