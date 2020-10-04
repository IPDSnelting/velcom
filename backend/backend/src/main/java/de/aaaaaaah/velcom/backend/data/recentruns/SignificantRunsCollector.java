package de.aaaaaaah.velcom.backend.data.recentruns;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.data.runcomparison.DimensionDifference;
import de.aaaaaaah.velcom.backend.data.runcomparison.RunComparator;
import de.aaaaaaah.velcom.backend.data.runcomparison.SignificanceFactors;
import io.micrometer.core.annotation.Timed;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class SignificantRunsCollector {

	private static final int BATCH_SIZE = 50;
	private static final int MAX_TRIES = 10;

	private final SignificanceFactors significanceFactors;
	private final BenchmarkReadAccess benchmarkAccess;
	private final CommitReadAccess commitAccess;
	private final RunComparator runComparator;

	public SignificantRunsCollector(SignificanceFactors significanceFactors,
		BenchmarkReadAccess benchmarkAccess, CommitReadAccess commitAccess,
		RunComparator runComparator) {

		this.significanceFactors = significanceFactors;
		this.benchmarkAccess = Objects.requireNonNull(benchmarkAccess);
		this.commitAccess = Objects.requireNonNull(commitAccess);
		this.runComparator = Objects.requireNonNull(runComparator);
	}

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

		List<Run> runs = benchmarkAccess.getRecentRuns(skip, SignificantRunsCollector.BATCH_SIZE);
		Map<RepoId, Collection<Commit>> commitsPerRepo = getCommitsPerRepo(runs);
		Map<CommitSource, Collection<Run>> parentRunsPerSource = getParentRunsPerSource(commitsPerRepo);

		return runs.stream()
			.flatMap(run -> getSignificantRun(run, parentRunsPerSource).stream())
			.collect(toList());
	}

	private Map<RepoId, Collection<Commit>> getCommitsPerRepo(Collection<Run> runs) {
		Map<RepoId, Set<CommitHash>> hashesByRepo = runs.stream()
			.flatMap(run -> run.getSource().getLeft().stream())
			.collect(groupingBy(CommitSource::getRepoId, mapping(CommitSource::getHash, toSet())));

		Map<RepoId, Collection<Commit>> result = new HashMap<>();
		hashesByRepo.forEach((repoId, hashes) -> {
			Collection<Commit> commits = commitAccess.getCommits(repoId, hashes).values();
			result.put(repoId, commits);
		});

		return result;
	}

	private Map<CommitSource, Collection<Run>> getParentRunsPerSource(
		Map<RepoId, Collection<Commit>> commitsPerRepo) {

		Map<RepoId, Map<CommitHash, Run>> parentRuns = new HashMap<>();
		commitsPerRepo.forEach((repoId, commits) -> {
			Set<CommitHash> parentHashes = commits.stream()
				.flatMap(commit -> commit.getParentHashes().stream())
				.collect(toSet());
			parentRuns.put(repoId, benchmarkAccess.getLatestRuns(repoId, parentHashes));
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
	 * @param run a run
	 * @param parents a map of all known commits' parent runs. Is not required to contain an entry
	 * 	for this particular run
	 * @return a {@link SignificantRun} if the run is significant, {@link Optional#empty()} otherwise
	 */
	private Optional<SignificantRun> getSignificantRun(Run run,
		Map<CommitSource, Collection<Run>> parents) {

		List<DimensionDifference> significantDifferences = run.getSource().getLeft()
			.flatMap(source -> Optional.ofNullable(parents.get(source)))
			.stream()
			.flatMap(Collection::stream)
			.map(parent -> runComparator.compare(parent, run))
			.flatMap(comparison -> comparison.getDifferences().stream())
			.filter(this::isDifferenceSignificant)
			.collect(toList());

		if (hasFails(run) || !significantDifferences.isEmpty()) {
			return Optional.of(new SignificantRun(run, significantDifferences));
		} else {
			return Optional.empty();
		}
	}

	/**
	 * @return true if the run has failed measurements or is entirely failed
	 */
	private boolean hasFails(Run run) {
		return run.getResult().getRight()
			.map(ms -> ms.stream().anyMatch(m -> m.getContent().isLeft()))
			.orElse(true);
	}

	/**
	 * @param diff the difference to check
	 * @return true if the difference is significant according to the {@code significanceFactors}
	 */
	private boolean isDifferenceSignificant(DimensionDifference diff) {
		boolean relSignificant = diff.getReldiff()
			.map(reldiff -> Math.abs(reldiff) >= significanceFactors.getRelativeThreshold())
			// There is no reldiff if the first value is 0. But if the second value is also zero, that
			// hardly constitutes a significant difference. Otherwise, it is a move away from 0, which is
			// always significant.
			.orElse(diff.getFirst() != diff.getSecond());

		boolean stddevSignificant = diff.getSecondStddev()
			.map(stddev -> Math.abs(diff.getDiff()) >= significanceFactors.getStddevThreshold() * stddev)
			// If there is no stddev, this check should not prevent differences from being significant
			.orElse(true);

		return relSignificant && stddevSignificant;
	}
}