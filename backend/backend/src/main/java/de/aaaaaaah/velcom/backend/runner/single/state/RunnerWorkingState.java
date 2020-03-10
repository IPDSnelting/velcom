package de.aaaaaaah.velcom.backend.runner.single.state;

import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.runner.shared.protocol.StatusCodeMappings;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The runner is currently working!
 */
public class RunnerWorkingState implements RunnerState {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunnerWorkingState.class);

	@Override
	public RunnerStatusEnum getStatus() {
		return RunnerStatusEnum.WORKING;
	}

	@Override
	public void onSelected(ActiveRunnerInformation information) {
		if (information.getCurrentCommit().isEmpty()) {
			try {
				information.getRunnerStateMachine().resetRunner("Benchmark aborted manually");
			} catch (IOException e) {
				LOGGER.error("Error resetting runner", e);
				information.getConnectionManager().disconnect(
					StatusCodeMappings.SERVER_INITIATED_DISCONNECT,
					"Reset failed"
				);
			}
		}
	}

	@Override
	public RunnerState onMessage(String type, SentEntity entity,
		ActiveRunnerInformation information) {
		if (BenchmarkResults.class.getSimpleName().equals(type)) {
			information.getRunnerStateMachine().onWorkDone((BenchmarkResults) entity);
			return new RunnerIdleState();
		} else if (RunnerInformation.class.getSimpleName().equals(type)) {
			information.setRunnerInformation((RunnerInformation) entity);
			return this;
		}
		LOGGER.info(
			"Runner sent invalid message of type {} with data {}, kicking runner {}",
			type, entity, information.getRunnerInformation()
		);
		information.getConnectionManager().disconnect(
			StatusCodeMappings.SERVER_INITIATED_DISCONNECT,
			"Invalid message: " + type
		);
		return this;
	}
}
