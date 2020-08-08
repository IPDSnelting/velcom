package de.aaaaaaah.velcom.backend.restapi.exception;

import de.aaaaaaah.velcom.backend.access.exceptions.CommitAccessException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * An {@link ExceptionMapper} that transforms {@link CommitAccessException}s to NOT_FOUND.
 */
public class CommitAccessExceptionMapper implements ExceptionMapper<CommitAccessException> {

	@Override
	public Response toResponse(CommitAccessException exception) {
		return Response.status(Status.NOT_FOUND).build();
	}
}
