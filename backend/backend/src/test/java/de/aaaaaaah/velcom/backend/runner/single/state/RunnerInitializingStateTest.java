package de.aaaaaaah.velcom.backend.runner.single.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.backend.runner.single.RunnerConnectionManager;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RunnerInitializingStateTest {

	private RunnerInitializingState state;
	private ActiveRunnerInformation runnerInformation;
	private RunnerConnectionManager connectionManager;

	@BeforeEach
	void setUp() {
		state = new RunnerInitializingState();
		runnerInformation = mock(ActiveRunnerInformation.class);
		connectionManager = mock(RunnerConnectionManager.class);

		when(runnerInformation.getConnectionManager()).thenReturn(connectionManager);
	}

	@Test
	void statusCorrect() {
		assertThat(state.getStatus()).isEqualTo(RunnerStatusEnum.INITIALIZING);
	}

	@Test
	void resumeToIdle() {
		RunnerInformation information = new RunnerInformation(
			"Test", "OS", 20, 20, RunnerStatusEnum.IDLE, "hash"
		);
		RunnerState newState = state.onMessage(
			RunnerInformation.class.getSimpleName(),
			information,
			runnerInformation
		);

		assertThat(newState).isInstanceOf(RunnerIdleState.class);
		verify(runnerInformation).setRunnerInformation(information);
	}

	@Test
	void resumeToWorking() {
		RunnerInformation information = new RunnerInformation(
			"Test", "OS", 20, 20, RunnerStatusEnum.WORKING, "hash"
		);
		RunnerState newState = state.onMessage(
			RunnerInformation.class.getSimpleName(),
			information,
			runnerInformation
		);

		assertThat(newState).isInstanceOf(RunnerWorkingState.class);
		verify(runnerInformation).setRunnerInformation(information);
	}

	@Test
	void disconnectsOnOtherMessage() {
		RunnerState newState = state.onMessage("hello world", null, runnerInformation);

		assertThat(newState).isEqualTo(state);
		verify(connectionManager).disconnect();
	}
}