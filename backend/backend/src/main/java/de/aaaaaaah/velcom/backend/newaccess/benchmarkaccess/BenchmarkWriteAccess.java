package de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess;

import de.aaaaaaah.velcom.backend.access.entities.benchmark.NewRun;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;

public class BenchmarkWriteAccess extends BenchmarkReadAccess {

	public BenchmarkWriteAccess(DatabaseStorage databaseStorage) {
		super(databaseStorage);
	}

	public void insertRun(NewRun result) {
		// TODO: 20.12.20 Implement
	}
}
