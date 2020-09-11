package de.aaaaaaah.velcom.backend.restapi.exception;

import de.aaaaaaah.velcom.backend.access.exceptions.CommitAccessException;
import java.util.UUID;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * An {@link ExceptionMapper} that transforms {@link CommitAccessException}s to NOT_FOUND.
 */
public class CommitAccessExceptionMapper implements ExceptionMapper<CommitAccessException> {

	@Override
	public Response toResponse(CommitAccessException exception) {
		return Response
			.status(Status.NOT_FOUND)
			.entity(new Info(
				"error while retrieving commit",
				exception.getRepoId().getId(),
				exception.getCommitHash().getHash()
			))
			.build();
	}

	private static class Info {

		private final String message;
		private final UUID repoId;
		private final String hash;

		public Info(String message, UUID repoId, String hash) {
			this.message = message;
			this.repoId = repoId;
			this.hash = hash;
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
	}
}
