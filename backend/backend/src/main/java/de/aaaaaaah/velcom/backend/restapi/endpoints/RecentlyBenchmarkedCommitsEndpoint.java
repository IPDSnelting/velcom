package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.benchmark.BenchmarkAccess;
import de.aaaaaaah.velcom.backend.access.benchmark.Run;
import de.aaaaaaah.velcom.backend.data.commitcomparison.CommitComparer;
import de.aaaaaaah.velcom.backend.data.linearlog.LinearLog;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonCommitComparison;
import java.util.List;
import java.util.Optional;
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

		try (Stream<Run> recentRuns = benchmarkAccess.getRecentRuns()) {
			List<JsonCommitComparison> interestingCommits = recentRuns
				.map(run -> {
					Optional<Run> previousRun = linearLog.getPreviousCommit(run.getCommit())
						.flatMap(benchmarkAccess::getLatestRunOf);
					return commitComparer.compare(
						previousRun.map(Run::getCommit).orElse(null),
						previousRun.orElse(null),
						run.getCommit(),
						run
					);
				})
				.filter(comparison -> !significantOnly || comparison.isSignificant())
				.limit(amount)
				.map(JsonCommitComparison::new)
				.collect(Collectors.toUnmodifiableList());

			return new GetReply(interestingCommits);
		}
	}

	private static class GetReply {

		private final List<JsonCommitComparison> commits;

		public GetReply(List<JsonCommitComparison> commits) {
			this.commits = commits;
		}

		public List<JsonCommitComparison> getCommits() {
			return commits;
		}
	}
}
