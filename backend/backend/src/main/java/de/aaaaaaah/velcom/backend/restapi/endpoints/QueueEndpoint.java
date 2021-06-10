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
import de.aaaaaaah.velcom.backend.access.taskaccess.exceptions.TaskCreationException;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.restapi.authentication.Admin;
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
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
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
	public void emptyQueue(@Auth Admin admin) throws NoSuchTaskException {
		queue.deleteAllTasks();
	}

	@DELETE
	@Path("{taskid}")
	@Timed(histogram = true)
	public void deleteTask(@Auth Admin admin, @PathParam("taskid") UUID taskUuid)
		throws NoSuchTaskException {

		TaskId taskId = new TaskId(taskUuid);
		queue.guardTaskExists(taskId);
		queue.deleteTasks(List.of(taskId));
	}

	@PATCH
	@Path("{taskid}")
	@Timed(histogram = true)
	public void patchTask(@Auth Admin admin, @PathParam("taskid") UUID taskUuid)
		throws NoSuchTaskException {

		TaskId taskId = new TaskId(taskUuid);
		queue.guardTaskExists(taskId);
		queue.prioritizeTask(taskId, TaskPriority.MANUAL);
	}

	@POST
	@Path("commit/{repoid}/{hash}")
	@Timed(histogram = true)
	public PostCommitReply addCommit(
		@Auth Admin admin,
		@PathParam("repoid") UUID repoUuid,
		@PathParam("hash") String commitHashString
	) throws NoSuchRepoException {
		RepoId repoId = new RepoId(repoUuid);
		CommitHash commitHash = new CommitHash(commitHashString);

		repoReadAccess.guardRepoExists(repoId);
		commitReadAccess.guardCommitExists(repoId, commitHash);
		// If at any point between these checks and inserting the task the repo is deleted, JOOQ will
		// throw a DataAccessException (I believe) and this endpoint will return a 500. But the chance
		// of that happening is low enough that putting in the effort to perform the checks in an atomic
		// way is not worth it.

		// TODO: Really don't tell them the id of the existing task?
		final Optional<Task> insertedTask = queue
			.addCommit(AUTHOR_NAME_ADMIN, repoId, commitHash, TaskPriority.MANUAL);

		if (insertedTask.isEmpty()) {
			throw new TaskAlreadyExistsException(commitHash, repoId);
		}

		return new PostCommitReply(JsonTask.fromTask(insertedTask.get(), commitReadAccess));
	}


	@POST
	@Path("commit/{repoid}/{hash}/one-up")
	@Timed(histogram = true)
	public PostOneUpReply PostOneUpReply(
		@Auth Admin admin,
		@PathParam("repoid") UUID repoUuid,
		@PathParam("hash") String commitHashString
	) throws NoSuchRepoException {
		RepoId repoId = new RepoId(repoUuid);
		CommitHash rootHash = new CommitHash(commitHashString);

		commitReadAccess.guardCommitExists(repoId, rootHash);

		List<CommitHash> hashes = commitReadAccess.getDescendantCommits(repoId, rootHash);

		queue.addCommits(AUTHOR_NAME_ADMIN, repoId, hashes, TaskPriority.LISTENER);

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
		@Auth Admin admin,
		@NotNull @FormDataParam("description") String description,
		@Nullable @FormDataParam("repo_id") UUID repoUuid,
		@NotNull @FormDataParam("file") InputStream inputStream,
		@NotNull @FormDataParam("file") FormDataContentDisposition fileDisposition
	) throws IOException {
		Optional<RepoId> repoId = Optional.ofNullable(repoUuid).map(RepoId::new);

		final InputStream uncompressedInput =
			fileDisposition.getFileName().endsWith(".tar.gz")
				? new GZIPInputStream(inputStream)
				: inputStream;

		try (uncompressedInput) {
			Task task = queue.addTar(
				AUTHOR_NAME_ADMIN,
				TaskPriority.MANUAL,
				repoId.orElse(null),
				description,
				uncompressedInput
			);

			return new UploadTarReply(JsonTask.fromTask(task, commitReadAccess));
		} catch (TaskCreationException e) {
			Status status = e.isOurFault() ? Status.INTERNAL_SERVER_ERROR : Status.BAD_REQUEST;
			throw new WebApplicationException(e.getMessage(), status);
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
		Optional<LinesWithOffset> lastOutputLinesOpt = dispatcher.findLinesForTask(taskId);

		if (lastOutputLinesOpt.isEmpty()) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		LinesWithOffset lastOutputLines = lastOutputLinesOpt.orElseThrow();

		return new GetTaskOutputReply(lastOutputLines.getLines(), lastOutputLines.getFirstLineOffset());
	}

	private static class PostOneUpReply {

		public final List<JsonTask> tasks;

		public PostOneUpReply(List<JsonTask> tasks) {
			this.tasks = tasks;
		}
	}

	private static class PostCommitReply {

		public final JsonTask task;

		public PostCommitReply(JsonTask task) {
			this.task = task;
		}
	}

	private static class UploadTarReply {

		public final JsonTask task;

		public UploadTarReply(JsonTask task) {
			this.task = task;
		}
	}

	private static class GetQueueReply {

		public final List<JsonTask> tasks;
		public final List<JsonRunner> runners;

		public GetQueueReply(List<JsonTask> tasks, List<JsonRunner> runners) {
			this.tasks = tasks;
			this.runners = runners;
		}
	}

	private static class GetTaskOutputReply {

		public final List<String> output;
		public final int indexOfFirstLine;

		public GetTaskOutputReply(List<String> output, int indexOfFirstLine) {
			this.output = output;
			this.indexOfFirstLine = indexOfFirstLine;
		}
	}

	private static class GetTaskInfoReply {

		public final JsonTask task;
		public final int position;
		@Nullable
		public final Long runningSince;

		public GetTaskInfoReply(JsonTask task, int position, @Nullable Long runningSince) {
			this.task = task;
			this.position = position;
			this.runningSince = runningSince;
		}
	}
}
