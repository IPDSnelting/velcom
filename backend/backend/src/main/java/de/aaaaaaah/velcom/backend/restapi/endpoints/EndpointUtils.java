package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.RunId;
import de.aaaaaaah.velcom.backend.access.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.access.entities.sources.TarSource;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonCommitDescription;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonResult;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRun;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonSource;
import de.aaaaaaah.velcom.backend.util.Either;
import java.util.Map;
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
	 * Create a {@link JsonRun} from a {@link Run} and a few other variables.
	 *
	 * @param benchmarkAccess a {@link BenchmarkReadAccess}
	 * @param commitAccess a {@link CommitReadAccess}
	 * @param run the run
	 * @param allValues whether the full lists of values should also be included for each
	 * 	measurement
	 * @return the created {@link JsonRun}
	 */
	public static JsonRun fromRun(BenchmarkReadAccess benchmarkAccess, CommitReadAccess commitAccess,
		Run run, boolean allValues) {

		JsonSource source = convertToSource(commitAccess, run.getSource());
		Map<Dimension, DimensionInfo> dimensionInfos = benchmarkAccess
			.getDimensionInfos(run.getAllDimensionsUsed());

		if (run.getResult().isLeft()) {
			return new JsonRun(
				run.getId().getId(),
				run.getAuthor(),
				run.getRunnerName(),
				run.getRunnerInfo(),
				run.getStartTime().getEpochSecond(),
				run.getStopTime().getEpochSecond(),
				source,
				JsonResult.fromRunError(run.getResult().getLeft().get())
			);
		} else {
			return new JsonRun(
				run.getId().getId(),
				run.getAuthor(),
				run.getRunnerName(),
				run.getRunnerInfo(),
				run.getStartTime().getEpochSecond(),
				run.getStopTime().getEpochSecond(),
				source,
				JsonResult.fromMeasurements(
					run.getResult().getRight().get(),
					dimensionInfos,
					allValues
				)
			);
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
