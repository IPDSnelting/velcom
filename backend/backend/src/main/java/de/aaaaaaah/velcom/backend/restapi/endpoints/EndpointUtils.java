package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.RunId;
import de.aaaaaaah.velcom.backend.access.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.access.entities.sources.TarSource;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonCommitDescription;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonSource;
import de.aaaaaaah.velcom.backend.util.Either;
import java.util.UUID;
import javax.annotation.Nullable;

public class EndpointUtils {

	private EndpointUtils() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Obtain a {@link Run} either by its ID or by a commit.
	 *
	 * @param benchmarkAccess a {@link BenchmarkReadAccess}
	 * @param id the run's ID, or the commit's repo id, if {@code hash} is specified
	 * @param hash the commit's hash, if the run should be obtained through a commit
	 * @return the run if it can be found
	 */
	public static Run getRun(BenchmarkReadAccess benchmarkAccess, UUID id, @Nullable String hash) {
		if (hash == null) {
			RunId runId = new RunId(id);
			return benchmarkAccess.getRun(runId);
		} else {
			RepoId repoId = new RepoId(id);
			CommitHash commitHash = new CommitHash(hash);
			// TODO use exception instead
			return benchmarkAccess.getLatestRun(repoId, commitHash).get();
		}
	}

	/**
	 * Convert a source to a {@link JsonSource}.
	 *
	 * @param commitAccess a {@link CommitReadAccess}
	 * @param source the source to convert
	 * @return the converted source
	 */
	public static JsonSource convertToSource(CommitReadAccess commitAccess,
		Either<CommitSource, TarSource> source) {

		if (source.isLeft()) {
			CommitSource commitSource = source.getLeft().get();
			Commit commit = commitAccess.getCommit(commitSource.getRepoId(), commitSource.getHash());
			return JsonSource.fromCommit(JsonCommitDescription.fromCommit(commit));
		} else {
			TarSource tarSource = source.getRight().get();
			UUID repoId = tarSource.getRepoId()
				.map(RepoId::getId)
				.orElse(null);
			return JsonSource.fromUploadedTar(tarSource.getDescription(), repoId);
		}
	}
}
