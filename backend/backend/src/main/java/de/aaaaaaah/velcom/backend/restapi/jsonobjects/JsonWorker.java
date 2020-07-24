package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

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

	public JsonWorker(String name, @Nullable JsonCommit workingOn, @Nullable String machineInfo) {
		this.name = name;
		this.workingOn = workingOn;
		this.machineInfo = machineInfo;
	}

	public String getName() {
		return name;
	}

	@Nullable
	public JsonCommit getWorkingOn() {
		return workingOn;
	}

	@Nullable
	public String getMachineInfo() {
		return machineInfo;
	}
}
