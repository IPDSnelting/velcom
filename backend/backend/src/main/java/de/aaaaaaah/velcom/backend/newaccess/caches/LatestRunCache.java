package de.aaaaaaah.velcom.backend.newaccess.caches;

import static java.util.stream.Collectors.toMap;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.aaaaaaah.velcom.backend.access.entities.RunId;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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

	public Map<CommitHash, Optional<RunId>> getLatestRunIds(BenchmarkReadAccess benchmarkAccess,
		RepoId repoId,
		Collection<CommitHash> commitHashes) {

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
		);
	}

	public void invalidate(RepoId repoId, CommitHash commitHash) {
		getCacheForRepo(repoId).invalidate(commitHash);
	}

	public void invalidate(RepoId repoId) {
		cache.remove(repoId);
	}
}
