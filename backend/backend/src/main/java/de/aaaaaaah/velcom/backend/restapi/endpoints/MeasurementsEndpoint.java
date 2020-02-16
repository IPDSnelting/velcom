package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.newaccess.BenchmarkWriteAccess;
import de.aaaaaaah.velcom.backend.newaccess.entities.MeasurementName;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.restapi.RepoUser;
import io.dropwizard.auth.Auth;
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

	private final BenchmarkWriteAccess benchmarkAccess;

	public MeasurementsEndpoint(BenchmarkWriteAccess benchmarkAccess) {
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
		@Auth RepoUser user,
		@NotNull @QueryParam("repo_id") UUID repoUuid,
		@NotNull @QueryParam("benchmark") String benchmark,
		@NotNull @QueryParam("metric") String metric) {

		RepoId repoId = new RepoId(repoUuid);
		user.guardRepoAccess(repoId);

		MeasurementName measurementName = new MeasurementName(benchmark, metric);

		benchmarkAccess.deleteAllMeasurementsOfName(repoId, measurementName);
	}

}
