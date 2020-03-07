package de.aaaaaaah.velcom.backend.runner.single.state;

import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The runner is currently idle or getting work.
 */
public class RunnerIdleState implements RunnerState {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunnerIdleState.class);

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
		if (BenchmarkResults.class.getSimpleName().equals(type)) {
			information.getRunnerStateMachine()
				.onWorkDone((BenchmarkResults) entity);
			return this;
		} else if (RunnerInformation.class.getSimpleName().equals(type)) {
			information.setRunnerInformation((RunnerInformation) entity);
			return this;
		}
		LOGGER.info(
			"Runner sent invalid message of type {} with data {}, kicking {}",
			type, entity, information.getRunnerInformation()
		);
		information.getConnectionManager().disconnect();
		return this;
	}
}
