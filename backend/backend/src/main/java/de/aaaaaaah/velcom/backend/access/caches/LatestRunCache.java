package de.aaaaaaah.velcom.backend.access.caches;

import static java.util.stream.Collectors.toMap;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Run;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches the latest runs for a commit. Needs to be invalidated when runs are added or deleted, or
 * when repos are deleted.
 */
public class LatestRunCache {

	private static final int MAXIMUM_SIZE = 10000;

	private final ConcurrentHashMap<RepoId, Cache<CommitHash, Optional<RunId>>> cache;

	public LatestRunCache() {
		cache = new ConcurrentHashMap<>();
	}

	private Cache<CommitHash, Optional<RunId>> getCacheForRepo(RepoId repoId) {
		return cache.computeIfAbsent(
			repoId,
			missingRepoId -> Caffeine.newBuilder()
				.maximumSize(MAXIMUM_SIZE)
				.build()
		);
	}

	public Optional<RunId> getLatestRunId(BenchmarkReadAccess benchmarkAccess, RepoId repoId,
		CommitHash commitHash) {

		return getCacheForRepo(repoId).get(
			commitHash,
			missingHash -> benchmarkAccess.getLatestRunId(repoId, missingHash)
		);
	}

	public Map<CommitHash, RunId> getLatestRunIds(BenchmarkReadAccess benchmarkAccess,
		RepoId repoId, Collection<CommitHash> commitHashes) {

		return getCacheForRepo(repoId).getAll(
			commitHashes,
			missingHashesIterator -> {
				List<CommitHash> missingHashes = new ArrayList<>();
				missingHashesIterator.forEach(missingHashes::add);
				Map<CommitHash, RunId> missingIds = benchmarkAccess.getLatestRunIds(repoId, missingHashes);
				return missingHashes.stream()
					.collect(toMap(
						it -> it,
						it -> Optional.ofNullable(missingIds.get(it))
					));
			}
		).entrySet().stream()
			.filter(entry -> entry.getValue().isPresent())
			.collect(toMap(
				Entry::getKey,
				entry -> entry.getValue().get()
			));
	}

	public Optional<Run> getLatestRun(BenchmarkReadAccess benchmarkAccess, RunCache runCache,
		RepoId repoId, CommitHash commitHash) {

		return getLatestRunId(benchmarkAccess, repoId, commitHash)
			.map(runId -> runCache.getRun(benchmarkAccess, runId));
	}

	public Map<CommitHash, Run> getLatestRuns(BenchmarkReadAccess benchmarkAccess, RunCache runCache,
		RepoId repoId, Collection<CommitHash> commitHashes) {

		Map<CommitHash, RunId> latestRunIds = getLatestRunIds(benchmarkAccess, repoId, commitHashes);
		Map<RunId, Run> runs = runCache.getRuns(benchmarkAccess, latestRunIds.values());

		return latestRunIds.entrySet().stream()
			.collect(toMap(
				Entry::getKey,
				entry -> runs.get(entry.getValue())
			));
	}

	public void invalidate(RepoId repoId, CommitHash commitHash) {
		getCacheForRepo(repoId).invalidate(commitHash);
	}

	public void invalidate(RepoId repoId) {
		cache.remove(repoId);
	}
}
