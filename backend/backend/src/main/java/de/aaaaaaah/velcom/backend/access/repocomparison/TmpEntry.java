package de.aaaaaaah.velcom.backend.access.repocomparison;

import de.aaaaaaah.velcom.backend.access.benchmark.MeasurementValues;
import de.aaaaaaah.velcom.backend.access.commit.Commit;
import java.util.ArrayList;
import java.util.List;

public class TmpEntry {

	private final Commit commit;
	private List<Double> values;

	TmpEntry(Commit commit) {
		this.commit = commit;
		values = new ArrayList<>();
	}

	public Commit getCommit() {
		return commit;
	}

	public void addValue(double value) {
		values.add(value);
	}

	public boolean hasValue() {
		return !values.isEmpty();
	}

	public double getValue() {
		return MeasurementValues.getValue(values);
	}

	@Override
	public String toString() {
		return "TmpEntry{" +
			"commit=" + commit +
			", values=" + values +
			'}';
	}
}
