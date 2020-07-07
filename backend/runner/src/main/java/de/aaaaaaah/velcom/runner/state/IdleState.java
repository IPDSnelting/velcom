package de.aaaaaaah.velcom.runner.state;

import de.aaaaaaah.velcom.runner.entity.RunnerConfiguration;
import de.aaaaaaah.velcom.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.shared.protocol.serverbound.entities.ReadyForWork;
import de.aaaaaaah.velcom.shared.util.FileHelper;
import java.io.IOException;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The runner is idling or receiving work.
 */
public class IdleState implements RunnerState {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdleState.class);

	private final boolean broadcastAvailability;

	private RunnerWorkOrder workOrder;

	public IdleState() {
		this(true);
	}

	public IdleState(boolean broadcastAvailability) {
		this.broadcastAvailability = broadcastAvailability;
	}

	@Override
	public RunnerStatusEnum getStatus() {
		return workOrder != null ? RunnerStatusEnum.PREPARING_WORK : RunnerStatusEnum.IDLE;
	}

	@Override
	public void onSelected(RunnerConfiguration configuration) {
		if (broadcastAvailability) {
			try {
				configuration.getConnectionManager().sendEntity(new ReadyForWork());
			} catch (IOException e) {
				LOGGER.info("Could not send ready for work", e);
				configuration.getConnectionManager().disconnect();
			}
		} else {
			LOGGER.info("Keeping work for myself");
		}
	}

	@Override
	public RunnerState onWorkArrived(RunnerWorkOrder workOrder, RunnerConfiguration configuration) {
		if (getStatus() != RunnerStatusEnum.IDLE) {
			throw new IllegalStateException("Runner already has an order!");
		}
		this.workOrder = workOrder;
		return this;
	}

	@Override
	public RunnerState onFileReceived(Path path, RunnerConfiguration configuration) {
		if (workOrder == null) {
			throw new IllegalStateException("Got a file without any work order first!");
		}
		FileHelper.deleteOnExit(path);
		return new ExecutingState(
			path,
			workOrder,
			configuration.getWorkExecutor().getCancelNonce()
		);
	}
}
