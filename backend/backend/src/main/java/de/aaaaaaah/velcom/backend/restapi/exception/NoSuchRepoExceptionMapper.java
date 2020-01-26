package de.aaaaaaah.velcom.backend.restapi.exception;

import de.aaaaaaah.velcom.backend.access.repo.exception.NoSuchRepoException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * An {@link ExceptionMapper} that transforms {@link NoSuchRepoException}s to NOT_FOUND.
 */
public class NoSuchRepoExceptionMapper implements ExceptionMapper<NoSuchRepoException> {

	@Override
	public Response toResponse(NoSuchRepoException exception) {
		return Response.noContent().status(Status.NOT_FOUND).build();
	}
}
