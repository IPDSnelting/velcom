package de.aaaaaaah.velcom.backend.access.caches;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Run;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.exceptions.NoSuchRunException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Caches runs by their id. Needs to be invalidated when runs or repos are deleted (but not when
 * runs are added, since it only stores those runs that exist).
 */
// TODO: 21.12.20 Also store which runs don't exist?
public class RunCache {

	private static final int MAXIMUM_SIZE = 10000;

	private final Cache<RunId, Run> cache;

	public RunCache() {
		cache = Caffeine.newBuilder()
			.maximumSize(MAXIMUM_SIZE)
			.build();
	}

	public Run getRun(BenchmarkReadAccess benchmarkAccess, RunId runId) throws NoSuchRunException {
		return cache.get(runId, benchmarkAccess::getRun);
	}

	public Map<RunId, Run> getRuns(BenchmarkReadAccess benchmarkAccess, Collection<RunId> runIds) {
		return cache.getAll(
			runIds,
			missingIdIter -> {
				List<RunId> missingIds = new ArrayList<>();
				missingIdIter.forEach(missingIds::add);
				return benchmarkAccess.getRuns(missingIds).stream()
					.collect(toMap(Run::getId, it -> it));
			}
		);
	}

	public List<Run> getRunsInOrder(BenchmarkReadAccess benchmarkReadAccess,
		Collection<RunId> runIds) {

		Map<RunId, Run> runs = getRuns(benchmarkReadAccess, runIds);
		return runIds.stream()
			.map(runs::get)
			.filter(Objects::nonNull)
			.collect(toList());
	}

	public void invalidate(RunId runId) {
		cache.invalidate(runId);
	}

	public void invalidateAll() {
		cache.invalidateAll();
	}
}
