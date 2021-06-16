package de.aaaaaaah.velcom.backend.restapi.endpoints;

import static java.util.stream.Collectors.toList;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.ShortRunDescription;
import de.aaaaaaah.velcom.backend.access.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/search")
@Produces(MediaType.APPLICATION_JSON)
public class SearchEndpoint {

	private static final int SEARCH_LIMIT = 500;

	private final BenchmarkReadAccess benchmarkAccess;
	private final CommitReadAccess commitAccess;

	public SearchEndpoint(BenchmarkReadAccess benchmarkAccess, CommitReadAccess commitAccess) {
		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
	}

	@GET
	@Timed(histogram = true)
	public SearchGetReply get(
		@QueryParam("limit") @Nullable Integer maybeLimit,
		@QueryParam("repo_id") @Nullable UUID repoUuid,
		@QueryParam("query") @NotNull String query
	) {
		Optional<RepoId> repoId = Optional.ofNullable(repoUuid).map(RepoId::new);
		int limit = Optional.ofNullable(maybeLimit).orElse(SEARCH_LIMIT);

		List<CommitInfo> commits = commitAccess
			.searchCommits(limit, repoId.orElse(null), query)
			.stream()
			.map(pair -> new CommitInfo(pair.getFirst(), pair.getSecond()))
			.collect(toList());

		limit = Math.max(0, limit - commits.size());

		List<RunInfo> runs = benchmarkAccess
			.searchRuns(limit, repoId.orElse(null), query)
			.stream()
			.map(pair -> new RunInfo(pair.getFirst(), pair.getSecond().orElse(null)))
			.collect(toList());

		return new SearchGetReply(commits, runs);
	}

	private static class SearchGetReply {

		public final List<CommitInfo> commits;
		public final List<RunInfo> runs;

		public SearchGetReply(List<CommitInfo> commits, List<RunInfo> runs) {
			this.commits = commits;
			this.runs = runs;
		}
	}

	private static class CommitInfo {

		public final String repoId;
		public final String hash;
		public final String author;
		public final long authorDate;
		public final String committer;
		public final long committerDate;
		public final String summary;
		public final boolean hasRun;

		public CommitInfo(Commit commit, boolean hasRun) {

			this.repoId = commit.getRepoId().getIdAsString();
			this.hash = commit.getHashAsString();
			this.author = commit.getAuthor();
			this.authorDate = commit.getAuthorDate().getEpochSecond();
			this.committer = commit.getCommitter();
			this.committerDate = commit.getCommitterDate().getEpochSecond();
			this.summary = commit.getSummary();
			this.hasRun = hasRun;
		}
	}

	private static class RunInfo {

		public final String id;
		@Nullable
		public final String repoId;
		@Nullable
		public final String commitHash;
		@Nullable
		public final String commitSummary;
		@Nullable
		public final String tarDescription;

		public RunInfo(ShortRunDescription run, @Nullable RepoId repoId) {

			this.id = run.getId().getIdAsString();
			this.repoId = Optional.ofNullable(repoId).map(RepoId::getIdAsString).orElse(null);
			this.commitHash = run.getCommitHash().orElse(null);
			this.commitSummary = run.getCommitSummary().orElse(null);
			this.tarDescription = run.getTarDescription().orElse(null);
		}
	}
}
