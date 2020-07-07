package de.aaaaaaah.velcom.backend.restapi.endpoints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.aaaaaaah.velcom.backend.access.entities.Task;
import de.aaaaaaah.velcom.backend.access.entities.TaskId;
import de.aaaaaaah.velcom.backend.access.exceptions.NoSuchTaskException;
import de.aaaaaaah.velcom.backend.data.linearlog.LinearLog;
import de.aaaaaaah.velcom.backend.data.linearlog.LinearLogException;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.RepoReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Branch;
import de.aaaaaaah.velcom.backend.access.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.restapi.RepoUser;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonTask;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonWorker;
import de.aaaaaaah.velcom.backend.runner.Dispatcher;
import io.dropwizard.auth.Auth;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The REST API endpoint allowing to interact with the {@link Queue}.
 */
@Path("/queue")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class QueueEndpoint {

	private static final Logger LOGGER = LoggerFactory.getLogger(QueueEndpoint.class);

	private final CommitReadAccess commitAccess;
	private final RepoReadAccess repoReadAccess;
	private final Queue queue;
	private final Dispatcher dispatcher;
	private final LinearLog linearLog;

	public QueueEndpoint(CommitReadAccess commitAccess, Queue queue, Dispatcher dispatcher,
		LinearLog linearLog, RepoReadAccess repoReadAccess) {
		this.commitAccess = commitAccess;
		this.queue = queue;
		this.dispatcher = dispatcher;
		this.linearLog = linearLog;
		this.repoReadAccess = repoReadAccess;
	}

	/**
	 * @return all commits that are currently queued and all active runners.
	 */
	@GET
	public GetReply get() {
		List<JsonTask> tasks = queue.getTasksSorted().stream()
			.map(JsonTask::new)
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
	@POST
	public void post(@Auth RepoUser user, @NotNull PostRequest postRequest) {
		RepoId repoId = new RepoId(postRequest.getRepoId());
		user.guardAdminAccess();

		CommitHash commitHash = new CommitHash(postRequest.getCommitHash());

		String author = "admin of repo " + repoId.getId().toString();

		queue.addCommits(author, repoId, List.of(commitHash));

		if (postRequest.isIncludeUpwards()) {
			List<BranchName> branchNames = repoReadAccess.getBranches(repoId)
				.stream()
				.map(Branch::getName)
				.collect(Collectors.toList());

			try {
				// Do we really need to load complete commit objects?
				List<CommitHash> commits = linearLog.walk(repoId, branchNames)
					.takeWhile(it -> !it.getHash().equals(commitHash))
					.map(Commit::getHash)
					.collect(Collectors.toList());

				queue.addCommits(author, repoId, commits);
			} catch (LinearLogException e) {
				LOGGER.error("Error fetching linear log for " + repoId + " - " + commitHash, e);
			}
		}
	}

	/**
	 * Deletes a commit from the queue.
	 *
	 * @param user user who authored this delete
	 * @param taskId the id of the task to delete
	 *
	 * @throws NoSuchTaskException if no task with the given id exists
	 */
	@DELETE
	public void delete(@Auth RepoUser user, @NotNull @QueryParam("task_id") UUID taskId)
		throws NoSuchTaskException {

		Task task = queue.getTaskById(new TaskId(taskId));

		if (task.getSource().getLeft().isPresent()) {
			RepoId repoId = task.getSource().getLeft().get().getRepoId();
			user.guardRepoAccess(repoId);
		}
		else {
			// task is tar
			user.guardAdminAccess();
		}

		queue.abortTaskProcess(new TaskId(taskId));
	}

	private static class GetReply {

		private final Collection<JsonTask> tasks;
		private final Collection<JsonWorker> workers;

		public GetReply(Collection<JsonTask> tasks, Collection<JsonWorker> workers) {
			this.tasks = tasks;
			this.workers = workers;
		}

		public Collection<JsonTask> getTasks() {
			return tasks;
		}

		public Collection<JsonWorker> getWorkers() {
			return workers;
		}
	}

	private static class PostRequest {

		private final UUID repoId;
		private final String commitHash;
		private final boolean includeUpwards;

		@JsonCreator
		public PostRequest(
			@JsonProperty(value = "repo_id", required = true) UUID repoId,
			@JsonProperty(value = "commit_hash", required = true) String commitHash,
			@JsonProperty(value = "include_all_commits_after", required = false) Boolean includeUpwards) {

			this.repoId = Objects.requireNonNull(repoId);
			this.commitHash = Objects.requireNonNull(commitHash);
			this.includeUpwards = includeUpwards == null ? false : includeUpwards;
		}

		public UUID getRepoId() {
			return repoId;
		}

		public String getCommitHash() {
			return commitHash;
		}

		public boolean isIncludeUpwards() {
			return includeUpwards;
		}
	}
}
