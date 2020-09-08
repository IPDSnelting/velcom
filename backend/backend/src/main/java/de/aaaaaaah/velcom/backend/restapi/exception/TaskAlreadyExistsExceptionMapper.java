package de.aaaaaaah.velcom.backend.restapi.exception;

import java.util.UUID;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * An {@link ExceptionMapper} that transforms {@link TaskAlreadyExistsException}s to CONFLICT.
 */
public class TaskAlreadyExistsExceptionMapper implements
	ExceptionMapper<TaskAlreadyExistsException> {

	@Override
	public Response toResponse(TaskAlreadyExistsException exception) {
		return Response
			.status(Status.CONFLICT)
			.entity(new Info(
				"that task already exists in the queue",
				exception.getRepoId().getId(),
				exception.getHash().getHash(),
				exception.getTaskId().getId()
			))
			.build();
	}

	private static class Info {

		private final String message;
		private final UUID repoId;
		private final String hash;
		private final UUID taskId;

		public Info(String message, UUID repoId, String hash, UUID taskId) {
			this.message = message;
			this.repoId = repoId;
			this.hash = hash;
			this.taskId = taskId;
		}

		public String getMessage() {
			return message;
		}

		public UUID getRepoId() {
			return repoId;
		}

		public String getHash() {
			return hash;
		}

		public UUID getTaskId() {
			return taskId;
		}
	}
}
