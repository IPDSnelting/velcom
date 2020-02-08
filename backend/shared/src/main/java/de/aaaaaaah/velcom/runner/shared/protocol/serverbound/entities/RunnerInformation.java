package de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import java.util.Objects;
import java.util.Optional;

/**
 * Contains information about the runner.
 */
public class RunnerInformation implements SentEntity {

	private final String name;
	private final String operatingSystem;
	private final int coreCount;
	private final long availableMemory;
	private final RunnerStatusEnum runnerState;
	private final String currentBenchmarkRepoHash;

	/**
	 * Creates a new {@link RunnerInformation} packet.
	 *
	 * @param name a unique name for this runner
	 * @param operatingSystem the operating system of the runner
	 * @param coreCount the available (online and JVM accessible) core count
	 * @param availableMemory the available memory
	 * @param runnerState the current state of the runner
	 * @param currentBenchmarkRepoHash the current hash of the benchmark repository. May be null.
	 */
	@JsonCreator
	public RunnerInformation(String name, String operatingSystem, int coreCount,
		long availableMemory, RunnerStatusEnum runnerState, String currentBenchmarkRepoHash) {
		this.name = name;
		this.operatingSystem = operatingSystem;
		this.coreCount = coreCount;
		this.availableMemory = availableMemory;
		this.runnerState = runnerState;
		this.currentBenchmarkRepoHash = currentBenchmarkRepoHash;
	}

	/**
	 * Returns the name of the runner.
	 *
	 * @return the name of the runner
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the operating system of the runner.
	 *
	 * @return the operating system of the runner
	 */
	public String getOperatingSystem() {
		return operatingSystem;
	}

	/**
	 * Returns the available (online and JVM accessible) core count.
	 *
	 * @return the available (online and JVM accessible) core count
	 */
	public int getCoreCount() {
		return coreCount;
	}

	/**
	 * Returns the available memory.
	 *
	 * @return the available memory
	 */
	public long getAvailableMemory() {
		return availableMemory;
	}

	/**
	 * Return state current runner state.
	 *
	 * @return the runner state
	 */
	public RunnerStatusEnum getRunnerState() {
		return runnerState;
	}

	/**
	 * Returns the hash of the current HEAD of the local benchmark repo.
	 *
	 * @return the hash of the current HEAD of the local benchmark repo
	 */
	public Optional<String> getCurrentBenchmarkRepoHash() {
		return Optional.ofNullable(currentBenchmarkRepoHash);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RunnerInformation that = (RunnerInformation) o;
		return coreCount == that.coreCount &&
			availableMemory == that.availableMemory &&
			Objects.equals(name, that.name) &&
			Objects.equals(operatingSystem, that.operatingSystem) &&
			runnerState == that.runnerState &&
			Objects.equals(currentBenchmarkRepoHash, that.currentBenchmarkRepoHash);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, operatingSystem, coreCount, availableMemory, runnerState,
			currentBenchmarkRepoHash);
	}

	@Override
	public String toString() {
		return "RunnerInformation{" +
			"name='" + name + '\'' +
			", operatingSystem='" + operatingSystem + '\'' +
			", coreCount=" + coreCount +
			", availableMemory=" + availableMemory +
			", runnerState=" + runnerState +
			", currentBenchmarkRepoHash='" + currentBenchmarkRepoHash + '\'' +
			'}';
	}
}
