package de.aaaaaaah.velcom.backend.runner.single.state;

import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;

/**
 * The runner is currently idle or getting work.
 */
public class RunnerIdleState implements RunnerState {

	@Override
	public RunnerStatusEnum getStatus() {
		return RunnerStatusEnum.IDLE;
	}

	@Override
	public void onSelected(ActiveRunnerInformation information) {
		information.setIdle();
	}

	@Override
	public RunnerState onMessage(String type, SentEntity entity,
		ActiveRunnerInformation information) {
		switch (type) {
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
