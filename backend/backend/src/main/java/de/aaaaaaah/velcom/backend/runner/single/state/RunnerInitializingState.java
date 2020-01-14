package de.aaaaaaah.velcom.backend.runner.single.state;

import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runner is connected but not ready to accept jobs
 */
public class RunnerInitializingState implements RunnerState {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunnerInitializingState.class);

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

		LOGGER.info(
			"Runner sent invalid message of type {} with data {}, kicking {}",
			type, entity, information.getRunnerInformation()
		);
		information.getConnectionManager().disconnect();
		return this;
	}
}
