package de.aaaaaaah.velcom.backend.data.recentruns;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Run;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.access.caches.LatestRunCache;
import de.aaaaaaah.velcom.backend.access.caches.RunCache;
import de.aaaaaaah.velcom.backend.access.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.FullCommit;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.DimensionReadAccess;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.data.significance.SignificanceDetector;
import de.aaaaaaah.velcom.backend.data.significance.SignificanceFactors;
import io.micrometer.core.annotation.Timed;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * This class detects significant runs.
 */
public class SignificantRunsCollector {

	private static final int BATCH_SIZE = 50;
	private static final int MAX_TRIES = 10;

	private final SignificanceFactors significanceFactors;
	private final BenchmarkReadAccess benchmarkAccess;
	private final CommitReadAccess commitAccess;
	private final DimensionReadAccess dimensionAccess;
	private final RunCache runCache;
	private final LatestRunCache latestRunCache;
	private final SignificanceDetector significanceDetector;

	public SignificantRunsCollector(SignificanceFactors significanceFactors,
		BenchmarkReadAccess benchmarkAccess, CommitReadAccess commitAccess,
		DimensionReadAccess dimensionAccess, RunCache runCache, LatestRunCache latestRunCache,
		SignificanceDetector significanceDetector) {

		this.significanceFactors = significanceFactors;
		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
		this.dimensionAccess = dimensionAccess;
		this.runCache = runCache;
		this.latestRunCache = latestRunCache;
		this.significanceDetector = significanceDetector;
	}

	/**
	 * Tries to fetch the {@link SignificantRun} for a given {@link Run}, if the run has any
	 * significant changes.
	 *
	 * @param run the run
	 * @return the significant run if it was significant, empty otherwise
	 */
	@Timed(histogram = true)
	public Optional<SignificantRun> getSignificantRun(Run run) {
		Optional<CommitSource> commitSource = run.getSource().getLeft();
		if (commitSource.isEmpty()) {
			return Optional.empty();
		}
		CommitSource source = commitSource.get();

		Set<CommitHash> parentHashes = commitAccess.getParentHashes(
			source.getRepoId(),
			source.getHash()
		);
		Collection<Run> parentRuns = latestRunCache
			.getLatestRuns(benchmarkAccess, runCache, source.getRepoId(), parentHashes)
			.values();

		Set<Dimension> significantDimensions = dimensionAccess.getSignificantDimensions();

		return getSignificantRun(run, Map.of(source, parentRuns), significantDimensions);
	}

	/**
	 * Find the specified amount of significant runs, starting at the most recent run and going
	 * backwards from there.
	 *
	 * @param amount the maximum amount of significant runs to return
	 * @return the significant runs which were found
	 */
	@Timed(histogram = true)
	public List<SignificantRun> collectMostRecent(int amount) {
		List<SignificantRun> runs = new ArrayList<>();

		for (int i = 0; i < MAX_TRIES && runs.size() < amount; i++) {
			runs.addAll(collectBatch(i * BATCH_SIZE));
		}

		return runs.subList(0, Math.min(amount, runs.size()));
	}

	private List<SignificantRun> collectBatch(int skip) {
		// 1. Load runs
		// 2. Load parent runs per run
		//   2.1. Load commits per RepoId
		//   2.2. Load parent runs per CommitSource
		// 3. Filter out significant runs

		List<RunId> recentRunIds = benchmarkAccess.getRecentRunIds(skip, BATCH_SIZE);
		List<Run> runs = runCache.getRunsInOrder(benchmarkAccess, recentRunIds);
		Map<RepoId, List<FullCommit>> commitsPerRepo = getCommitsPerRepo(runs);
		Map<CommitSource, Collection<Run>> parentRunsPerSource = getParentRunsPerSource(commitsPerRepo);
		Set<Dimension> significantDimensions = dimensionAccess.getSignificantDimensions();

		return runs.stream()
			.flatMap(run -> getSignificantRun(run, parentRunsPerSource, significantDimensions).stream())
			.collect(toList());
	}

	private Map<RepoId, List<FullCommit>> getCommitsPerRepo(Collection<Run> runs) {
		Map<RepoId, Set<CommitHash>> hashesByRepo = runs.stream()
			.flatMap(run -> run.getSource().getLeft().stream())
			.collect(groupingBy(CommitSource::getRepoId, mapping(CommitSource::getHash, toSet())));

		List<Commit> commits = hashesByRepo.entrySet().stream()
			.flatMap(entry -> commitAccess.getCommits(entry.getKey(), entry.getValue()).stream())
			.collect(toList());

		return commitAccess.promoteCommits(commits).stream()
			.collect(groupingBy(Commit::getRepoId, toList()));
	}

	private Map<CommitSource, Collection<Run>> getParentRunsPerSource(
		Map<RepoId, List<FullCommit>> commitsPerRepo) {

		Map<RepoId, Map<CommitHash, Run>> parentRuns = new HashMap<>();
		commitsPerRepo.forEach((repoId, commits) -> {
			Set<CommitHash> parentHashes = commits.stream()
				.flatMap(commit -> commit.getParentHashes().stream())
				.collect(toSet());
			Map<CommitHash, Run> latestRuns = latestRunCache
				.getLatestRuns(benchmarkAccess, runCache, repoId, parentHashes);
			parentRuns.put(repoId, latestRuns);
		});

		return commitsPerRepo.values().stream()
			.flatMap(Collection::stream)
			.collect(toMap(
				CommitSource::fromCommit,
				commit -> {
					// There will always be an entry for the repo id as long as commitsPerRepo doesn't contain
					// any commits under the wrong repo id
					Map<CommitHash, Run> runByHash = parentRuns.get(commit.getRepoId());

					return commit.getParentHashes().stream()
						// Ignoring parent commits without associated run, which do occur occasionally
						.flatMap(hash -> Optional.ofNullable(runByHash.get(hash)).stream())
						.collect(toList());
				}
			));
	}

	/**
	 * Check if a run is significant.
	 *
	 * @param run a run
	 * @param parentMap a map of all known commits' parent runs. Is not required to contain an entry
	 * 	for this particular run
	 * @param significantDimensions all significant dimensions
	 * @return a {@link SignificantRun} if the run is significant, {@link Optional#empty()} otherwise
	 */
	private Optional<SignificantRun> getSignificantRun(Run run,
		Map<CommitSource, Collection<Run>> parentMap, Set<Dimension> significantDimensions) {

		return run.getSource()
			.getLeft()
			.map(source -> Optional.ofNullable(parentMap.get(source)).orElse(List.of()))
			.flatMap(parents -> significanceDetector.getSignificance(run, parents, significantDimensions))
			.map(reasons -> new SignificantRun(run, reasons));
	}
}
