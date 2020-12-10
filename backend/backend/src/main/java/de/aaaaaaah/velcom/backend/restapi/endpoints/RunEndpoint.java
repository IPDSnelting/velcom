package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.data.runcomparison.RunComparator;
import de.aaaaaaah.velcom.backend.data.runcomparison.RunComparison;
import de.aaaaaaah.velcom.backend.data.runcomparison.SignificanceFactors;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.CommitSource;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.exceptions.NoSuchRunException;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.DimensionReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.restapi.endpoints.utils.EndpointUtils;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimensionDifference;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRun;
import io.micrometer.core.annotation.Timed;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
	private final DimensionReadAccess dimensionAccess;
	private final RunComparator comparer;
	private final SignificanceFactors significanceFactors;

	public RunEndpoint(BenchmarkReadAccess benchmarkAccess, CommitReadAccess commitAccess,
		DimensionReadAccess dimensionAccess, RunComparator comparer,
		SignificanceFactors significanceFactors) {

		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
		this.dimensionAccess = dimensionAccess;
		this.comparer = comparer;
		this.significanceFactors = significanceFactors;
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
			return benchmarkAccess.getLatestRun(commitSource.getRepoId(), parentHash);
		} else {
			return Optional.empty(); // No unambiguous previous commit
		}
	}

	@GET
	@Timed(histogram = true)
	public GetReply get(
		@PathParam("runid") UUID runUuid,
		@QueryParam("all_values") @Nullable Boolean allValuesOptional,
		@QueryParam("hash") @Nullable String hashString,
		@QueryParam("diff_prev") @Nullable Boolean diffPrevOptional
	) throws NoSuchRunException {
		boolean allValues = (allValuesOptional != null) && allValuesOptional;
		boolean diffPrev = (diffPrevOptional != null) && diffPrevOptional;

		Run run = EndpointUtils.getRun(benchmarkAccess, runUuid, hashString);

		// Obtain differences to previous run
		Optional<List<JsonDimensionDifference>> differences;
		if (diffPrev) {
			differences = getPrevRun(run)
				.map(prevRun -> {
					RunComparison comparison = comparer.compare(prevRun, run);
					Map<Dimension, DimensionInfo> infos = dimensionAccess
						.getDimensionInfoMap(comparison.getDimensions());
					return JsonDimensionDifference.fromRunComparison(comparison, infos);
				});
		} else {
			differences = Optional.empty();
		}

		return new GetReply(
			EndpointUtils.fromRun(dimensionAccess, commitAccess, run, significanceFactors, allValues),
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
