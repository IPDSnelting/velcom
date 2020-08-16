package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.RunId;
import java.util.UUID;
import javax.annotation.Nullable;

public class EndpointUtils {

	private EndpointUtils() {
		throw new UnsupportedOperationException();
	}

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
}
