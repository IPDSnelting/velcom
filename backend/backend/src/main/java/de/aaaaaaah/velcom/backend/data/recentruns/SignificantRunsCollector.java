package de.aaaaaaah.velcom.backend.data.recentruns;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.data.runcomparison.DimensionDifference;
import de.aaaaaaah.velcom.backend.data.runcomparison.RunComparator;
import io.micrometer.core.annotation.Timed;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SignificantRunsCollector {

	private static final int BATCH_SIZE = 50;
	private static final int MAX_TRIES = 10;

	private final BenchmarkReadAccess benchmarkAccess;
	private final CommitReadAccess commitAccess;
	private final RunComparator runComparator;

	public SignificantRunsCollector(
		BenchmarkReadAccess benchmarkAccess,
		CommitReadAccess commitAccess,
		RunComparator runComparator) {
		this.benchmarkAccess = Objects.requireNonNull(benchmarkAccess);
		this.commitAccess = Objects.requireNonNull(commitAccess);
		this.runComparator = Objects.requireNonNull(runComparator);
	}

	@Timed(histogram = true)
	public List<SignificantRun> collectMostRecent(int amount) {
		List<SignificantRun> runs = new ArrayList<>();

		for (int i = 0; i < MAX_TRIES && runs.size() < amount; i++) {
			runs.addAll(collectBatch(i * BATCH_SIZE, BATCH_SIZE));
		}

		return runs.subList(0, Math.min(amount, runs.size()));
	}

	private List<SignificantRun> collectBatch(int skip, int amount) {
		List<SignificantRun> batchResult = new ArrayList<>();

		// 1.) Load the runs for this batch from database
		List<Run> runs = benchmarkAccess.getRecentRuns(skip, amount).stream()
			.filter(run -> run.getSource().isLeft())
			.collect(toCollection(ArrayList::new));

		if (runs.isEmpty()) {
			return emptyList();
		}

		// Runs that failed entirely are already significant and don't need comparisons
		runs.stream()
			.filter(run -> run.getResult().isLeft()) // run has RunError instead of measurements
			.forEach(failedRun -> batchResult.add(new SignificantRun(failedRun, emptyList())));
		runs.removeIf(run -> run.getResult().isLeft()); // no need to load parent runs for comparisons

		Map<RepoId, Set<CommitHash>> groupedCommits = runs.stream()
			.map(run -> run.getSource().getLeft().get())
			.collect(groupingBy(CommitSource::getRepoId, mapping(CommitSource::getHash, toSet())));

		// 2.) Get parent runs for the collected runs
		Map<CommitSource, Collection<CommitHash>> parentMap = new HashMap<>();
		Map<CommitSource, Run> parentRuns = new HashMap<>();

		for (RepoId repoId : groupedCommits.keySet()) {
			// 2.1) Load commits to find out what hashes the parent runs have
			Set<CommitHash> hashesForThisRepo = groupedCommits.get(repoId);
			Set<CommitHash> parentHashesForThisRepo = new HashSet<>();

			commitAccess.getCommits(repoId, hashesForThisRepo).values().forEach(commit -> {
				parentMap.put(new CommitSource(repoId, commit.getHash()), commit.getParentHashes());
				parentHashesForThisRepo.addAll(commit.getParentHashes());
			});

			// 2.2) Load parent runs from database
			benchmarkAccess.getLatestRuns(repoId, parentHashesForThisRepo).forEach((hash, run) -> {
				parentRuns.put(new CommitSource(repoId, hash), run);
			});
		}

		// 3.) Compare commits with their parents
		for (Run run : runs) {
			CommitSource source = run.getSource().getLeft().get();
			Collection<CommitHash> parentHashes = parentMap.get(source);

			if (parentHashes == null) {
				// This run is referencing a commit that either does not exist anymore or has no parents
				continue;
			}

			List<DimensionDifference> differences = parentHashes.stream()
				.map(parentHash -> new CommitSource(source.getRepoId(), parentHash))
				.filter(parentRuns::containsKey)
				.map(parentRuns::get)
				.map(parentRun -> runComparator.compare(parentRun, run))
				.flatMap(comparison -> comparison.getDifferences().stream())
				.filter(DimensionDifference::isSignificant)
				.collect(toList());

			if (!differences.isEmpty()) {
				batchResult.add(new SignificantRun(run, differences));
			}
		}

		return batchResult;
	}

}
