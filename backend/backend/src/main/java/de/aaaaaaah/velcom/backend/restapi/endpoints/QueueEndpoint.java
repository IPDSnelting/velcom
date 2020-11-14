package de.aaaaaaah.velcom.backend.restapi.endpoints;

import static java.util.stream.Collectors.toMap;

import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.RepoReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.exceptions.NoSuchRepoException;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.Task;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskId;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskPriority;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.exceptions.NoSuchTaskException;
import de.aaaaaaah.velcom.backend.restapi.authentication.RepoUser;
import de.aaaaaaah.velcom.backend.restapi.exception.TaskAlreadyExistsException;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRunner;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonSource;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonTask;
import de.aaaaaaah.velcom.backend.runner.IDispatcher;
import de.aaaaaaah.velcom.backend.runner.KnownRunner;
import de.aaaaaaah.velcom.shared.util.LinesWithOffset;
import de.aaaaaaah.velcom.shared.util.Pair;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

@Path("/queue")
@Produces(MediaType.APPLICATION_JSON)
public class QueueEndpoint {

	// TODO: 12.09.20 Add one-up endpoint (also update the API spec)

	private final CommitReadAccess commitReadAccess;
	private final RepoReadAccess repoReadAccess;
	private final Queue queue;
	private final IDispatcher dispatcher;

	public QueueEndpoint(CommitReadAccess commitReadAccess, RepoReadAccess repoReadAccess,
		Queue queue, IDispatcher dispatcher) {

		this.commitReadAccess = commitReadAccess;
		this.repoReadAccess = repoReadAccess;
		this.queue = queue;
		this.dispatcher = dispatcher;
	}

	@GET
	@Timed(histogram = true)
	public GetQueueReply getQueue() {
		List<Task> tasks = queue.getAllTasksInOrder();

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
			.collect(toMap(
				Entry::getKey,
				it -> commitReadAccess.getCommits(it.getKey(), it.getValue()).stream()
					.collect(toMap(Commit::getHash, commit -> commit))
			));

		List<JsonTask> jsonTasks = tasks.stream()
			.map(task -> {
				JsonSource source = task.getSource()
					.mapLeft(it -> {
						RepoId repoId = it.getRepoId();
						CommitHash hash = it.getHash();
						return commitPerHashPerRepo.get(repoId)
							.getOrDefault(hash, Commit.placeholder(repoId, hash));
					})
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
	@Timed(histogram = true)
	public void emptyQueue(@Auth RepoUser user) throws NoSuchTaskException {
		if (user.getRepoId().isEmpty()) {
			user.guardAdminAccess();
			queue.deleteAllTasks();
			return;
		}

		RepoId repoId = user.getRepoId().get();

		List<TaskId> tasksToDelete = queue.getAllTasksInOrder()
			.stream()
			// Exclude tasks without a repo or with another id
			.filter(it -> it.getRepoId().map(repoId::equals).orElse(false))
			.map(Task::getId)
			.collect(Collectors.toList());

		queue.deleteTasks(tasksToDelete);
	}

	@DELETE
	@Path("{taskId}")
	@Timed(histogram = true)
	public void deleteTask(@Auth RepoUser user, @PathParam("taskId") UUID taskId)
		throws NoSuchTaskException {
		Task task = queue.getTask(new TaskId(taskId));
		if (task.getRepoId().isEmpty()) {
			user.guardAdminAccess();
		} else {
			user.guardRepoAccess(task.getRepoId().get());
		}

		queue.deleteTasks(List.of(new TaskId(taskId)));
	}

	@PATCH
	@Path("{taskId}")
	@Timed(histogram = true)
	public void patchTask(@Auth RepoUser user, @PathParam("taskId") UUID taskId)
		throws NoSuchTaskException {
		user.guardAdminAccess();

		// Throws an exception if the task does not exist! This is needed.
		Task task = queue.getTask(new TaskId(taskId));

		queue.prioritizeTask(task.getId(), TaskPriority.MANUAL);
	}

	@POST
	@Path("commit/{repoId}/{hash}")
	@Timed(histogram = true)
	public PostCommitReply addCommit(
		@Auth RepoUser user,
		@PathParam("repoId") UUID repoUuid,
		@PathParam("hash") String commitHashString
	) throws NoSuchRepoException {
		RepoId repoId = new RepoId(repoUuid);
		CommitHash commitHash = new CommitHash(commitHashString);

		user.guardRepoAccess(repoId);

		repoReadAccess.guardRepoExists(repoId);
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
		final Optional<Task> insertedTask = queue
			.addCommit(author, repoId, commitHash, TaskPriority.MANUAL);

		if (insertedTask.isEmpty()) {
			throw new TaskAlreadyExistsException(commitHash, repoId);
		}

		return new PostCommitReply(JsonTask.fromTask(insertedTask.get(), commitReadAccess));
	}

	@Path("task/{taskId}/progress")
	@GET
	@Timed(histogram = true)
	public GetTaskOutputReply getRunnerOutput(@PathParam("taskId") UUID taskId) {
		Optional<KnownRunner> worker = dispatcher.getKnownRunners().stream()
			.filter(it -> it.getCurrentTask().isPresent())
			.filter(it -> it.getCurrentTask().get().getId().getId().equals(taskId))
			.findAny();

		if (worker.isEmpty()) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		LinesWithOffset lastOutputLines = worker.get().getLastOutputLines()
			.orElse(new LinesWithOffset(0, List.of()));

		return new GetTaskOutputReply(lastOutputLines.getLines(), lastOutputLines.getFirstLineOffset());
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

	private static class GetTaskOutputReply {

		private final List<String> output;
		private final int indexOfFirstLine;

		private GetTaskOutputReply(List<String> output, int indexOfFirstLine) {
			this.output = output;
			this.indexOfFirstLine = indexOfFirstLine;
		}

		public List<String> getOutput() {
			return output;
		}

		public int getIndexOfFirstLine() {
			return indexOfFirstLine;
		}
	}
}
