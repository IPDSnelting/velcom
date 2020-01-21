package de.aaaaaaah.velcom.backend.restapi.endpoints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.aaaaaaah.velcom.backend.access.benchmark.MeasurementName;
import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitAccess;
import de.aaaaaaah.velcom.backend.access.repo.RepoAccess;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.data.reducedlog.ReducedLog;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRepo;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRun;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * The REST API endpoint providing data for a repo comparison graph.
 */
@Path("/repo-comparison-graph")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RepoComparisonGraphEndpoint {

	private final CommitAccess commitAccess;
	private final RepoAccess repoAccess;
	private final ReducedLog reducedLog;

	public RepoComparisonGraphEndpoint(CommitAccess commitAccess, RepoAccess repoAccess,
		ReducedLog reducedLog) {

		this.commitAccess = commitAccess;
		this.repoAccess = repoAccess;
		this.reducedLog = reducedLog;
	}

	/**
	 * Returns all measurements that are needed to build a repository comparison graph between the
	 * given time intervals and repositories.
	 *
	 * @param request the times, repositories and other constraints
	 * @return all measurements needed to build the comparison graph
	 */
	@POST
	public PostReply post(@NotNull PostRequest request) {
		Instant startTime = Instant.ofEpochSecond(request.getStartTime());
		Instant stopTime = Instant.ofEpochSecond(request.getStopTime());
		MeasurementName measurementName = new MeasurementName(request.getBenchmark(),
			request.getMetric());

		final List<RepoInfo> repoInfos = request.getRepos().stream()
			.map(branchSpec -> {
				final RepoId repoId = new RepoId(branchSpec.getRepoId());
				final JsonRepo repo = new JsonRepo(repoAccess.getRepo(repoId));

				final Collection<Commit> commits = commitAccess.getCommitsBetween(repoId,
					branchSpec.getBranches(), startTime, stopTime);

				final List<JsonRun> reducedRuns = reducedLog.reduce(commits, measurementName)
					.stream()
					.map(JsonRun::new)
					.collect(Collectors.toUnmodifiableList());

				return new RepoInfo(repo, reducedRuns);
			})
			.collect(Collectors.toUnmodifiableList());

		return new PostReply(repoInfos);
	}

	private static class PostRequest {

		private final Collection<BranchSpec> repos;
		private final long startTime;
		private final long stopTime;
		private final String benchmark;
		private final String metric;

		@JsonCreator
		public PostRequest(
			@JsonProperty(value = "repos", required = true) Collection<BranchSpec> repos,
			@JsonProperty(value = "start_time", required = true) long startTime,
			@JsonProperty(value = "stop_time", required = true) long stopTime,
			@JsonProperty(value = "benchmark", required = true) String benchmark,
			@JsonProperty(value = "metric", required = true) String metric) {

			this.repos = Objects.requireNonNull(repos);
			this.startTime = startTime;
			this.stopTime = stopTime;
			this.benchmark = benchmark;
			this.metric = metric;
		}

		public Collection<BranchSpec> getRepos() {
			return repos;
		}

		public long getStartTime() {
			return startTime;
		}

		public long getStopTime() {
			return stopTime;
		}

		public String getBenchmark() {
			return benchmark;
		}

		public String getMetric() {
			return metric;
		}
	}

	private static class BranchSpec {

		private final UUID repoId;
		private final Collection<String> branches;

		@JsonCreator
		public BranchSpec(
			@JsonProperty(value = "repo_id", required = true) UUID repoId,
			@JsonProperty(value = "branches", required = true) Collection<String> branches) {

			this.repoId = Objects.requireNonNull(repoId);
			this.branches = Objects.requireNonNull(branches);
		}

		public UUID getRepoId() {
			return repoId;
		}

		public Collection<String> getBranches() {
			return branches;
		}
	}

	private static class PostReply {

		private final Collection<RepoInfo> repos;

		public PostReply(Collection<RepoInfo> repos) {
			this.repos = repos;
		}

		public Collection<RepoInfo> getRepos() {
			return repos;
		}
	}

	private static class RepoInfo {

		private final JsonRepo repo;
		private final List<JsonRun> runs;

		public RepoInfo(JsonRepo repo, List<JsonRun> runs) {
			this.repo = repo;
			this.runs = runs;
		}

		public JsonRepo getRepo() {
			return repo;
		}

		public List<JsonRun> getRuns() {
			return runs;
		}
	}

}
