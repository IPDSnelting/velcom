package de.aaaaaaah.velcom.backend.restapi.exception;

import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.exceptions.NoSuchDimensionException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * An {@link ExceptionMapper} that transforms {@link NoSuchDimensionException}s to NOT_FOUND.
 */
public class NoSuchDimensionExceptionMapper implements ExceptionMapper<NoSuchDimensionException> {

	@Override
	public Response toResponse(NoSuchDimensionException exception) {
		return Response
			.status(Status.NOT_FOUND)
			.entity(new Info(
				"could not find dimension",
				exception.getInvalidDimension().getBenchmark(),
				exception.getInvalidDimension().getMetric()
			))
			.build();
	}

	private static class Info {

		private final String message;
		private final String benchmark;
		private final String metric;

		public Info(String message, String benchmark, String metric) {
			this.message = message;
			this.benchmark = benchmark;
			this.metric = metric;
		}

		public String getMessage() {
			return message;
		}

		public String getBenchmark() {
			return benchmark;
		}

		public String getMetric() {
			return metric;
		}
	}
}
