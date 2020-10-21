package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.newaccess.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.RepoReadAccess;
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

	private final CommitReadAccess commitAccess;
	private final RepoReadAccess repoAccess;

	private final Dispatcher dispatcher;

	public DebugEndpoint(CommitReadAccess commitAccess, RepoReadAccess repoAccess,
		Dispatcher dispatcher) {

		this.commitAccess = commitAccess;
		this.repoAccess = repoAccess;

		this.dispatcher = dispatcher;
	}

	@GET
	public GetReply get() {
		List<RunnerInfo> runnerHashes = dispatcher.getKnownRunners().stream()
			.map(r -> new RunnerInfo(r.getName(), r.getVersionHash().orElse(null)))
			.collect(Collectors.toList());

		int unmigratedBranches = repoAccess.getUnmigratedBranchCount();
		int unmigratedCommits = commitAccess.getUnmigratedCommitCount();
		boolean safeToMigrate = unmigratedBranches == 0 && unmigratedCommits == 0;

		return new GetReply(
			safeToMigrate,
			GitProperties.getBuildTime(),
			GitProperties.getHash(),
			runnerHashes,
			unmigratedBranches,
			unmigratedCommits
		);
	}

	private static class GetReply {

		private final boolean safeToUpdate;
		private final String buildTime;
		private final String backendHash;
		private final List<RunnerInfo> runnerHashes;
		private final int unmigratedBranches;
		private final int unmigratedCommits;

		public GetReply(boolean safeToUpdate, String buildTime, String backendHash,
			List<RunnerInfo> runnerHashes, int unmigratedBranches, int unmigratedCommits) {

			this.safeToUpdate = safeToUpdate;
			this.buildTime = buildTime;
			this.backendHash = backendHash;
			this.runnerHashes = runnerHashes;
			this.unmigratedBranches = unmigratedBranches;
			this.unmigratedCommits = unmigratedCommits;
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

		public int getUnmigratedBranches() {
			return unmigratedBranches;
		}

		public int getUnmigratedCommits() {
			return unmigratedCommits;
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
