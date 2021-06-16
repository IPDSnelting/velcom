package de.aaaaaaah.velcom.backend.restapi.endpoints;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Run;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.ShortRunDescription;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.exceptions.NoSuchRunException;
import de.aaaaaaah.velcom.backend.access.caches.LatestRunCache;
import de.aaaaaaah.velcom.backend.access.caches.RunCache;
import de.aaaaaaah.velcom.backend.access.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.DimensionReadAccess;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.data.recentruns.SignificantRun;
import de.aaaaaaah.velcom.backend.data.recentruns.SignificantRunsCollector;
import de.aaaaaaah.velcom.backend.data.runcomparison.DimensionDifference;
import de.aaaaaaah.velcom.backend.data.runcomparison.RunComparator;
import de.aaaaaaah.velcom.backend.data.runcomparison.RunComparison;
import de.aaaaaaah.velcom.backend.data.significance.SignificanceFactors;
import de.aaaaaaah.velcom.backend.data.significance.SignificanceReasons;
import de.aaaaaaah.velcom.backend.restapi.endpoints.utils.EndpointUtils;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimension;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimensionDifference;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRun;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonShortRunDescription;
import io.micrometer.core.annotation.Timed;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/run")
@Produces(MediaType.APPLICATION_JSON)
public class RunEndpoint {

	private final BenchmarkReadAccess benchmarkAccess;
	private final CommitReadAccess commitAccess;
	private final DimensionReadAccess dimensionAccess;
	private final RunCache runCache;
	private final LatestRunCache latestRunCache;
	private final RunComparator comparer;
	private final SignificanceFactors significanceFactors;
	private final SignificantRunsCollector significantRunsCollector;

	public RunEndpoint(BenchmarkReadAccess benchmarkAccess, CommitReadAccess commitAccess,
		DimensionReadAccess dimensionAccess, RunCache runCache, LatestRunCache latestRunCache,
		RunComparator comparer, SignificanceFactors significanceFactors,
		SignificantRunsCollector significantRunsCollector) {

		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
		this.dimensionAccess = dimensionAccess;
		this.runCache = runCache;
		this.latestRunCache = latestRunCache;
		this.comparer = comparer;
		this.significanceFactors = significanceFactors;
		this.significantRunsCollector = significantRunsCollector;
	}

	private Optional<Run> getPrevRun(Run run) {
		Optional<CommitSource> left = run.getSource().getLeft();
		if (left.isEmpty()) {
			return Optional.empty(); // Run doesn't come from a commit
		}
		CommitSource commitSource = left.get();

		Iterator<CommitHash> parentHashIt = commitAccess
			.getParentHashes(commitSource.getRepoId(), commitSource.getHash())
			.iterator();
		if (parentHashIt.hasNext()) {
			CommitHash parentHash = parentHashIt.next();
			return latestRunCache
				.getLatestRun(benchmarkAccess, runCache, commitSource.getRepoId(), parentHash);
		} else {
			return Optional.empty(); // No unambiguous previous commit
		}
	}

	@GET
	@Path("{runid}")
	@Timed(histogram = true)
	public GetReply get(
		@PathParam("runid") UUID runUuid,
		@QueryParam("all_values") @Nullable Boolean allValuesOptional,
		@QueryParam("hash") @Nullable String hashString,
		@QueryParam("diff_prev") @Nullable Boolean diffPrevOptional
	) throws NoSuchRunException {
		boolean allValues = (allValuesOptional != null) && allValuesOptional;
		boolean diffPrev = (diffPrevOptional != null) && diffPrevOptional;

		Run run = EndpointUtils.getRun(benchmarkAccess, runCache, latestRunCache, runUuid, hashString);

		// Obtain differences to previous run
		final Optional<List<JsonDimensionDifference>> differences;
		final Optional<List<JsonDimensionDifference>> significantDifferences;
		final Optional<List<JsonDimension>> significantFailedDimensions;
		if (diffPrev) {
			Optional<List<DimensionDifference>> prevRunDiffs = getPrevRun(run)
				.map(it -> comparer.compare(it, run))
				.map(RunComparison::getDifferences);

			Optional<SignificanceReasons> significanceReasons = significantRunsCollector
				.getSignificantRun(run)
				.map(SignificantRun::getReasons);

			Set<Dimension> dimensions = Stream.concat(
				prevRunDiffs.stream()
					.flatMap(Collection::stream)
					.map(DimensionDifference::getDimension),
				significanceReasons.stream()
					.map(SignificanceReasons::getDimensions)
					.flatMap(Collection::stream)
			).collect(toSet());

			Map<Dimension, DimensionInfo> infos = dimensionAccess.getDimensionInfoMap(dimensions);

			differences = prevRunDiffs
				.map(diffs -> diffs.stream()
					.map(diff -> JsonDimensionDifference.fromDimensionDifference(diff, infos))
					.collect(toList()));

			significantDifferences = significanceReasons
				.map(reasons -> reasons.getSignificantDifferences()
					.stream()
					.map(diff -> JsonDimensionDifference.fromDimensionDifference(diff, infos))
					.collect(toList()));

			significantFailedDimensions = significanceReasons
				.map(reasons -> reasons.getSignificantFailedDimensions()
					.stream()
					.map(infos::get)
					.map(JsonDimension::fromDimensionInfo)
					.collect(toList()));
		} else {
			differences = Optional.empty();
			significantDifferences = Optional.empty();
			significantFailedDimensions = Optional.empty();
		}

		return new GetReply(
			EndpointUtils.fromRun(dimensionAccess, commitAccess, run, significanceFactors, allValues),
			differences.orElse(null),
			significantDifferences.orElse(null),
			significantFailedDimensions.orElse(null)
		);
	}

	private static class GetReply {

		public final JsonRun run;
		@Nullable
		public final List<JsonDimensionDifference> differences;
		@Nullable
		public final List<JsonDimensionDifference> significantDifferences;
		@Nullable
		public final List<JsonDimension> significantFailedDimensions;

		public GetReply(JsonRun run, @Nullable List<JsonDimensionDifference> differences,
			@Nullable List<JsonDimensionDifference> significantDifferences,
			@Nullable List<JsonDimension> significantFailedDimensions) {

			this.run = run;
			this.differences = differences;
			this.significantDifferences = significantDifferences;
			this.significantFailedDimensions = significantFailedDimensions;
		}
	}

	@GET
	@Path("{runid}/short")
	@Timed(histogram = true)
	public GetShortReply getShort(@PathParam("runid") UUID runUuid) throws NoSuchRunException {
		RunId runId = new RunId(runUuid);
		ShortRunDescription run = benchmarkAccess.getShortRunDescription(runId);
		return new GetShortReply(JsonShortRunDescription.fromShortRunDescription(run));
	}

	private static class GetShortReply {

		public final JsonShortRunDescription run;

		public GetShortReply(JsonShortRunDescription run) {
			this.run = run;
		}
	}
}

