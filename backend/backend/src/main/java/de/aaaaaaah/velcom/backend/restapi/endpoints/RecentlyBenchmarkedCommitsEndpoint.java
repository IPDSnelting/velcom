package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.benchmark.BenchmarkAccess;
import de.aaaaaaah.velcom.backend.access.benchmark.Run;
import de.aaaaaaah.velcom.backend.access.benchmark.RunId;
import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.data.commitcomparison.CommitComparer;
import de.aaaaaaah.velcom.backend.data.commitcomparison.CommitComparison;
import de.aaaaaaah.velcom.backend.data.linearlog.LinearLog;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonCommitComparison;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

	private static final int COMMITS_PER_STEP = 100;

	private final BenchmarkAccess benchmarkAccess;
	private final CommitComparer commitComparer;
	private final LinearLog linearLog;

	public RecentlyBenchmarkedCommitsEndpoint(BenchmarkAccess benchmarkAccess,
		CommitComparer commitComparer, LinearLog linearLog) {

		this.benchmarkAccess = benchmarkAccess;
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

		final Stream<Run> recentRuns = getRecentRunsStream();

		List<CommitComparison> interestingCommits = recentRuns
			.map(run -> {
				Commit runCommit = run.getCommit().orElse(null);
				if (runCommit == null) {
					return null;
				}

				Commit previousCommit = linearLog.getPreviousCommit(runCommit).orElse(null);
				Run previousRun = previousCommit == null ? null :
					benchmarkAccess.getLatestRunOf(previousCommit).orElse(null);

				return commitComparer.compare(
					previousCommit,
					previousRun,
					runCommit,
					run
				);
			})
			.filter(Objects::nonNull)
			.filter(comparison -> !significantOnly || comparison.isSignificant())
			.limit(amount)
			.collect(Collectors.toUnmodifiableList());

		return new GetReply(interestingCommits);
	}

	private Stream<Run> getRecentRunsStream() {
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
