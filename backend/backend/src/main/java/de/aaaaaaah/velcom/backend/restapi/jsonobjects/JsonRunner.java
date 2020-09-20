package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.runner.KnownRunner;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

public class JsonRunner {

	private final String name;
	private final String info;
	@Nullable
	private final UUID workingOn;
	@Nullable
	private final Long workingSince;

	public JsonRunner(String name, String info, @Nullable UUID workingOn,
		@Nullable Long workingSince) {
		this.name = Objects.requireNonNull(name, "name can not be null!");
		this.info = Objects.requireNonNull(info, "info can not be null!");
		this.workingOn = workingOn;
		this.workingSince = workingSince;
	}

	public static JsonRunner fromKnownRunner(KnownRunner runner) {
		return new JsonRunner(
			runner.getName(),
			runner.getInformation(),
			runner.getCurrentTask().map(it -> it.getId().getId()).orElse(null),
			runner.getWorkingSince().map(Instant::getEpochSecond).orElse(null)
		);
	}


	public String getName() {
		return name;
	}

	public String getInfo() {
		return info;
	}

	@Nullable
	public UUID getWorkingOn() {
		return workingOn;
	}

	@Nullable
	public Long getWorkingSince() {
		return workingSince;
	}
}
