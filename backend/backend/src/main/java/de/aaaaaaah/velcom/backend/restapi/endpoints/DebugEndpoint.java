package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.BenchmarkReadAccess;
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

	private final BenchmarkReadAccess benchmarkAccess;
	private final Dispatcher dispatcher;

	public DebugEndpoint(BenchmarkReadAccess benchmarkAccess, Dispatcher dispatcher) {
		this.benchmarkAccess = benchmarkAccess;
		this.dispatcher = dispatcher;
	}

	@GET
	public GetReply get() {
		List<RunnerInfo> runnerHashes = dispatcher.getKnownRunners().stream()
			.map(r -> new RunnerInfo(r.getName(), r.getVersionHash().orElse(null)))
			.collect(Collectors.toList());

		long tarNotAttached = benchmarkAccess.getAmountOfTarRunsNotAttachedToRepo();
		long tarAttached = benchmarkAccess.getAmountOfTarRunsAttachedToRepo();
		Stats stats = new Stats(
			benchmarkAccess.getAmountOfOutdatedRuns(),
			tarNotAttached + tarAttached,
			tarNotAttached,
			tarAttached,
			benchmarkAccess.getAmountOfFailedRuns(),
			benchmarkAccess.getAmountOfFailedRunErrorMsgChars(),
			benchmarkAccess.getAmountOfRunsForUntrackedCommits(),
			benchmarkAccess.getAmountOfRunsForUnreachableCommits()
		);

		return new GetReply(
			true,
			GitProperties.getBuildTime(),
			GitProperties.getHash(),
			runnerHashes,
			stats
		);
	}

	private static class GetReply {

		public final boolean safeToUpdate;
		public final String buildTime;
		public final String backendHash;
		public final List<RunnerInfo> runnerHashes;
		public final Stats stats;

		public GetReply(boolean safeToUpdate, String buildTime, String backendHash,
			List<RunnerInfo> runnerHashes, Stats stats) {

			this.safeToUpdate = safeToUpdate;
			this.buildTime = buildTime;
			this.backendHash = backendHash;
			this.runnerHashes = runnerHashes;
			this.stats = stats;
		}
	}

	private static class RunnerInfo {

		public final String name;
		@Nullable
		public final String hash;

		public RunnerInfo(String name, @Nullable String hash) {
			this.name = name;
			this.hash = hash;
		}
	}

	private static class Stats {

		public final long outdatedRuns;
		public final long tarRunsTotal;
		public final long tarRunsNotAttachedToRepo;
		public final long tarRunsAttachedToRepo;
		public final long failedRuns;
		public final long failedRunErrorMsgChars;
		public final long runsForUntrackedCommits;
		public final long runsForUnreachableCommits;

		public Stats(long outdatedRuns, long tarRunsTotal, long tarRunsNotAttachedToRepo,
			long tarRunsAttachedToRepo, long failedRuns, long failedRunErrorMsgChars,
			long runsForUntrackedCommits, long runsForUnreachableCommits) {

			this.outdatedRuns = outdatedRuns;
			this.tarRunsTotal = tarRunsTotal;
			this.tarRunsNotAttachedToRepo = tarRunsNotAttachedToRepo;
			this.tarRunsAttachedToRepo = tarRunsAttachedToRepo;
			this.failedRuns = failedRuns;
			this.failedRunErrorMsgChars = failedRunErrorMsgChars;
			this.runsForUntrackedCommits = runsForUntrackedCommits;
			this.runsForUnreachableCommits = runsForUnreachableCommits;
		}
	}
}
