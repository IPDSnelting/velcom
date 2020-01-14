package de.aaaaaaah.velcom.backend.runner.single.state;

import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;

/**
 * Runner is connected but not ready to accept jobs
 */
public class RunnerInitializingState implements RunnerState {

	@Override
	public RunnerStatusEnum getStatus() {
		return RunnerStatusEnum.INITIALIZING;
	}

	@Override
	public RunnerState onMessage(String type, SentEntity entity,
		ActiveRunnerInformation information) {
		if ("RunnerInformation".equals(type)) {
			RunnerInformation runnerInformation = (RunnerInformation) entity;
			information.setRunnerInformation(runnerInformation);
			if (runnerInformation.getRunnerState() == RunnerStatusEnum.WORKING) {
				return new RunnerWorkingState();
			}
			return new RunnerIdleState();
		}
		System.out.println("Unknwon message received: " + type);
		return this;
	}
}
