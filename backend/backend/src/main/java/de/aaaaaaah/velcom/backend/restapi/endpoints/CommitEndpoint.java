package de.aaaaaaah.velcom.backend.restapi.endpoints;

import static java.util.stream.Collectors.toList;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.caches.RunCache;
import de.aaaaaaah.velcom.backend.access.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.FullCommit;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonCommit;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonCommitDescription;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRunDescription;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRunDescription.JsonSuccess;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonSource;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Endpoint for retrieving detailed commit info.
 */
@Path("/commit/{repoid}/{hash}")
@Produces(MediaType.APPLICATION_JSON)
public class CommitEndpoint {

	private final BenchmarkReadAccess benchmarkAccess;
	private final CommitReadAccess commitAccess;
	private final RunCache runCache;

	public CommitEndpoint(BenchmarkReadAccess benchmarkAccess, CommitReadAccess commitAccess,
		RunCache runCache) {

		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
		this.runCache = runCache;
	}

	@GET
	@Timed(histogram = true)
	public GetReply get(
		@PathParam("repoid") UUID repoUuid,
		@PathParam("hash") String hashString
	) {
		RepoId repoId = new RepoId(repoUuid);
		CommitHash hash = new CommitHash(hashString);

		FullCommit commit = commitAccess.getFullCommit(repoId, hash);

		List<Commit> parentCommits = commitAccess.getCommits(repoId, commit.getParentHashes());
		List<JsonCommitDescription> trackedParents = parentCommits.stream()
			.filter(Commit::isTracked)
			.map(JsonCommitDescription::fromCommit)
			.collect(toList());
		List<JsonCommitDescription> untrackedParents = parentCommits.stream()
			.filter(it -> !it.isTracked())
			.map(JsonCommitDescription::fromCommit)
			.collect(toList());

		List<Commit> childCommits = commitAccess.getCommits(repoId, commit.getChildHashes());
		List<JsonCommitDescription> trackedChildren = childCommits.stream()
			.filter(Commit::isReachable)
			.filter(Commit::isTracked)
			.map(JsonCommitDescription::fromCommit)
			.collect(toList());
		List<JsonCommitDescription> untrackedChildren = childCommits.stream()
			.filter(Commit::isReachable)
			.filter(it -> !it.isTracked())
			.map(JsonCommitDescription::fromCommit)
			.collect(toList());

		List<RunId> runIds = benchmarkAccess.getAllRunIds(repoId, hash);
		List<JsonRunDescription> runs = runCache.getRunsInOrder(benchmarkAccess, runIds).stream()
			.map(run -> new JsonRunDescription(
				run.getId().getId(),
				run.getStartTime().getEpochSecond(),
				JsonSuccess.fromRunResult(run.getResult()),
				JsonSource.fromSource(run.getSource(), commitAccess)
			))
			.collect(toList());

		return new GetReply(new JsonCommit(
			commit.getRepoId().getId(),
			commit.getHash().getHash(),
			commit.isTracked(),
			trackedParents,
			untrackedParents,
			trackedChildren,
			untrackedChildren,
			commit.getAuthor(),
			commit.getAuthorDate().getEpochSecond(),
			commit.getCommitter(),
			commit.getCommitterDate().getEpochSecond(),
			commit.getSummary(),
			commit.getMessageWithoutSummary().orElse(null),
			runs
		));
	}

	private static class GetReply {

		public final JsonCommit commit;

		public GetReply(JsonCommit commit) {
			this.commit = commit;
		}
	}

	@Path("/short")
	@GET
	@Timed(histogram = true)
	public GetShortReply getShort(
		@PathParam("repoid") UUID repoUuid,
		@PathParam("hash") String hashString
	) {
		RepoId repoId = new RepoId(repoUuid);
		CommitHash commitHash = new CommitHash(hashString);

		Commit commit = commitAccess.getCommit(repoId, commitHash);
		return new GetShortReply(commit.getSummary());
	}

	private static class GetShortReply {

		public final String summary;

		public GetShortReply(String summary) {
			this.summary = summary;
		}
	}
}
