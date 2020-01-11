package de.aaaaaaah.velcom.backend.restapi.endpoints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitAccess;
import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.restapi.RepoUser;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonCommit;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonWorker;
import de.aaaaaaah.velcom.backend.runner.Dispatcher;
import io.dropwizard.auth.Auth;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * The REST API endpoint allowing to interact with the {@link Queue}.
 */
@Path("/queue")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class QueueEndpoint {

	private final CommitAccess commitAccess;
	private final Queue queue;
	private final Dispatcher dispatcher;

	public QueueEndpoint(CommitAccess commitAccess, Queue queue, Dispatcher dispatcher) {
		this.commitAccess = commitAccess;
		this.queue = queue;
		this.dispatcher = dispatcher;
	}

	/**
	 * @return all commits that are currently queued and all active runners.
	 */
	@GET
	public GetReply get() {
		List<JsonCommit> tasks = queue.viewAllCurrentTasks().stream()
			.map(JsonCommit::new)
			.collect(Collectors.toUnmodifiableList());

		List<JsonWorker> workers = dispatcher.getKnownRunners().stream()
			.map(JsonWorker::new)
			.collect(Collectors.toUnmodifiableList());

		return new GetReply(tasks, workers);
	}

	/**
	 * Adds a new commit to the queue as a manual task.
	 *
	 * @param postRequest the commit to add
	 */
	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	@POST
	public void post(@Auth Optional<RepoUser> user, @NotNull PostRequest postRequest) {
		RepoId repoId = new RepoId(postRequest.getRepoId());
		RepoUser.guardRepoAccess(user, repoId);

		CommitHash commitHash = new CommitHash(postRequest.getCommitHash());

		Commit commit = commitAccess.getCommit(repoId, commitHash);
		queue.addManualTask(commit);
	}

	/**
	 * Deletes a commit from the queue.
	 *
	 * @param repoUuid the id of the repo
	 * @param hashString the hash of the commit
	 */
	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	@DELETE
	public void delete(
		@Auth Optional<RepoUser> user,
		@NotNull @QueryParam("repo_id") UUID repoUuid,
		@NotNull @QueryParam("commit_hash") String hashString) {

		RepoId repoId = new RepoId(repoUuid);
		RepoUser.guardRepoAccess(user, repoId);

		CommitHash commitHash = new CommitHash(hashString);

		queue.abortTask(repoId, commitHash);
		dispatcher.abort(commitHash, repoId);
	}

	private static class GetReply {

		private final Collection<JsonCommit> tasks;
		private final Collection<JsonWorker> workers;

		public GetReply(Collection<JsonCommit> tasks, Collection<JsonWorker> workers) {
			this.tasks = tasks;
			this.workers = workers;
		}

		public Collection<JsonCommit> getTasks() {
			return tasks;
		}

		public Collection<JsonWorker> getWorkers() {
			return workers;
		}
	}

	private static class PostRequest {

		private final UUID repoId;
		private final String commitHash;

		@JsonCreator
		public PostRequest(
			@JsonProperty(value = "repo_id", required = true) UUID repoId,
			@JsonProperty(value = "commit_hash", required = true) String commitHash) {

			this.repoId = Objects.requireNonNull(repoId);
			this.commitHash = Objects.requireNonNull(commitHash);
		}

		public UUID getRepoId() {
			return repoId;
		}

		public String getCommitHash() {
			return commitHash;
		}
	}
}
