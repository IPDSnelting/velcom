package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Task;
import de.aaaaaaah.velcom.backend.access.entities.TaskId;
import de.aaaaaaah.velcom.backend.access.exceptions.NoSuchTaskException;
import de.aaaaaaah.velcom.backend.access.policy.QueuePriority;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.restapi.authentication.RepoUser;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRunner;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonTask;
import de.aaaaaaah.velcom.backend.runner.IDispatcher;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/queue")
@Produces(MediaType.APPLICATION_JSON)
public class QueueEndpoint {

	private final CommitReadAccess commitReadAccess;
	private final Queue queue;
	private final IDispatcher dispatcher;

	public QueueEndpoint(CommitReadAccess commitReadAccess, Queue queue, IDispatcher dispatcher) {
		this.commitReadAccess = commitReadAccess;
		this.queue = queue;
		this.dispatcher = dispatcher;
	}

	@GET
	public GetQueueReply getQueue() {
		List<JsonTask> tasks = queue.getTasksSorted()
			.stream()
			.map(it -> JsonTask.fromTask(it, commitReadAccess))
			.collect(Collectors.toList());

		List<JsonRunner> worker = dispatcher.getKnownRunners()
			.stream()
			.map(JsonRunner::fromKnownRunner)
			.collect(Collectors.toList());

		return new GetQueueReply(tasks, worker);
	}

	@DELETE
	@Path("{taskId}")
	public void deleteTask(@Auth RepoUser user, @PathParam("taskId") UUID taskId) {
		user.guardAdminAccess();

		queue.deleteTasks(List.of(new TaskId(taskId)));
	}

	@PATCH
	@Path("{taskId}")
	public void patchTask(@Auth RepoUser user, @PathParam("taskId") UUID taskId)
		throws NoSuchTaskException {
		user.guardAdminAccess();

		// Throws an exception if the task does not exist! This is needed.
		Task task = queue.getTaskById(new TaskId(taskId));

		queue.prioritizeTask(task.getId(), QueuePriority.MANUAL);
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
