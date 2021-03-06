package de.aaaaaaah.velcom.backend.access;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.sources.TarSource;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.shared.util.Either;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * A utility class containing helper functions specific to the access layer.
 */
public class AccessUtils {

	private AccessUtils() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Interpret a repo id, commit hash and tar description as a source.
	 *
	 * <p> If a commit hash is present, the source is a {@link CommitSource}, otherwise it is a
	 * {@link TarSource}. If it is a {@link CommitSource}, the repo id must also be present and the
	 * tar description must not be present. If it is a {@link TarSource}, the repo id may or may not
	 * be present.
	 *
	 * @param repoId the repo id
	 * @param commitHash the commit hash
	 * @param tarDesc the tar description
	 * @return the source
	 */
	public static Either<CommitSource, TarSource> readSource(@Nullable String repoId,
		@Nullable String commitHash, @Nullable String tarDesc) {

		if (commitHash != null) { // Must be commit source
			if (repoId == null) {
				throw new IllegalArgumentException("repoId must not be null if commit hash is not null");
			}
			if (tarDesc != null) {
				throw new IllegalArgumentException("tarDesc must be null if commit hash is not null");
			}

			return Either.ofLeft(new CommitSource(
				RepoId.fromString(repoId),
				new CommitHash(commitHash)
			));
		} else { // Must be tar source
			if (tarDesc == null) {
				throw new IllegalArgumentException("tarDesc must not be null if commit hash is null");
			}

			return Either.ofRight(new TarSource(
				tarDesc,
				Optional.ofNullable(repoId)
					.map(RepoId::fromString)
					.orElse(null)
			));
		}
	}

}
