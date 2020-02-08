package de.aaaaaaah.velcom.backend.runner;

import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import java.util.Objects;
import java.util.Optional;

/**
 * A runner the dispatcher knows something about.
 */
public class KnownRunner {

	private final RunnerStatusEnum currentStatus;
	private final String name;
	private final String operatingSystem;
	private final int coreCount;
	private final long availableMemory;
	private final Commit currentCommit;

	/**
	 * Creates a new known runner.
	 *
	 * @param currentStatus the current runner status
	 * @param name the name of the runner
	 * @param operatingSystem the OS of the runner
	 * @param coreCount the core count available to the JVM on the runner
	 * @param availableMemory the total available memory measured in bytes
	 * @param currentCommit the commit the runner is currently executing
	 */
	KnownRunner(RunnerStatusEnum currentStatus, String name, String operatingSystem,
		int coreCount, long availableMemory, Commit currentCommit) {
		this.currentStatus = currentStatus;
		this.name = name;
		this.operatingSystem = operatingSystem;
		this.coreCount = coreCount;
		this.availableMemory = availableMemory;
		this.currentCommit = currentCommit;
	}

	/**
	 * Returns the current runner status.
	 *
	 * @return the current runner status
	 */
	public RunnerStatusEnum getCurrentStatus() {
		return currentStatus;
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
	 * Returns the OS of the runner.
	 *
	 * @return the OS of the runner
	 */
	public String getOperatingSystem() {
		return operatingSystem;
	}

	/**
	 * Returns the core count available to the JVM on the runner.
	 *
	 * @return the core count available to the JVM on the runner
	 */
	public int getCoreCount() {
		return coreCount;
	}

	/**
	 * Returns the available memory.
	 *
	 * @return the total available memory measured in bytes
	 */
	public long getAvailableMemory() {
		return availableMemory;
	}

	/**
	 * Returns an aggregated, human readable, string containing the runner's machine information
	 * (Operating system, CPU cores, etc.).
	 *
	 * @return a description of the machine the runner runs on
	 */
	public String getMachineInfo() {
		String memory = getAvailableMemory() / 1_000_000 + "MB";
		return getOperatingSystem() + "\n" + getCoreCount() + " cores\n" + memory + " max RAM";
	}

	/**
	 * Returns the commit the runner is currently executing.
	 *
	 * @return the commit the runner is currently executing
	 */
	public Optional<Commit> getCurrentCommit() {
		return Optional.ofNullable(currentCommit);
	}

	/**
	 * Converts some {@link RunnerInformation} to a {@link KnownRunner}.
	 *
	 * @param runnerInformation the runner information
	 * @param commit the commit the runner is currently working on. Might be null.
	 * @return a corresponding known runner
	 */
	static KnownRunner fromRunnerInformation(RunnerInformation runnerInformation, Commit commit) {
		return new KnownRunner(
			runnerInformation.getRunnerState(),
			runnerInformation.getName(),
			runnerInformation.getOperatingSystem(),
			runnerInformation.getCoreCount(),
			runnerInformation.getAvailableMemory(),
			commit
		);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		KnownRunner that = (KnownRunner) o;
		return coreCount == that.coreCount &&
			availableMemory == that.availableMemory &&
			currentStatus == that.currentStatus &&
			Objects.equals(name, that.name) &&
			Objects.equals(operatingSystem, that.operatingSystem) &&
			Objects.equals(currentCommit, that.currentCommit);
	}

	@Override
	public int hashCode() {
		return Objects.hash(currentStatus, name, operatingSystem, coreCount, availableMemory,
			currentCommit);
	}

	@Override
	public String toString() {
		return "KnownRunner{" +
			"currentStatus=" + currentStatus +
			", name='" + name + '\'' +
			", operatingSystem='" + operatingSystem + '\'' +
			", coreCount=" + coreCount +
			", availableMemory=" + availableMemory +
			", machineInfo=" + getMachineInfo() +
			", currentCommit=" + currentCommit +
			'}';
	}
}
