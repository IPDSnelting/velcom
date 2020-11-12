package de.aaaaaaah.velcom.backend.newaccess;

import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.CommitSource;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.TarSource;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.shared.util.Either;
import javax.annotation.Nullable;

public class AccessUtils {

	private AccessUtils() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Interpret a repo id, commit hash and tar description as a source.
	 * <p>
	 * If a commit hash is present, the source is a {@link CommitSource}, otherwise it is a {@link
	 * TarSource}. If it is a {@link CommitSource}, the repo id must also be present and the tar
	 * description must not be present. If it is a {@link TarSource}, the repo id may or may not be
	 * present.
	 *
	 * @param repoId the repo id
	 * @param commitHash the commit hash
	 * @param tarDesc the tar description
	 * @return the source
	 */
	public static Either<CommitSource, TarSource> readSource(@Nullable String repoId,
		@Nullable String commitHash, @Nullable String tarDesc) {

		if (commitHash != null) { // Must be commit source
			return Either.ofLeft(new CommitSource(
				RepoId.fromString(repoId),
				new CommitHash(commitHash)
			));
		} else if (repoId != null) { // Must be tar source with repo id
			return Either.ofRight(new TarSource(
				tarDesc,
				RepoId.fromString(repoId)
			));
		} else { // Must be tar source without repo id
			return Either.ofRight(new TarSource(tarDesc));
		}
	}

}