package de.aaaaaaah.velcom.backend.restapi.endpoints;

import static java.util.stream.Collectors.toList;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.SearchRunDescription;
import de.aaaaaaah.velcom.backend.access.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.access.repoaccess.RepoReadAccess;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.SearchBranchDescription;
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
	private final RepoReadAccess repoAccess;

	public SearchEndpoint(BenchmarkReadAccess benchmarkAccess, CommitReadAccess commitAccess,
		RepoReadAccess repoAccess) {

		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
		this.repoAccess = repoAccess;
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

		List<BranchInfo> branches = repoAccess
			.searchBranches(limit, repoId.orElse(null), query)
			.stream()
			.map(BranchInfo::new)
			.collect(toList());

		limit = Math.max(0, limit - branches.size());

		List<CommitInfo> commits = commitAccess
			.searchCommits(limit, repoId.orElse(null), query)
			.stream()
			.map(pair -> new CommitInfo(pair.getFirst(), pair.getSecond()))
			.collect(toList());

		limit = Math.max(0, limit - commits.size());

		List<RunInfo> runs = benchmarkAccess
			.searchRuns(limit, repoId.orElse(null), query)
			.stream()
			.map(RunInfo::new)
			.collect(toList());

		return new SearchGetReply(commits, runs, branches);
	}

	private static class SearchGetReply {

		public final List<CommitInfo> commits;
		public final List<RunInfo> runs;
		public final List<BranchInfo> branches;

		public SearchGetReply(List<CommitInfo> commits, List<RunInfo> runs, List<BranchInfo> branches) {
			this.commits = commits;
			this.runs = runs;
			this.branches = branches;
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
		public final String commitSummary;
		@Nullable
		public final String tarDescription;
		public final long startTime;
		public final long stopTime;

		public RunInfo(SearchRunDescription run) {

			this.id = run.getId().getIdAsString();
			this.repoId = run.getRepoId().map(RepoId::getIdAsString).orElse(null);
			this.commitSummary = run.getCommitSummary().orElse(null);
			this.tarDescription = run.getTarDescription().orElse(null);
			this.startTime = run.getStartTime().getEpochSecond();
			this.stopTime = run.getStopTime().getEpochSecond();
		}
	}

	private static class BranchInfo {

		public final String repoId;
		public final String name;
		public final String commitHash;
		public final String commitSummary;
		public final boolean hasRun;

		public BranchInfo(SearchBranchDescription branch) {
			this.repoId = branch.getRepoId().getIdAsString();
			this.name = branch.getName().getName();
			this.commitHash = branch.getCommitHash().getHash();
			this.commitSummary = branch.getCommitSummary();
			this.hasRun = branch.hasRun();
		}
	}
}
