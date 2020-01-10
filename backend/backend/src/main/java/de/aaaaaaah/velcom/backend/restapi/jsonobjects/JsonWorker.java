package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.runner.KnownRunner;
import javax.annotation.Nullable;

/**
 * A helper class for serialization representing a benchmark runner.
 */
public class JsonWorker {

	private final String name;
	@Nullable
	private final JsonCommit workingOn;
	@Nullable
	private final String machineInfo;

	public JsonWorker(KnownRunner runner) {
		name = runner.getName();
		workingOn = runner.getCurrentCommit().map(JsonCommit::new).orElse(null);
		machineInfo = runner.getMachineInfo();
	}

	public String getName() {
		return name;
	}

	public JsonCommit getWorkingOn() {
		return workingOn;
	}
}
