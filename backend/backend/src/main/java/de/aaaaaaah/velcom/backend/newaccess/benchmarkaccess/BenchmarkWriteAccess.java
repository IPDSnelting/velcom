package de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess;

import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;

public class BenchmarkWriteAccess extends BenchmarkReadAccess {

	public BenchmarkWriteAccess(DatabaseStorage databaseStorage) {
		super(databaseStorage);
	}
}
