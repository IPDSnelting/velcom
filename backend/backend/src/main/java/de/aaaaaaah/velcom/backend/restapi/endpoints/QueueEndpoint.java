package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.RepoReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
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
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonSource;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonTask;
import de.aaaaaaah.velcom.backend.runner.IDispatcher;
import de.aaaaaaah.velcom.shared.util.Pair;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
	public GetQueueReply getQueue() {
		// TODO: 08.09.20 Fix NPE when calling this endpoint

		List<Task> tasks = queue.getTasksSorted();

		Map<RepoId, List<CommitHash>> hashPerRepo = tasks.stream()
			// do
			//   repoId <- taskRepoId t
			//   hash <- getHash <$> getLeft (taskSource t)
			//   pure (repoId, hash)
			.flatMap(task -> task.getSource().getLeft()
				.flatMap(cs -> task.getRepoId().map(rid -> new Pair<>(rid, cs.getHash())))
				.stream())
			.collect(Collectors.groupingBy(
				Pair::getFirst,
				Collectors.mapping(Pair::getSecond, Collectors.toList())
			));

		Map<RepoId, Map<CommitHash, Commit>> commitPerHashPerRepo = hashPerRepo.entrySet().stream()
			.collect(Collectors.toMap(
				Entry::getKey,
				it -> commitReadAccess.getCommits(it.getKey(), it.getValue())
			));

		List<JsonTask> jsonTasks = tasks.stream()
			.map(task -> {
				JsonSource source = task.getSource()
					.mapLeft(it -> commitPerHashPerRepo.get(it.getRepoId()).get(it.getHash()))
					.consume(JsonSource::fromCommit, JsonSource::fromTarSource);
				return JsonTask.fromTask(task, source);
			})
			.collect(Collectors.toList());

		List<JsonRunner> worker = dispatcher.getKnownRunners()
			.stream()
			.map(JsonRunner::fromKnownRunner)
			.collect(Collectors.toList());

		return new GetQueueReply(jsonTasks, worker);
	}

	@DELETE
	@Path("{taskId}")
	public Response deleteTask(@Auth RepoUser user, @PathParam("taskId") UUID taskId)
		throws NoSuchTaskException {
		Task task = queue.getTaskById(new TaskId(taskId));
		if (task.getRepoId().isEmpty()) {
			user.guardAdminAccess();
		} else {
			user.guardRepoAccess(task.getRepoId().get());
		}

		queue.deleteTasks(List.of(new TaskId(taskId)));

		return Response.ok().build();
	}

	@PATCH
	@Path("{taskId}")
	public Response patchTask(@Auth RepoUser user, @PathParam("taskId") UUID taskId)
		throws NoSuchTaskException {
		user.guardAdminAccess();

		// Throws an exception if the task does not exist! This is needed.
		Task task = queue.getTaskById(new TaskId(taskId));

		queue.prioritizeTask(task.getId(), QueuePriority.MANUAL);

		return Response.ok().build();
	}

	// TODO: 12.09.20 Gracefully handle the case that the specified commit does not exist in the repo
	// At the moment, a foreign key constraint violation is raised by sqlite because the task refers
	// to the known_commit table which doesn't contain the requested commit. Instead, the
	// TaskWriteAccess should probably throw a NoSuchCommitException.
	@POST
	@Path("commit/{repoId}/{hash}")
	public PostCommitReply addCommit(@Auth RepoUser user, @PathParam("repoId") UUID repoUuid,
		@PathParam("hash") String commitHashString) {
		RepoId repoId = new RepoId(repoUuid);
		CommitHash commitHash = new CommitHash(commitHashString);

		user.guardRepoAccess(repoId);

		// Ensure the repository exists, 404s otherwise
		Repo repo = repoReadAccess.getRepo(repoId);
		// Ensure the commit exists, 404s otherwise
		Commit commit = commitReadAccess.getCommit(repoId, commitHash);
		// If at any point between these checks and inserting the task the repo is deleted, JOOQ will
		// throw a DataAccessException (I believe) and this endpoint will return a 500. But the chance
		// of that happening is low enough that putting in the effort to perform the checks in an atomic
		// way is not worth it.

		// There's no need to include the repo name or ID since that info is already included in the
		// task's source.
		String author = user.isAdmin() ? "Admin" : "Repo-Admin";

		// TODO: Really don't tell them the id of the existing task?
		final Collection<Task> insertedTasks = queue
			.addCommits(author, repoId, List.of(commitHash), QueuePriority.MANUAL);

		if (insertedTasks.isEmpty()) {
			throw new TaskAlreadyExistsException(commitHash, repoId);
		}

		Task task = insertedTasks.iterator().next();

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
