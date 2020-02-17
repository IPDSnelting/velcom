package de.aaaaaaah.velcom.backend.restapi.endpoints;

import static java.util.stream.Collectors.toList;

import de.aaaaaaah.velcom.backend.data.commitcomparison.CommitComparer;
import de.aaaaaaah.velcom.backend.data.commitcomparison.CommitComparison;
import de.aaaaaaah.velcom.backend.data.linearlog.LinearLog;
import de.aaaaaaah.velcom.backend.newaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.newaccess.entities.Run;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonCommitComparison;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * The REST API endpoint providing all recently benchmarked commits for some kind of overview page.
 */
@Path("/recently-benchmarked-commits")
@Produces(MediaType.APPLICATION_JSON)
public class RecentlyBenchmarkedCommitsEndpoint {

	private static final int COMMITS_PER_STEP = 400;
	private static final int MAX_STEPS = 10;

	private final BenchmarkReadAccess benchmarkAccess;
	private final CommitReadAccess commitAccess;
	private final CommitComparer commitComparer;
	private final LinearLog linearLog;

	public RecentlyBenchmarkedCommitsEndpoint(BenchmarkReadAccess benchmarkAccess,
		CommitReadAccess commitAccess, CommitComparer commitComparer, LinearLog linearLog) {

		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
		this.commitComparer = commitComparer;
		this.linearLog = linearLog;
	}

	/**
	 * Returns all recently benchmarked commits, filtering them if requested.
	 *
	 * @param amount the amount of commits to return
	 * @param significantOnly whether to only return significant commits
	 * @return all recently benchmarked commits respecting the two parameters
	 */
	@GET
	public GetReply get(
		@NotNull @QueryParam("amount") Integer amount,
		@DefaultValue("false") @QueryParam("significant_only") boolean significantOnly) {

		List<CommitComparison> interestingCommits = new ArrayList<>();

		for (int step = 0; step < MAX_STEPS; step++) {
			if (interestingCommits.size() >= amount) { break; }

			List<Run> runs = benchmarkAccess.getRecentRuns(
				step * COMMITS_PER_STEP, COMMITS_PER_STEP
			);

			// Transform to comparisons
			List<CommitComparison> comparisons = runs.stream()
				.map(this::compareWithPrevious)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toCollection(ArrayList::new));

			// Filter out commits if significantOnly
			if (significantOnly) {
				Iterator<CommitComparison> iterator = comparisons.iterator();
				while (iterator.hasNext()) {
					CommitComparison next = iterator.next();

					if (!next.isSignificant()) {
						iterator.remove();
					}
				}
			}

			interestingCommits.addAll(comparisons);
		}

		return new GetReply(interestingCommits);
	}

	private Optional<CommitComparison> compareWithPrevious(Run run) {
		Commit commit = commitAccess.getCommit(run).orElse(null);

		if (commit == null) {
			return Optional.empty();
		}

		Optional<Commit> previousCommit = linearLog.getPreviousCommit(commit);

		Optional<Run> previousRun = previousCommit
			.flatMap(prev -> benchmarkAccess.getLatestRun(run.getRepoId(), prev.getHash()));

		return Optional.of(commitComparer.compare(
			previousCommit.orElse(null),
			previousRun.orElse(null),
			commit,
			run
		));
	}

	/*private Stream<Run> getRecentRunsStream() {
		// The try-with-resources makes this stream not look as nice as it could otherwise look.
		// But we neeeeed them...
		return Stream.iterate(0, i -> i + COMMITS_PER_STEP)
			.map(i -> {
				try (Stream<RunId> stream = benchmarkAccess.getRecentRunIds()) {
					return stream
						.skip(i)
						.limit(COMMITS_PER_STEP + 1)
						.collect(Collectors.toUnmodifiableList());
				}
			})
			.map(benchmarkAccess::getRuns)
			.flatMap(Collection::stream);
	}*/

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
