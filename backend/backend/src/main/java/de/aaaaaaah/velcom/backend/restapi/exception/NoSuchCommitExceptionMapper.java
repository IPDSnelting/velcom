package de.aaaaaaah.velcom.backend.restapi.exception;

import de.aaaaaaah.velcom.backend.access.exceptions.NoSuchCommitException;
import de.aaaaaaah.velcom.backend.restapi.util.ErrorResponseUtil;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * An {@link ExceptionMapper} that transforms {@link NoSuchCommitException}s to NOT_FOUND.
 */
public class NoSuchCommitExceptionMapper implements ExceptionMapper<NoSuchCommitException> {

	@Override
	public Response toResponse(NoSuchCommitException exception) {
		return ErrorResponseUtil.errorResponse(
			Status.NOT_FOUND,
			"Commit " + exception.getCommitHash() + " not found!"
		);
	}
}
