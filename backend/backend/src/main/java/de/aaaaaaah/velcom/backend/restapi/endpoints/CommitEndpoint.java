package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.FullCommit;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.RepoReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonCommit;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonCommitDescription;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRunDescription;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRunDescription.JsonSuccess;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonSource;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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

	private final CommitReadAccess commitAccess;
	private final RepoReadAccess repoAccess;
	private final BenchmarkReadAccess benchmarkAccess;

	public CommitEndpoint(CommitReadAccess commitAccess, RepoReadAccess repoAccess,
		BenchmarkReadAccess benchmarkAccess) {

		this.commitAccess = commitAccess;
		this.repoAccess = repoAccess;
		this.benchmarkAccess = benchmarkAccess;
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

		List<JsonCommitDescription> parents = commitAccess
			.getCommits(repoId, commit.getParentHashes())
			.stream()
			.map(JsonCommitDescription::fromCommit)
			.collect(Collectors.toList());

		List<Commit> childCommits = commitAccess.getCommits(repoId, commit.getChildHashes());
		List<JsonCommitDescription> trackedChildren = childCommits.stream()
			.filter(Commit::isTracked)
			.map(JsonCommitDescription::fromCommit)
			.collect(Collectors.toList());
		List<JsonCommitDescription> untrackedChildren = childCommits.stream()
			.filter(it -> !it.isTracked())
			.map(JsonCommitDescription::fromCommit)
			.collect(Collectors.toList());

		List<JsonRunDescription> runs = benchmarkAccess.getAllRuns(repoId, hash).stream()
			.map(run -> new JsonRunDescription(
				run.getId().getId(),
				run.getStartTime().getEpochSecond(),
				JsonSuccess.fromRunResult(run.getResult()),
				JsonSource.fromSource(run.getSource(), commitAccess)
			))
			.collect(Collectors.toList());

		return new GetReply(new JsonCommit(
			commit.getRepoId().getId(),
			commit.getHash().getHash(),
			parents,
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
}
