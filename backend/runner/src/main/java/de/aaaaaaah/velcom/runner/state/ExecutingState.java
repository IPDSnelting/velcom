package de.aaaaaaah.velcom.runner.state;

import de.aaaaaaah.velcom.runner.entity.RunnerConfiguration;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import java.nio.file.Path;

/**
 * The runner is currently executing some work, so new work will be rejected.
 */
public class ExecutingState implements RunnerState {

	private final Path workPath;
	private final RunnerWorkOrder workOrder;

	public ExecutingState(Path workPath, RunnerWorkOrder workOrder) {
		this.workPath = workPath;
		this.workOrder = workOrder;
	}

	@Override
	public RunnerStatusEnum getStatus() {
		return RunnerStatusEnum.WORKING;
	}

	@Override
	public void onSelected(RunnerConfiguration configuration) {
		configuration.getWorkExecutor().startExecution(workPath, workOrder, configuration);
	}

	@Override
	public RunnerState onWorkArrived(RunnerWorkOrder workOrder, RunnerConfiguration configuration) {
		throw new IllegalStateException(
			"Received new work while executing! Is your scheduler challenged?"
		);
	}
}
