package de.aaaaaaah.velcom.backend.runner.single.state;

import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;

/**
 * Represents a disconnected runner.
 */
public class RunnerDisconnectedState implements RunnerState {

	@Override
	public RunnerStatusEnum getStatus() {
		return RunnerStatusEnum.DISCONNECTED;
	}
}
