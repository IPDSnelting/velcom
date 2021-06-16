package de.aaaaaaah.velcom.backend.restapi.endpoints;

import static java.util.stream.Collectors.toList;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Run;
import de.aaaaaaah.velcom.backend.access.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.DimensionReadAccess;
import de.aaaaaaah.velcom.backend.access.repoaccess.RepoReadAccess;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Branch;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.data.significance.SignificanceFactors;
import de.aaaaaaah.velcom.backend.restapi.endpoints.utils.EndpointUtils;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRun;
import de.aaaaaaah.velcom.shared.util.Pair;
import io.micrometer.core.annotation.Timed;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Endpoint for getting the repo status comparison graph.
 */
@Path("/graph/status-comparison")
@Produces(MediaType.APPLICATION_JSON)
public class GraphStatusComparisonEndpoint {

	private final BenchmarkReadAccess benchmarkAccess;
	private final CommitReadAccess commitAccess;
	private final DimensionReadAccess dimensionAccess;
	private final RepoReadAccess repoAccess;
	private final SignificanceFactors significanceFactors;

	public GraphStatusComparisonEndpoint(BenchmarkReadAccess benchmarkAccess,
		CommitReadAccess commitAccess, DimensionReadAccess dimensionAccess, RepoReadAccess repoAccess,
		SignificanceFactors significanceFactors) {

		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
		this.dimensionAccess = dimensionAccess;
		this.repoAccess = repoAccess;
		this.significanceFactors = significanceFactors;
	}

	@GET
	@Timed(histogram = true)
	public GetReply get(@QueryParam("repos") @NotNull String reposStr) {
		List<JsonRepoEntry> runs = EndpointUtils.parseRepos(reposStr)
			.entrySet()
			.stream()
			.flatMap(entry -> {
				RepoId repoId = entry.getKey();
				List<Branch> branches = repoAccess.getAllBranches(repoId)
					.stream()
					.filter(branch -> entry.getValue().contains(branch.getName()))
					.collect(toList());

				if (branches.isEmpty()) {
					return Stream.of();
				}

				List<CommitHash> commitHashes = branches.stream()
					.map(Branch::getLatestCommitHash)
					.collect(toList());
				return commitAccess.getCommits(repoId, commitHashes)
					.stream()
					.max(Comparator.comparing(Commit::getCommitterDate))
					.stream();
			})
			.map(commit -> {
				Optional<Run> run = benchmarkAccess.getLatestRunId(commit.getRepoId(), commit.getHash())
					.map(benchmarkAccess::getRun);
				return new Pair<>(commit, run);
			})
			.map(pair -> {
				Commit commit = pair.getFirst();
				Optional<JsonRun> jsonRun = pair.getSecond()
					.map(run -> EndpointUtils
						.fromRun(dimensionAccess, commitAccess, run, significanceFactors, false));

				return new JsonRepoEntry(
					commit.getRepoId().getIdAsString(),
					commit.getHashAsString(),
					jsonRun.orElse(null)
				);
			})
			.collect(toList());

		return new GetReply(runs);
	}

	private static class GetReply {

		public final List<JsonRepoEntry> runs;

		public GetReply(List<JsonRepoEntry> runs) {
			this.runs = runs;
		}
	}

	private static class JsonRepoEntry {

		public final String repoId;
		public final String commitHash;
		@Nullable
		public final JsonRun run;

		public JsonRepoEntry(String repoId, String commitHash, @Nullable JsonRun run) {
			this.repoId = repoId;
			this.commitHash = commitHash;
			this.run = run;
		}
	}
}
