package de.aaaaaaah.velcom.backend.restapi.endpoints;

import static de.aaaaaaah.velcom.backend.util.MetricsUtils.timer;

import com.codahale.metrics.annotation.Timed;
import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.RepoReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.Repo;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.Task;
import de.aaaaaaah.velcom.backend.access.entities.TaskId;
import de.aaaaaaah.velcom.backend.access.exceptions.NoSuchTaskException;
import de.aaaaaaah.velcom.backend.access.policy.QueuePriority;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.restapi.authentication.RepoUser;
import de.aaaaaaah.velcom.backend.restapi.exception.TaskAlreadyExistsException;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRunner;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonTask;
import de.aaaaaaah.velcom.backend.runner.IDispatcher;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import io.micrometer.core.instrument.Timer;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/queue")
@Produces(MediaType.APPLICATION_JSON)
public class QueueEndpoint {

	// TODO: 12.09.20 Add one-up endpoint (also update the API spec)

	private static final Timer ENDPOINT_TIMER_GET = timer("velcom.endpoint.queue.get");
	private static final Timer ENDPOINT_TIMER_DELETE = timer("velcom.endpoint.queue.delete");
	private static final Timer ENDPOINT_TIMER_PATCH = timer("velcom.endpoint.queue.patch");
	private static final Timer ENDPOINT_TIMER_POST = timer("velcom.endpoint.queue.post");

	private final CommitReadAccess commitReadAccess;
	private final RepoReadAccess repoReadAccess;
	private final Queue queue;
	private final IDispatcher dispatcher;

	public QueueEndpoint(CommitReadAccess commitReadAccess,
		RepoReadAccess repoReadAccess, Queue queue,
		IDispatcher dispatcher) {
		this.commitReadAccess = commitReadAccess;
		this.repoReadAccess = repoReadAccess;
		this.queue = queue;
		this.dispatcher = dispatcher;
	}

	@GET
	@Timed
	public GetQueueReply getQueue() {
		final var timer = Timer.start();

		// TODO: 08.09.20 Fix NPE when calling this endpoint
		List<JsonTask> tasks = queue.getTasksSorted()
			.stream()
			.map(it -> JsonTask.fromTask(it, commitReadAccess))
			.collect(Collectors.toList());

		List<JsonRunner> worker = dispatcher.getKnownRunners()
			.stream()
			.map(JsonRunner::fromKnownRunner)
			.collect(Collectors.toList());

		timer.stop(ENDPOINT_TIMER_GET);

		return new GetQueueReply(tasks, worker);
	}

	@DELETE
	@Path("{taskId}")
	@Timed
	public Response deleteTask(@Auth RepoUser user, @PathParam("taskId") UUID taskId)
		throws NoSuchTaskException {
		final var timer = Timer.start();

		Task task = queue.getTaskById(new TaskId(taskId));
		if (task.getRepoId().isEmpty()) {
			user.guardAdminAccess();
		} else {
			user.guardRepoAccess(task.getRepoId().get());
		}

		queue.deleteTasks(List.of(new TaskId(taskId)));

		timer.stop(ENDPOINT_TIMER_DELETE);

		return Response.ok().build();
	}

	@PATCH
	@Path("{taskId}")
	@Timed
	public Response patchTask(@Auth RepoUser user, @PathParam("taskId") UUID taskId)
		throws NoSuchTaskException {
		final var timer = Timer.start();

		user.guardAdminAccess();

		// Throws an exception if the task does not exist! This is needed.
		Task task = queue.getTaskById(new TaskId(taskId));

		queue.prioritizeTask(task.getId(), QueuePriority.MANUAL);

		timer.stop(ENDPOINT_TIMER_PATCH);

		return Response.ok().build();
	}

	// TODO: 12.09.20 Gracefully handle the case that the specified commit does not exist in the repo
	// At the moment, a foreign key constraint violation is raised by sqlite because the task refers
	// to the known_commit table which doesn't contain the requested commit. Instead, the
	// TaskWriteAccess should probably throw a NoSuchCommitException.
	@POST
	@Path("commit/{repoId}/{hash}")
	@Timed
	public PostCommitReply addCommit(@Auth RepoUser user, @PathParam("repoId") UUID repoUuid,
		@PathParam("hash") String commitHashString) {
		final var timer = Timer.start();

		RepoId repoId = new RepoId(repoUuid);
		CommitHash commitHash = new CommitHash(commitHashString);

		user.guardRepoAccess(repoId);

		Repo repo = repoReadAccess.getRepo(repoId);

		String author = String.format(
			"%s %s(%s)",
			user.isAdmin() ? "Admin" : "Repo-Admin",
			repo.getName(),
			repoId
		);

		// TODO: Really don't tell them the id of the existing task?
		final Collection<Task> insertedTasks = queue
			.addCommits(author, repoId, List.of(commitHash), QueuePriority.MANUAL);

		if (insertedTasks.isEmpty()) {
			throw new TaskAlreadyExistsException(commitHash, repoId);
		}

		Task task = insertedTasks.iterator().next();

		timer.stop(ENDPOINT_TIMER_POST);

		return new PostCommitReply(JsonTask.fromTask(task, commitReadAccess));
	}

	private static class PostCommitReply {

		private final JsonTask task;

		private PostCommitReply(JsonTask task) {
			this.task = task;
		}

		public JsonTask getTask() {
			return task;
		}
	}

	private static class GetQueueReply {

		private final List<JsonTask> tasks;
		private final List<JsonRunner> runners;

		public GetQueueReply(List<JsonTask> tasks, List<JsonRunner> runners) {
			this.tasks = tasks;
			this.runners = runners;
		}

		public List<JsonTask> getTasks() {
			return tasks;
		}

		public List<JsonRunner> getRunners() {
			return runners;
		}
	}
}
