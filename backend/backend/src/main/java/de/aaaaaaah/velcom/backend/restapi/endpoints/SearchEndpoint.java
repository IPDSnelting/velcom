package de.aaaaaaah.velcom.backend.restapi.endpoints;

import static java.util.stream.Collectors.toList;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonCommitDescription;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonShortRunDescription;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/search")
@Produces(MediaType.APPLICATION_JSON)
public class SearchEndpoint {

	private static final int SEARCH_LIMIT = 500;

	private final BenchmarkReadAccess benchmarkAccess;
	private final CommitReadAccess commitAccess;

	public SearchEndpoint(BenchmarkReadAccess benchmarkAccess, CommitReadAccess commitAccess) {
		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
	}

	@GET
	@Timed(histogram = true)
	public SearchGetReply get(
		@QueryParam("limit") @Nullable Integer maybeLimit,
		@QueryParam("repo_id") @Nullable UUID repoUuid,
		@QueryParam("query") @NotNull String query
	) {
		Optional<RepoId> repoId = Optional.ofNullable(repoUuid).map(RepoId::new);
		int limit = Optional.ofNullable(maybeLimit).orElse(SEARCH_LIMIT);

		List<JsonCommitDescription> commits = commitAccess
			.searchCommits(limit, repoId.orElse(null), query).stream()
			.map(JsonCommitDescription::fromCommit).collect(toList());

		limit = Math.max(0, limit - commits.size());

		List<JsonShortRunDescription> runs = benchmarkAccess
			.searchRuns(limit, repoId.orElse(null), query).stream()
			.map(JsonShortRunDescription::fromShortRunDescription).collect(toList());

		return new SearchGetReply(commits, runs);
	}

	private static class SearchGetReply {

		public final List<JsonCommitDescription> commits;
		public final List<JsonShortRunDescription> runs;

		public SearchGetReply(List<JsonCommitDescription> commits, List<JsonShortRunDescription> runs) {
			this.commits = commits;
			this.runs = runs;
		}
	}
}
