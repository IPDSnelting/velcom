package de.aaaaaaah.velcom.runner.benchmarking;

import de.aaaaaaah.velcom.shared.util.systeminfo.LinuxSystemInfo;
import java.nio.file.Path;
import java.util.UUID;

public class BenchRequest {

	private final LinuxSystemInfo systemInfo;
	private final UUID runId;
	private final String benchRepoHash;
	private final Path benchRepoPath;
	private final Path workRepoPath;
	private final String runnerName;

	public BenchRequest(LinuxSystemInfo systemInfo, UUID runId, String benchRepoHash,
		Path benchRepoPath, Path workRepoPath, String runnerName) {
		this.systemInfo = systemInfo;
		this.runId = runId;
		this.benchRepoHash = benchRepoHash;
		this.benchRepoPath = benchRepoPath;
		this.workRepoPath = workRepoPath;
		this.runnerName = runnerName;
	}

	public LinuxSystemInfo getSystemInfo() {
		return systemInfo;
	}

	public UUID getRunId() {
		return runId;
	}

	public String getBenchRepoHash() {
		return benchRepoHash;
	}

	public Path getBenchRepoPath() {
		return benchRepoPath;
	}

	public Path getWorkRepoPath() {
		return workRepoPath;
	}

	public String getRunnerName() {
		return runnerName;
	}
}
