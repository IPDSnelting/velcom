package de.aaaaaaah.velcom.backend.newaccess;

import de.aaaaaaah.velcom.backend.access.benchmark.MeasurementName;
import de.aaaaaaah.velcom.backend.access.benchmark.Run;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;

public class BenchmarkWriteAccess extends BenchmarkReadAccess {

	public BenchmarkWriteAccess(DatabaseStorage databaseStorage) {
		super(databaseStorage);
	}

	public void insertRun(Run run) {
	}

	public void deleteAllMeasurementsOfName(RepoId repoId, MeasurementName measurementName) {
	}

}
