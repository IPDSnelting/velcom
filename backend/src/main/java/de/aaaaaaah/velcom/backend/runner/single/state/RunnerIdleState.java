package de.aaaaaaah.velcom.backend.runner.single.state;

import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;

/**
 * The runner is currently idle or getting work.
 */
public class RunnerIdleState implements RunnerState {

	@Override
	public void onSelected(ActiveRunnerInformation information) {
		information.setState(RunnerStatusEnum.IDLE);
	}

	@Override
	public RunnerState onMessage(String type, SentEntity entity,
		ActiveRunnerInformation information) {
		switch (type) {
			case "RunnerInformation":
				RunnerInformation runnerInformation = (RunnerInformation) entity;
				information.setRunnerInformation(runnerInformation);
				information.setState(runnerInformation.getRunnerState());
				if (runnerInformation.getRunnerState() == RunnerStatusEnum.WORKING) {
					return new RunnerWorkingState();
				}
				return this;
			case "WorkReceived":
				return new RunnerWorkingState();
			case "BenchmarkResults":
				information.getRunnerStateMachine()
					.onWorkDone((BenchmarkResults) entity);
				return this;
			default:
				System.err.println("Unknown message received: " + type + " " + entity);
				return this;
		}

	}
}
