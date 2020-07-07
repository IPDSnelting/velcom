package de.aaaaaaah.velcom.runner.revision.benchmarking;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class Benchmarker {

	private final UUID runId;
	private final AtomicReference<BenchResult> result; // nullable inside the reference

	private final Thread thread;

	public Benchmarker(UUID runId) {
		this.runId = runId;
		result = new AtomicReference<>();

		thread = null; // TODO implement
	}

	public UUID getRunId() {
		return runId;
	}

	public Optional<BenchResult> getResult() {
		return Optional.ofNullable(result.get());
	}

	public void abort() {
		thread.interrupt();
	}

}
