package de.aaaaaaah.velcom.backend.restapi.endpoints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRepo;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRun;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
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

	/**
	 * Returns all measurements that are needed to build a repository comparison graph between the
	 * given time intervals and repositories.
	 *
	 * @param request the times, repositories and other constraints
	 * @return all measurements needed to build the comparison graph
	 */
	@POST
	public PostReply post(@NotNull PostRequest request) {
		return null;
	}

	private static class PostRequest {

		private final Collection<BranchSpec> repos;
		@Nullable
		private final Long startTime;
		@Nullable
		private final Long stopTime;

		@JsonCreator
		public PostRequest(
			@JsonProperty(value = "repos", required = true) Collection<BranchSpec> repos,
			@Nullable @JsonProperty("start_time") Long startTime,
			@Nullable @JsonProperty("stop_time") Long stopTime) {

			this.repos = Objects.requireNonNull(repos);
			this.startTime = startTime;
			this.stopTime = stopTime;
		}

		public Collection<BranchSpec> getRepos() {
			return repos;
		}

		@Nullable
		public Optional<Long> getStartTime() {
			return Optional.ofNullable(startTime);
		}

		@Nullable
		public Optional<Long> getStopTime() {
			return Optional.ofNullable(stopTime);
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
