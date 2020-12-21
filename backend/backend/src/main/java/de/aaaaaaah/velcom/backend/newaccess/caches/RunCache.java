package de.aaaaaaah.velcom.backend.newaccess.caches;

import static java.util.stream.Collectors.toMap;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.RunId;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.exceptions.NoSuchRunException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

	public void invalidate(RunId runId) {
		cache.invalidate(runId);
	}

	public void invalidateAll() {
		cache.invalidateAll();
	}
}
