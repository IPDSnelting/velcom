package de.aaaaaaah.velcom.backend.runner.single.state;

import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;

/**
 * The runner is currently working!
 */
public class RunnerWorkingState implements RunnerState {

	@Override
	public RunnerStatusEnum getStatus() {
		return RunnerStatusEnum.WORKING;
	}

	@Override
	public RunnerState onMessage(String type, SentEntity entity,
		ActiveRunnerInformation information) {
		if ("BenchmarkResults".equals(type)) {
			information.getRunnerStateMachine().onWorkDone((BenchmarkResults) entity);
			return new RunnerIdleState();
		}
		System.err.println("unknown type received: " + type + " " + entity);
		return this;
	}
}
