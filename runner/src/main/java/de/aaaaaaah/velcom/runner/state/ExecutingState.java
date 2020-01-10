package de.aaaaaaah.velcom.runner.state;

import de.aaaaaaah.velcom.runner.entity.RunnerConfiguration;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;

/**
 * The runner is currently executing some work, so new work will be rejected.
 */
public class ExecutingState implements RunnerState {

	@Override
	public RunnerState onSelected(RunnerConfiguration configuration) {
		configuration.getRunnerStateMachine().setCurrentRunnerStateEnum(RunnerStatusEnum.WORKING);
		return this;
	}

	@Override
	public RunnerState onWorkArrived(RunnerWorkOrder workOrder, RunnerConfiguration configuration) {
		throw new IllegalStateException(
			"Received new work while executing! Is your scheduler challenged?"
		);
	}
}
