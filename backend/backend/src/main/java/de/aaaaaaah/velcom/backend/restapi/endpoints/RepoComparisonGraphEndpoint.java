package de.aaaaaaah.velcom.backend.restapi.endpoints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementName;
import de.aaaaaaah.velcom.backend.access.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.data.repocomparison.ComparisonGraph;
import de.aaaaaaah.velcom.backend.data.repocomparison.GraphEntry;
import de.aaaaaaah.velcom.backend.data.repocomparison.RepoComparison;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonGraphEntry;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonGraphRepoInfo;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	private final RepoComparison repoComparison;

	public RepoComparisonGraphEndpoint(RepoComparison repoComparison) {
		this.repoComparison = repoComparison;
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

		// Collect all branches and repos
		final Map<RepoId, List<BranchName>> repoBranches = new HashMap<>();

		for (BranchSpec branchSpec : request.getRepos()) {
			final RepoId repoId = new RepoId(branchSpec.getRepoId());
			final List<BranchName> repoBranchNames = branchSpec.getBranches()
				.stream()
				.map(BranchName::fromName)
				.collect(Collectors.toList());

			repoBranches.put(repoId, repoBranchNames);
		}

		final ComparisonGraph graph = this.repoComparison.generateGraph(
			measurementName, repoBranches, startTime.orElse(null), stopTime.orElse(null)
		);

		final List<JsonGraphRepoInfo> jsonGraphData = graph.getData().stream()
			.map(repoGraphData -> new JsonGraphRepoInfo(
				repoGraphData.getRepoId(),
				convertEntriesToJson(repoGraphData.getEntries()),
				repoGraphData.getInterpretation(),
				repoGraphData.getUnit()))
			.collect(Collectors.toList());

		return new PostReply(jsonGraphData);
	}

	private List<JsonGraphEntry> convertEntriesToJson(Collection<GraphEntry> entries) {
		return entries.stream().map(JsonGraphEntry::new).collect(Collectors.toList());
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
