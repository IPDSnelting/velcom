package de.aaaaaaah.velcom.backend.access.benchmark;

import de.aaaaaaah.velcom.backend.access.commit.CommitAccess;
import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.RepoAccess;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

class TmpRun {

	private final RunId id;
	private final RepoId repoId;
	private final CommitHash commitHash;
	private final Instant startTime;
	private final Instant stopTime;
	@Nullable
	private final String errorMessage;
	private final Map<MeasurementName, TmpMeasurement> measurements;

	TmpRun(RunId id, RepoId repoId, CommitHash commitHash, Instant startTime,
		Instant stopTime, @Nullable String errorMessage) {

		this.id = id;
		this.repoId = repoId;
		this.commitHash = commitHash;
		this.startTime = startTime;
		this.stopTime = stopTime;
		this.errorMessage = errorMessage;

		measurements = new HashMap<>();
	}

	public RunId getId() {
		return id;
	}

	public Map<MeasurementName, TmpMeasurement> getMeasurements() {
		return measurements;
	}

	public Run toRun(BenchmarkAccess benchmarkAccess, CommitAccess commitAccess,
		RepoAccess repoAccess) {

		final List<Measurement> measurements = this.measurements.values().stream()
			.map(measurement -> measurement.toMeasurement(benchmarkAccess))
			.collect(Collectors.toUnmodifiableList());

		return new Run(commitAccess, repoAccess, id, repoId, commitHash, startTime,
			stopTime, errorMessage, (errorMessage == null) ? measurements : null);
	}
}
