package de.aaaaaaah.velcom.runner.state;

import de.aaaaaaah.velcom.runner.entity.RunnerConfiguration;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.WorkReceived;
import java.io.IOException;
import java.nio.file.Path;

/**
 * The runner is idling or receiving work.
 */
public class IdleState implements RunnerState {

	private RunnerWorkOrder workOrder;

	@Override
	public RunnerStatusEnum getStatus() {
		return workOrder != null ? RunnerStatusEnum.PREPARING_WORK : RunnerStatusEnum.IDLE;
	}

	@Override
	public RunnerState onWorkArrived(RunnerWorkOrder workOrder, RunnerConfiguration configuration) {
		this.workOrder = workOrder;
		return this;
	}

	@Override
	public RunnerState onFileReceived(Path path, RunnerConfiguration configuration) {
		if (workOrder == null) {
			throw new IllegalStateException("Got a file without any work order first!");
		}
		try {
			configuration.getConnectionManager()
				.sendEntity(new WorkReceived(workOrder));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		// Spawn a new thread so this completes and does not overwrite the executing state again
		new Thread(
			() -> configuration.getWorkExecutor().startExecution(path, workOrder, configuration)
		).start();
		return new ExecutingState();
	}
}
