package de.aaaaaaah.velcom.backend.restapi.endpoints;

import static java.util.stream.Collectors.toList;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Run;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.caches.RunCache;
import de.aaaaaaah.velcom.backend.access.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.DimensionReadAccess;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.data.recentruns.SignificantRunsCollector;
import de.aaaaaaah.velcom.backend.data.significance.SignificanceReasons;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimension;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimensionDifference;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRunDescription;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRunDescription.JsonSuccess;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonSource;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Endpoint for getting a list of the most recent runs.
 */
@Path("/recent/runs")
@Produces(MediaType.APPLICATION_JSON)
public class RecentRunsEndpoint {

	private static final int DEFAULT_N = 10;
	private static final int MIN_N = 1;
	private static final int MAX_N = 100;

	private final BenchmarkReadAccess benchmarkAccess;
	private final CommitReadAccess commitAccess;
	private final DimensionReadAccess dimensionAccess;
	private final RunCache runCache;
	private final SignificantRunsCollector significantRunsCollector;

	public RecentRunsEndpoint(BenchmarkReadAccess benchmarkAccess, CommitReadAccess commitAccess,
		DimensionReadAccess dimensionAccess, RunCache runCache,
		SignificantRunsCollector significantRunsCollector) {

		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
		this.dimensionAccess = dimensionAccess;
		this.runCache = runCache;
		this.significantRunsCollector = significantRunsCollector;
	}

	@GET
	@Timed(histogram = true)
	public GetReply getRuns(
		@QueryParam("n") @Nullable Integer nOptional,
		@QueryParam("significant") @Nullable Boolean significantOptional
	) {
		int n = (nOptional == null) ? DEFAULT_N : nOptional;
		n = Math.max(MIN_N, Math.min(MAX_N, n));

		boolean significant = (significantOptional != null) && significantOptional;

		final List<JsonRunEntry> runEntries;
		if (significant) {
			runEntries = significantRunsCollector.collectMostRecent(n).stream()
				.map(run -> toJsonRunEntry(run.getRun(), run.getReasons()))
				.collect(toList());
		} else {
			List<RunId> recentRunIds = benchmarkAccess.getRecentRunIds(0, n);
			runEntries = runCache.getRunsInOrder(benchmarkAccess, recentRunIds).stream()
				.map(run -> toJsonRunEntry(run, null))
				.collect(toList());
		}

		return new GetReply(runEntries);
	}

	private JsonRunEntry toJsonRunEntry(Run run, @Nullable SignificanceReasons reasons) {
		final Optional<List<JsonDimensionDifference>> significantDifferences;
		final Optional<List<JsonDimension>> significantFailedDimensions;

		if (reasons == null) {
			significantDifferences = Optional.empty();
			significantFailedDimensions = Optional.empty();
		} else {
			Set<Dimension> dimensions = reasons.getDimensions();
			Map<Dimension, DimensionInfo> dimInfos = dimensionAccess.getDimensionInfoMap(dimensions);

			significantDifferences = Optional.of(reasons.getSignificantDifferences()
				.stream()
				.map(diff -> JsonDimensionDifference.fromDimensionDifference(diff, dimInfos))
				.collect(toList()));

			significantFailedDimensions = Optional.of(reasons.getSignificantFailedDimensions()
				.stream()
				.map(dimInfos::get)
				.map(JsonDimension::fromDimensionInfo)
				.collect(toList()));
		}

		return new JsonRunEntry(
			new JsonRunDescription(
				run.getId().getId(),
				run.getStartTime().getEpochSecond(),
				JsonSuccess.fromRunResult(run.getResult()),
				JsonSource.fromSource(run.getSource(), commitAccess)
			),
			significantDifferences.orElse(null),
			significantFailedDimensions.orElse(null)
		);
	}

	private static class GetReply {

		public final List<JsonRunEntry> runs;

		public GetReply(List<JsonRunEntry> runs) {
			this.runs = runs;
		}
	}

	private static class JsonRunEntry {

		public final JsonRunDescription run;
		@Nullable
		public final List<JsonDimensionDifference> significantDifferences;
		@Nullable
		public final List<JsonDimension> significantFailedDimensions;

		public JsonRunEntry(JsonRunDescription run,
			@Nullable List<JsonDimensionDifference> significantDifferences,
			@Nullable List<JsonDimension> significantFailedDimensions) {

			this.run = run;
			this.significantDifferences = significantDifferences;
			this.significantFailedDimensions = significantFailedDimensions;
		}
	}
}
