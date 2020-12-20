package de.aaaaaaah.velcom.backend.newaccess.caches;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.RunId;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.exceptions.NoSuchRunException;

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

	public void invalidate(RunId runId) {
		cache.invalidate(runId);
	}

	public void invalidateAll() {
		cache.invalidateAll();
	}
}
