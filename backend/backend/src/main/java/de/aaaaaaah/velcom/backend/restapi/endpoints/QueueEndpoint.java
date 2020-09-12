package de.aaaaaaah.velcom.backend.restapi.endpoints;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.RepoReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.Repo;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.Task;
import de.aaaaaaah.velcom.backend.access.entities.TaskId;
import de.aaaaaaah.velcom.backend.access.entities.sources.CommitSource;
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
		List<Task> queueTasks = queue.getTasksSorted();

		@SuppressWarnings("OptionalGetWithoutIsPresent")
		Map<RepoId, List<CommitHash>> commitHashesPerRepo = queueTasks
			.stream()
			.filter(it -> it.getSource().getLeft().isPresent())
			.filter(it -> it.getRepoId().isPresent())
			.collect(groupingBy(
				task -> task.getRepoId().get(),
				Collectors.mapping(task -> task.getSource().getLeft().get().getHash(), toList())
			));

		Map<RepoId, Map<CommitHash, Commit>> repoToCommitMap = commitHashesPerRepo.entrySet().stream()
			.collect(toMap(
				Entry::getKey,
				it -> commitReadAccess.getCommits(it.getKey(), it.getValue()).stream()
					.collect(toMap(Commit::getHash, commit -> commit, (commit, commit2) -> commit))
			));

		List<JsonTask> tasks = queueTasks
			.stream()
			.map(it -> {
				if (it.getSource().isRight()) {
					return JsonTask.fromTask(it, commitReadAccess);
				}
				CommitSource commitSource = it.getSource().getLeft().get();
				Commit commit = repoToCommitMap.get(commitSource.getRepoId()).get(commitSource.getHash());

				return JsonTask.fromTask(it, commit);
			})
			.collect(Collectors.toList());

		List<JsonRunner> worker = dispatcher.getKnownRunners()
			.stream()
			.map(JsonRunner::fromKnownRunner)
			.collect(Collectors.toList());

		return new GetQueueReply(tasks, worker);
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

	@POST
	@Path("commit/{repoId}/{hash}")
	public PostCommitReply addCommit(@Auth RepoUser user, @PathParam("repoId") UUID repoUuid,
		@PathParam("hash") String commitHashString) {
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
