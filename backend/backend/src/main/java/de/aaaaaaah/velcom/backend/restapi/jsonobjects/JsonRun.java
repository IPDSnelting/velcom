package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.access.benchmark.Run;
import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitAccessException;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * A helper class for serialization representing a run.
 */
public class JsonRun {

	private final JsonCommit commit;
	private final long start_time;
	private final long stop_time;
	@Nullable
	private final Collection<JsonMeasurement> measurements;
	@Nullable
	private final String errorMessage;

	public JsonRun(Run run, Commit commit) {
		this.commit = new JsonCommit(commit);

		start_time = run.getStartTime().getEpochSecond();
		stop_time = run.getStopTime().getEpochSecond();

		measurements = run.getMeasurements().map(
			m -> m.stream()
				.map(JsonMeasurement::new)
				.collect(Collectors.toUnmodifiableList())
		).orElse(null);

		errorMessage = run.getErrorMessage().orElse(null);
	}

	public JsonRun(Run run) throws CommitAccessException {
		this(run, run.getCommit());
	}

	public JsonCommit getCommit() {
		return commit;
	}

	public long getStart_time() {
		return start_time;
	}

	public long getStop_time() {
		return stop_time;
	}

	public Collection<JsonMeasurement> getMeasurements() {
		return measurements;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

}
