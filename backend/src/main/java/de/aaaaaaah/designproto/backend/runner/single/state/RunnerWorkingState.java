package de.aaaaaaah.designproto.backend.runner.single.state;

import de.aaaaaaah.designproto.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.designproto.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.designproto.runner.shared.protocol.SentEntity;
import de.aaaaaaah.designproto.runner.shared.protocol.serverbound.entities.BenchmarkResults;

/**
 * The runner is currently working!
 */
public class RunnerWorkingState implements RunnerState {

	@Override
	public void onSelected(ActiveRunnerInformation information) {
		information.setState(RunnerStatusEnum.WORKING);
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
