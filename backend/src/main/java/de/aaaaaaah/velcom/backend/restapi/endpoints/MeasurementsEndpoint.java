package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.benchmark.BenchmarkAccess;
import de.aaaaaaah.velcom.backend.access.benchmark.MeasurementName;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * The REST API endpoint providing a way to alter measurements for a given repo, benchmark and
 * metric.
 */
@Path("/measurements")
@Produces(MediaType.APPLICATION_JSON)
public class MeasurementsEndpoint {

	private final BenchmarkAccess benchmarkAccess;

	public MeasurementsEndpoint(BenchmarkAccess benchmarkAccess) {
		this.benchmarkAccess = benchmarkAccess;
	}

	/**
	 * Deletes all measurements for a given benchmark-metric combination.
	 *
	 * @param repoUuid the id of the repo
	 * @param benchmark the benchmark name
	 * @param metric the metric name
	 */
	@DELETE
	public void delete(
		@NotNull @QueryParam("repo_id") UUID repoUuid,
		@NotNull @QueryParam("benchmark") String benchmark,
		@NotNull @QueryParam("metric") String metric) {

		RepoId repoId = new RepoId(repoUuid);
		MeasurementName measurementName = new MeasurementName(benchmark, metric);

		benchmarkAccess.deleteAllMeasurementsOfName(repoId, measurementName);
	}
}
