package de.aaaaaaah.velcom.backend.access.dimensionaccess;

import static org.jooq.codegen.db.tables.Dimension.DIMENSION;

import de.aaaaaaah.velcom.backend.access.caches.AvailableDimensionsCache;
import de.aaaaaaah.velcom.backend.access.caches.RunCache;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.exceptions.NoSuchDimensionException;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.util.Collection;

public class DimensionWriteAccess extends DimensionReadAccess {

	private final AvailableDimensionsCache availableDimensionsCache;
	private final RunCache runCache;

	public DimensionWriteAccess(DatabaseStorage databaseStorage,
		AvailableDimensionsCache availableDimensionsCache, RunCache runCache) {

		super(databaseStorage);

		this.availableDimensionsCache = availableDimensionsCache;
		this.runCache = runCache;
	}

	/**
	 * Deletes the given dimensions atomically. If any dimension does not exist, <em>nothing</em> is
	 * deleted and an exception is raised.
	 *
	 * @param dimensions the dimensions to delete
	 * @throws NoSuchDimensionException if a dimension does not exist
	 */
	public void deleteDimensions(Collection<Dimension> dimensions) {
		databaseStorage.acquireWriteTransaction(db -> {
			// This is not a good idea with most databases, but should be fine with SQLite as it is
			// in-process.
			for (Dimension dimension : dimensions) {
				int affectedRows = db.dsl()
					.deleteFrom(DIMENSION)
					.where(DIMENSION.BENCHMARK.eq(dimension.getBenchmark()))
					.and(DIMENSION.METRIC.eq(dimension.getMetric()))
					.execute();

				if (affectedRows == 0) {
					throw new NoSuchDimensionException(dimension);
				}
			}
		});

		availableDimensionsCache.invalidateAll();
		runCache.invalidateAll();
	}
}
