package de.aaaaaaah.velcom.backend.restapi.endpoints;

import static de.aaaaaaah.velcom.backend.util.MetricsUtils.timer;

import com.codahale.metrics.annotation.Timed;
import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.restapi.endpoints.utils.EndpointUtils;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonCommit;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonCommitDescription;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRunDescription;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRunDescription.JsonSuccess;
import io.micrometer.core.instrument.Timer;
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

	private static final Timer ENDPOINT_TIMER = timer("velcom.endpoint.commit.get");

	private final CommitReadAccess commitAccess;
	private final BenchmarkReadAccess benchmarkAccess;

	public CommitEndpoint(CommitReadAccess commitAccess, BenchmarkReadAccess benchmarkAccess) {
		this.commitAccess = commitAccess;
		this.benchmarkAccess = benchmarkAccess;
	}

	@GET
	@Timed
	public GetReply get(
		@PathParam("repoid") UUID repoUuid,
		@PathParam("hash") String hashString
	) {
		final var timer = Timer.start();

		RepoId repoId = new RepoId(repoUuid);
		CommitHash hash = new CommitHash(hashString);

		Commit commit = commitAccess.getCommit(repoId, hash);

		List<JsonCommitDescription> parents = commit.getParentHashes().stream()
			.map(parentHash -> commitAccess.getCommit(repoId, parentHash))
			.map(c -> new JsonCommitDescription(
				c.getRepoId().getId(),
				c.getHash().getHash(),
				c.getAuthor(),
				c.getAuthorDate().getEpochSecond(),
				c.getSummary()
			))
			.collect(Collectors.toList());

		List<JsonRunDescription> runs = benchmarkAccess.getAllRuns(repoId, hash).stream()
			.map(run -> new JsonRunDescription(
				run.getId().getId(),
				run.getStartTime().getEpochSecond(),
				JsonSuccess.fromRunResult(run.getResult()),
				EndpointUtils.convertToSource(commitAccess, run.getSource())
			))
			.collect(Collectors.toList());

		timer.stop(ENDPOINT_TIMER);

		return new GetReply(new JsonCommit(
			commit.getRepoId().getId(),
			commit.getHash().getHash(),
			parents,
			List.of(), // TODO implement getting children of a commit
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
