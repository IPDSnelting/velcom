package de.aaaaaaah.velcom.backend.restapi.endpoints;

import static java.util.stream.Collectors.toMap;

import de.aaaaaaah.velcom.backend.access.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.repoaccess.RepoReadAccess;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.repoaccess.exceptions.NoSuchRepoException;
import de.aaaaaaah.velcom.backend.access.taskaccess.entities.Task;
import de.aaaaaaah.velcom.backend.access.taskaccess.entities.TaskId;
import de.aaaaaaah.velcom.backend.access.taskaccess.entities.TaskPriority;
import de.aaaaaaah.velcom.backend.access.taskaccess.exceptions.NoSuchTaskException;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
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
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Path("/queue")
@Produces(MediaType.APPLICATION_JSON)
public class QueueEndpoint {

	private static final String AUTHOR_NAME_ADMIN = "Admin";
	private static final String AUTHOR_NAME_REPO_ADMIN = "Repo-Admin";

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

	private static String getAuthor(boolean isAdmin) {
		// There's no need to include the repo name or ID since that info is already included in the
		// task's source.
		return isAdmin ? AUTHOR_NAME_ADMIN : AUTHOR_NAME_REPO_ADMIN;
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
	@Path("{taskid}")
	@Timed(histogram = true)
	public void deleteTask(@Auth RepoUser user, @PathParam("taskid") UUID taskId)
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
	@Path("{taskid}")
	@Timed(histogram = true)
	public void patchTask(@Auth RepoUser user, @PathParam("taskid") UUID taskId)
		throws NoSuchTaskException {
		user.guardAdminAccess();

		// Throws an exception if the task does not exist! This is needed.
		Task task = queue.getTask(new TaskId(taskId));

		queue.prioritizeTask(task.getId(), TaskPriority.MANUAL);
	}

	@POST
	@Path("commit/{repoid}/{hash}")
	@Timed(histogram = true)
	public PostCommitReply addCommit(
		@Auth RepoUser user,
		@PathParam("repoid") UUID repoUuid,
		@PathParam("hash") String commitHashString
	) throws NoSuchRepoException {
		RepoId repoId = new RepoId(repoUuid);
		CommitHash commitHash = new CommitHash(commitHashString);

		user.guardRepoAccess(repoId);

		repoReadAccess.guardRepoExists(repoId);
		commitReadAccess.guardCommitExists(repoId, commitHash);
		// If at any point between these checks and inserting the task the repo is deleted, JOOQ will
		// throw a DataAccessException (I believe) and this endpoint will return a 500. But the chance
		// of that happening is low enough that putting in the effort to perform the checks in an atomic
		// way is not worth it.

		String author = getAuthor(user.isAdmin());

		// TODO: Really don't tell them the id of the existing task?
		final Optional<Task> insertedTask = queue
			.addCommit(author, repoId, commitHash, TaskPriority.MANUAL);

		if (insertedTask.isEmpty()) {
			throw new TaskAlreadyExistsException(commitHash, repoId);
		}

		return new PostCommitReply(JsonTask.fromTask(insertedTask.get(), commitReadAccess));
	}


	@POST
	@Path("commit/{repoid}/{hash}/one-up")
	@Timed(histogram = true)
	public PostOneUpReply PostOneUpReply(
		@Auth RepoUser user,
		@PathParam("repoid") UUID repoUuid,
		@PathParam("hash") String commitHashString
	) throws NoSuchRepoException {
		RepoId repoId = new RepoId(repoUuid);
		CommitHash rootHash = new CommitHash(commitHashString);

		user.guardRepoAccess(repoId);
		commitReadAccess.guardCommitExists(repoId, rootHash);

		List<CommitHash> hashes = commitReadAccess.getDescendantCommits(repoId, rootHash);

		String author = getAuthor(user.isAdmin());
		queue.addCommits(author, repoId, hashes, TaskPriority.MANUAL);

		List<JsonTask> tasks = queue.getAllTasksInOrder().stream()
			.filter(it -> it.getRepoId().map(repoId::equals).orElse(false))
			.filter(it -> it.getCommitHash().isPresent())
			.filter(it -> hashes.contains(it.getCommitHash().get()))
			.map(task -> JsonTask.fromTask(task, commitReadAccess))
			.collect(Collectors.toList());

		return new PostOneUpReply(tasks);
	}

	@POST
	@Path("upload/tar")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Timed(histogram = true)
	public UploadTarReply uploadTar(
		@Auth RepoUser user,
		@Nonnull @FormDataParam("description") String description,
		@Nullable @FormDataParam("repo_id") UUID repoUuid,
		@FormDataParam("file") InputStream inputStream,
		@FormDataParam("file") FormDataContentDisposition fileDisposition
	) throws IOException {
		user.guardAdminAccess();

		Optional<RepoId> repoId = Optional.ofNullable(repoUuid).map(RepoId::new);

		final InputStream uncompressedInput =
			fileDisposition.getFileName().endsWith(".tar.gz")
				? new GZIPInputStream(inputStream)
				: inputStream;

		try (uncompressedInput) {
			Optional<Task> task = queue.addTar(
				AUTHOR_NAME_ADMIN,
				TaskPriority.MANUAL,
				repoId.orElse(null),
				description,
				uncompressedInput
			);

			if (task.isEmpty()) {
				// The task is empty if the tar could not be stored in the data dir, which should not happen
				// during normal operation.
				throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
			}

			return new UploadTarReply(JsonTask.fromTask(task.get(), commitReadAccess));
		}
	}

	@Path("task/{taskid}")
	@GET
	@Timed(histogram = true)
	public GetTaskInfoReply getTask(@PathParam("taskid") UUID taskId) {
		int indexOfTask = 0;
		Optional<Task> foundTask = Optional.empty();
		for (Task task : queue.getAllTasksInOrder()) {
			if (task.getIdAsUuid().equals(taskId)) {
				foundTask = Optional.of(task);
				break;
			}
			indexOfTask++;
		}

		if (foundTask.isEmpty()) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		Long runningSince = dispatcher.getKnownRunners().stream()
			.filter(it -> it.getCurrentTask().isPresent())
			.filter(it -> it.getCurrentTask().get().getId().getId().equals(taskId))
			.findAny()
			.flatMap(KnownRunner::getWorkingSince)
			.map(Instant::getEpochSecond)
			.orElse(null);

		return new GetTaskInfoReply(JsonTask.fromTask(
			foundTask.get(), commitReadAccess),
			indexOfTask,
			runningSince
		);
	}

	@Path("task/{taskid}/progress")
	@GET
	@Timed(histogram = true)
	public GetTaskOutputReply getRunnerOutput(@PathParam("taskid") UUID taskId) {
		Optional<LinesWithOffset> lastOutputLinesOpt = findLinesForTask(taskId);

		if (lastOutputLinesOpt.isEmpty()) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		LinesWithOffset lastOutputLines = lastOutputLinesOpt
			.orElse(new LinesWithOffset(0, List.of()));

		return new GetTaskOutputReply(lastOutputLines.getLines(), lastOutputLines.getFirstLineOffset());
	}

	private Optional<LinesWithOffset> findLinesForTask(UUID taskId) {
		Optional<KnownRunner> activeWorker = dispatcher.getKnownRunners().stream()
			.filter(it -> it.getCurrentTask().isPresent())
			.filter(it -> it.getCurrentTask().get().getId().getId().equals(taskId))
			.findAny();

		if (activeWorker.isPresent()) {
			LinesWithOffset lastOutputLines = activeWorker.get().getLastOutputLines()
				.orElse(new LinesWithOffset(0, List.of()));
			return Optional.of(lastOutputLines);
		}

		return dispatcher.getKnownRunners().stream()
			.flatMap(it -> it.getCompletedTasks().stream())
			.filter(it -> it.getTaskId().getId().equals(taskId))
			.findFirst()
			.map(it -> it.getLastLogLines().orElse(new LinesWithOffset(0, List.of())));
	}

	private static class PostOneUpReply {

		private final List<JsonTask> tasks;

		public PostOneUpReply(List<JsonTask> tasks) {
			this.tasks = tasks;
		}

		public List<JsonTask> getTasks() {
			return tasks;
		}
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

	private static class UploadTarReply {

		private final JsonTask task;

		private UploadTarReply(JsonTask task) {
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

	private static class GetTaskInfoReply {

		private final JsonTask task;
		private final int position;
		@Nullable
		private final Long runningSince;

		public GetTaskInfoReply(JsonTask task, int position, @Nullable Long runningSince) {
			this.task = task;
			this.position = position;
			this.runningSince = runningSince;
		}

		public JsonTask getTask() {
			return task;
		}

		public int getPosition() {
			return position;
		}

		@Nullable
		public Long getRunningSince() {
			return runningSince;
		}
	}
}
