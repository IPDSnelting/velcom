package de.aaaaaaah.velcom.backend.restapi.exception;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.exceptions.NoSuchRunException;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.shared.util.Either;
import de.aaaaaaah.velcom.shared.util.Pair;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * An {@link ExceptionMapper} that transforms {@link NoSuchRunException}s to NOT_FOUND.
 */
public class NoSuchRunExceptionMapper implements ExceptionMapper<NoSuchRunException> {

	@Override
	public Response toResponse(NoSuchRunException exception) {
		Either<RunId, Pair<RepoId, CommitHash>> source = exception.getInvalidSource();
		return Response
			.status(Status.NOT_FOUND)
			.entity(new Info(
				"could not find run",
				source.getLeft().map(RunId::getId).orElse(null),
				source.getRight().map(Pair::getFirst).map(RepoId::getId).orElse(null),
				source.getRight().map(Pair::getSecond).map(CommitHash::getHash).orElse(null)
			))
			.build();
	}

	private static class Info {

		public final String message;
		@Nullable
		public final UUID id;
		@Nullable
		public final UUID repoId;
		@Nullable
		public final String commitHash;

		public Info(String message, @Nullable UUID id, @Nullable UUID repoId,
			@Nullable String commitHash) {

			this.message = message;
			this.id = id;
			this.repoId = repoId;
			this.commitHash = commitHash;
		}
	}
}
