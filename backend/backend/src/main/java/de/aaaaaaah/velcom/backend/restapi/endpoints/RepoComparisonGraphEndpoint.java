package de.aaaaaaah.velcom.backend.restapi.endpoints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.aaaaaaah.velcom.backend.access.benchmark.MeasurementName;
import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitAccess;
import de.aaaaaaah.velcom.backend.access.repo.RepoAccess;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.access.repocomparison.RepoComparisonAccess;
import de.aaaaaaah.velcom.backend.access.repocomparison.timeslice.CommitGrouper;
import de.aaaaaaah.velcom.backend.access.repocomparison.timeslice.GroupByDay;
import de.aaaaaaah.velcom.backend.access.repocomparison.timeslice.GroupByHour;
import de.aaaaaaah.velcom.backend.access.repocomparison.timeslice.GroupByWeek;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonGraphRepoInfo;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

	// Difference of start and end time (in seconds) below which the hourly grouper should be used.
	private static final long HOURLY_THRESHOLD = 60 * 60 * 24 * 20; // 20 days
	// Difference of start and end time (in seconds) below which the daily grouper should be used.3
	private static final long DAILY_THRESHOLD = 60 * 60 * 24 * 7 * 20; // 20 weeks
	// If the start and end time difference is greater than this, the weekly grouper is used.

	private final CommitAccess commitAccess;
	private final RepoAccess repoAccess;
	private final RepoComparisonAccess repoComparisonAccess;

	private final CommitGrouper<Long> hourlyGrouper;
	private final CommitGrouper<Long> dailyGrouper;
	private final CommitGrouper<Long> weeklyGrouper;

	public RepoComparisonGraphEndpoint(CommitAccess commitAccess, RepoAccess repoAccess,
		RepoComparisonAccess repoComparisonAccess) {

		this.commitAccess = commitAccess;
		this.repoAccess = repoAccess;
		this.repoComparisonAccess = repoComparisonAccess;

		hourlyGrouper = new GroupByHour();
		dailyGrouper = new GroupByDay();
		weeklyGrouper = new GroupByWeek();
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
		// Make sure that the stop time comes before the start time.
		Optional<Instant> startTime = request.getStartTime().map(Instant::ofEpochSecond);
		Optional<Instant> stopTime = request.getStopTime().map(Instant::ofEpochSecond);

		if (startTime.isPresent() && stopTime.isPresent() && stopTime.get()
			.isBefore(startTime.get())) {

			Optional<Instant> tmp = startTime;
			startTime = stopTime;
			stopTime = tmp;
		}

		MeasurementName measurementName = new MeasurementName(request.getBenchmark(),
			request.getMetric());

		CommitGrouper<Long> grouper;
		if (startTime.isPresent() && stopTime.isPresent()) {
			long difference = stopTime.get().getEpochSecond() - startTime.get().getEpochSecond();
			if (difference < HOURLY_THRESHOLD) {
				grouper = hourlyGrouper;
			} else if (difference < DAILY_THRESHOLD) {
				grouper = dailyGrouper;
			} else {
				grouper = weeklyGrouper;
			}
		} else {
			grouper = weeklyGrouper; // TODO choose grouper based on returned commits?
		}

		// Need these variables because otherwise java won't compile
		Instant realStartTime = startTime.orElse(null);
		Instant realStopTime = stopTime.orElse(null);

		final List<JsonGraphRepoInfo> repoInfos = request.getRepos().stream()
			.map(branchSpec -> {
				final RepoId repoId = new RepoId(branchSpec.getRepoId());

				final Collection<Commit> commits = commitAccess.getCommitsBetween(repoId,
					branchSpec.getBranches(), realStartTime, realStopTime);

				return repoComparisonAccess.getRepoInfo(repoId, commits, measurementName, grouper);
			})
			.collect(Collectors.toUnmodifiableList());

		return new PostReply(repoInfos);
	}

	private static class PostRequest {

		private final Collection<BranchSpec> repos;
		private final Long startTime;
		private final Long stopTime;
		private final String benchmark;
		private final String metric;

		@JsonCreator
		public PostRequest(
			@JsonProperty(value = "repos", required = true) Collection<BranchSpec> repos,
			@JsonProperty(value = "start_time") Long startTime,
			@JsonProperty(value = "stop_time") Long stopTime,
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

		public Optional<Long> getStartTime() {
			return Optional.ofNullable(startTime);
		}

		public Optional<Long> getStopTime() {
			return Optional.ofNullable(stopTime);
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

		private final Collection<JsonGraphRepoInfo> repos;

		public PostReply(Collection<JsonGraphRepoInfo> repos) {
			this.repos = repos;
		}

		public Collection<JsonGraphRepoInfo> getRepos() {
			return repos;
		}
	}

}
