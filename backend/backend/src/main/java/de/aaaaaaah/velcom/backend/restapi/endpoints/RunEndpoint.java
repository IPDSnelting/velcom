package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.access.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.data.runcomparison.RunComparer;
import de.aaaaaaah.velcom.backend.data.runcomparison.RunComparison;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimensionDifference;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonResult;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRun;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/run/{runid}")
@Produces(MediaType.APPLICATION_JSON)
public class RunEndpoint {

	private final BenchmarkReadAccess benchmarkAccess;
	private final CommitReadAccess commitAccess;
	private final RunComparer comparer;

	public RunEndpoint(BenchmarkReadAccess benchmarkAccess, CommitReadAccess commitAccess,
		RunComparer comparer) {

		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
		this.comparer = comparer;
	}

	private Optional<Run> getPrevRun(Run run) {
		Optional<CommitSource> left = run.getSource().getLeft();
		if (left.isEmpty()) {
			return Optional.empty(); // Run doesn't come from a commit
		}
		CommitSource commitSource = left.get();

		Commit commit = commitAccess.getCommit(commitSource.getRepoId(), commitSource.getHash());
		ArrayList<CommitHash> parentHashes = new ArrayList<>(commit.getParentHashes());
		if (parentHashes.size() != 1) {
			return Optional.empty(); // No unambiguous previous commit
		}

		CommitHash parentHash = parentHashes.get(0);
		return benchmarkAccess.getLatestRun(commitSource.getRepoId(), parentHash);
	}

	@GET
	public GetReply get(
		@PathParam("runid") UUID runUuid,
		@QueryParam("all_values") @Nullable Boolean allValuesOptional,
		@QueryParam("hash") @Nullable String hashString,
		@QueryParam("diff_prev") @Nullable Boolean diffPrevOptional
	) {
		boolean allValues = (allValuesOptional != null) && allValuesOptional;
		boolean diffPrev = (diffPrevOptional != null) && diffPrevOptional;

		Run run = EndpointUtils.getRun(benchmarkAccess, runUuid, hashString);
		JsonSource source = EndpointUtils.convertToSource(commitAccess, run.getSource());

		// Obtain differences to previous run
		Optional<List<JsonDimensionDifference>> differences;
		if (diffPrev) {
			differences = getPrevRun(run)
				.map(prevRun -> {
					RunComparison comparison = comparer.compare(prevRun, run);
					Map<Dimension, DimensionInfo> infos = benchmarkAccess
						.getDimensionInfos(comparison.getDimensions());
					return comparison.getDifferences().stream()
						.map(difference -> JsonDimensionDifference.fromDimensionDifference(difference, infos))
						.collect(Collectors.toList());

				});
		} else {
			differences = Optional.empty();
		}

		final JsonRun jsonRun;
		if (run.getResult().isLeft()) {
			jsonRun = new JsonRun(
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
			Collection<Measurement> measurements = run.getResult().getRight().get();
			List<Dimension> dimensions = measurements.stream()
				.map(Measurement::getDimension)
				.collect(Collectors.toList());
			jsonRun = new JsonRun(
				run.getId().getId(),
				run.getAuthor(),
				run.getRunnerName(),
				run.getRunnerInfo(),
				run.getStartTime().getEpochSecond(),
				run.getStopTime().getEpochSecond(),
				source,
				JsonResult.fromMeasurements(
					measurements,
					benchmarkAccess.getDimensionInfos(dimensions),
					allValues
				)
			);
		}

		return new GetReply(
			jsonRun,
			differences.orElse(null)
		);
	}

	private static class GetReply {

		public final JsonRun run;
		@Nullable
		public final List<JsonDimensionDifference> differences;

		public GetReply(JsonRun run, @Nullable List<JsonDimensionDifference> differences) {
			this.run = run;
			this.differences = differences;
		}
	}
}
