package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.RunError;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * A helper class for serialization representing a run.
 */
public class JsonRun {

	private final JsonCommit commit;
	private final long startTime;
	private final long stopTime;
	@Nullable
	private final Collection<JsonMeasurement> measurements;
	@Nullable
	private final String errorMessage;

	public JsonRun(Run run, Commit commit) {
		this.commit = new JsonCommit(commit);

		startTime = run.getStartTime().getEpochSecond();
		stopTime = run.getStopTime().getEpochSecond();

		measurements = run.getResult().getLeft().map(
			m -> m.stream()
				.map(JsonMeasurement::new)
				.collect(Collectors.toUnmodifiableList())
		).orElse(null);

		errorMessage = run.getResult().getRight().map(RunError::getMessage).orElse(null);
	}

	public JsonCommit getCommit() {
		return commit;
	}

	public long getStartTime() {
		return startTime;
	}

	public long getStopTime() {
		return stopTime;
	}

	public Collection<JsonMeasurement> getMeasurements() {
		return measurements;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

}
