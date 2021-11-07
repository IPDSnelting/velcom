package de.aaaaaaah.velcom.backend.access.dimensionaccess;

import static org.jooq.codegen.db.tables.Dimension.DIMENSION;

import de.aaaaaaah.velcom.backend.access.caches.AvailableDimensionsCache;
import de.aaaaaaah.velcom.backend.access.caches.RunCache;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.storage.db.DBWriteAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;

public class DimensionWriteAccess extends DimensionReadAccess {

	private final AvailableDimensionsCache availableDimensionsCache;
	private final RunCache runCache;

	public DimensionWriteAccess(DatabaseStorage databaseStorage,
		AvailableDimensionsCache availableDimensionsCache, RunCache runCache) {

		super(databaseStorage);

		this.availableDimensionsCache = availableDimensionsCache;
		this.runCache = runCache;
	}

	public void deleteDimension(Dimension dimension) {
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.dsl()
				.deleteFrom(DIMENSION)
				.where(DIMENSION.BENCHMARK.eq(dimension.getBenchmark()))
				.and(DIMENSION.METRIC.eq(dimension.getMetric()))
				.execute();
		}

		availableDimensionsCache.invalidateAll();
		runCache.invalidateAll();
	}
}
