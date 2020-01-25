package de.aaaaaaah.velcom.backend.access.repocomparison;

import de.aaaaaaah.velcom.backend.access.benchmark.MeasurementName;
import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRun;
import java.util.HashMap;
import java.util.Map;

public class TmpCommit {

	private final String repoId;
	private final Commit commit;
	private final Map<MeasurementName, TmpMeasurement> measurements;

	TmpCommit(String repoId, Commit commit) {
		this.repoId = repoId;
		this.commit = commit;

		measurements = new HashMap<>();
	}

	public String getRepoId() {
		return repoId;
	}

	public Commit getCommit() {
		return commit;
	}

	public Map<MeasurementName, TmpMeasurement> getMeasurements() {
		return measurements;
	}

	public void addMeasurement(MeasurementName measurementName, TmpMeasurement tmpMeasurement) {
		measurements.put(measurementName, tmpMeasurement);
	}

	public TmpMeasurement getMeasurement(MeasurementName measurementName) {
		return measurements.get(measurementName);
	}

	@Override
	public String toString() {
		return "TmpCommit{" +
			"repoId='" + repoId + '\'' +
			", commit=" + commit +
			", measurements=" + measurements +
			'}';
	}
}
