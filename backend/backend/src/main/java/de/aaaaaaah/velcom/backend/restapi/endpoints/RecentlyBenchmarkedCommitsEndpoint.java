package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.data.commitcomparison.CommitComparer;
import de.aaaaaaah.velcom.backend.data.commitcomparison.CommitComparison;
import de.aaaaaaah.velcom.backend.data.linearlog.LinearLog;
import de.aaaaaaah.velcom.backend.data.recentbenchmarks.RecentBenchmarkCollector;
import de.aaaaaaah.velcom.backend.newaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.RepoReadAccess;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonCommitComparison;
import java.util.List;
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

	private final RecentBenchmarkCollector collector;

	public RecentlyBenchmarkedCommitsEndpoint(RepoReadAccess repoAccess,
		BenchmarkReadAccess benchmarkAccess,
		CommitReadAccess commitAccess, CommitComparer commitComparer, LinearLog linearLog) {

		this.collector = new RecentBenchmarkCollector(repoAccess, benchmarkAccess, commitAccess,
			linearLog, commitComparer);
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

		List<CommitComparison> interestingCommits = collector.collect(amount, significantOnly);

		return new GetReply(interestingCommits);
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
