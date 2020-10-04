package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.RepoReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.Branch;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonCommit;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonCommitDescription;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRunDescription;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRunDescription.JsonSuccess;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonSource;
import io.micrometer.core.annotation.Timed;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

		Commit commit = commitAccess.getCommit(repoId, hash);

		List<JsonCommitDescription> parents = commit.getParentHashes().stream()
			.map(parentHash -> commitAccess.getCommit(repoId, parentHash))
			.map(JsonCommitDescription::fromCommit)
			.collect(Collectors.toList());

		List<Branch> allBranches = repoAccess.getAllBranches(repoId);
		Set<BranchName> allNames = allBranches.stream()
			.map(Branch::getName)
			.collect(Collectors.toSet());
		Set<BranchName> trackedNames = allBranches.stream()
			.filter(Branch::isTracked)
			.map(Branch::getName)
			.collect(Collectors.toSet());

		// If the commit can't be reached from any tracked branch, none of its children can either. so
		// an empty list of tracked child hashes is appropriate.
		Set<CommitHash> trackedChildHashes = new HashSet<>(
			commitAccess.getChildren(repoId, hash, trackedNames).orElse(List.of()));
		// If the commit can't be reached from any branch, none of its children can either, so an empty
		// list of (untracked) child hashes is appropriate. Otherwise, commits like GitHub's auto
		// generated merge commits would be visible as children.
		Set<CommitHash> untrackedChildHashes = new HashSet<>(
			commitAccess.getChildren(repoId, hash, allNames).orElse(List.of()));
		untrackedChildHashes.removeAll(trackedChildHashes);

		List<JsonCommitDescription> trackedChildren = trackedChildHashes.stream()
			.map(childHash -> commitAccess.getCommit(repoId, childHash))
			.map(JsonCommitDescription::fromCommit)
			.collect(Collectors.toList());
		List<JsonCommitDescription> untrackedChildren = untrackedChildHashes.stream()
			.map(childHash -> commitAccess.getCommit(repoId, childHash))
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
