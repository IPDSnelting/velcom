package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.DimensionReadAccess;
import de.aaaaaaah.velcom.backend.runner.Dispatcher;
import de.aaaaaaah.velcom.shared.GitProperties;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Endpoint for debugging. Not specified in the API and can change at any time.
 */
@Path("/debug")
@Produces(MediaType.APPLICATION_JSON)
public class DebugEndpoint {

	private final DimensionReadAccess dimensionAccess;
	private final Dispatcher dispatcher;

	public DebugEndpoint(DimensionReadAccess dimensionAccess, Dispatcher dispatcher) {
		this.dimensionAccess = dimensionAccess;
		this.dispatcher = dispatcher;
	}

	@GET
	public GetReply get() {
		List<RunnerInfo> runnerHashes = dispatcher.getKnownRunners().stream()
			.map(r -> new RunnerInfo(r.getName(), r.getVersionHash().orElse(null)))
			.collect(Collectors.toList());

		int amountOfForeignKeyViolatingDimensions = dimensionAccess
			.getAmountOfForeignKeyViolatingDimensions();

		return new GetReply(
			amountOfForeignKeyViolatingDimensions == 0,
			GitProperties.getBuildTime(),
			GitProperties.getHash(),
			runnerHashes,
			dimensionAccess.getAmountOfDimensions(),
			amountOfForeignKeyViolatingDimensions
		);
	}

	private static class GetReply {

		private final boolean safeToUpdate;
		private final String buildTime;
		private final String backendHash;
		private final List<RunnerInfo> runnerHashes;
		private final int amountOfDimensions;
		private final int amountOfForeignKeyViolatingDimensions;

		public GetReply(boolean safeToUpdate, String buildTime, String backendHash,
			List<RunnerInfo> runnerHashes, int amountOfDimensions,
			int amountOfForeignKeyViolatingDimensions) {

			this.safeToUpdate = safeToUpdate;
			this.buildTime = buildTime;
			this.backendHash = backendHash;
			this.runnerHashes = runnerHashes;
			this.amountOfDimensions = amountOfDimensions;
			this.amountOfForeignKeyViolatingDimensions = amountOfForeignKeyViolatingDimensions;
		}

		public boolean isSafeToUpdate() {
			return safeToUpdate;
		}

		public String getBuildTime() {
			return buildTime;
		}

		public String getBackendHash() {
			return backendHash;
		}

		public List<RunnerInfo> getRunnerHashes() {
			return runnerHashes;
		}

		public int getAmountOfDimensions() {
			return amountOfDimensions;
		}

		public int getAmountOfForeignKeyViolatingDimensions() {
			return amountOfForeignKeyViolatingDimensions;
		}
	}

	private static class RunnerInfo {

		private final String name;
		@Nullable
		private final String hash;

		public RunnerInfo(String name, @Nullable String hash) {
			this.name = name;
			this.hash = hash;
		}

		public String getName() {
			return name;
		}

		@Nullable
		public String getHash() {
			return hash;
		}
	}
}
