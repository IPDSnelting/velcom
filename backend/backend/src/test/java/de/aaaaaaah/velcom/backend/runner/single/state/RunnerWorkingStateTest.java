package de.aaaaaaah.velcom.backend.runner.single.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.backend.runner.single.RunnerConnectionManager;
import de.aaaaaaah.velcom.backend.runner.single.ServerRunnerStateMachine;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RunnerWorkingStateTest {

	private RunnerWorkingState workingState;
	private ActiveRunnerInformation runnerInformation;
	private RunnerConnectionManager connectionManager;
	private ServerRunnerStateMachine stateMachine;

	@BeforeEach
	void setUp() {
		workingState = new RunnerWorkingState();
		runnerInformation = mock(ActiveRunnerInformation.class);
		connectionManager = mock(RunnerConnectionManager.class);
		stateMachine = mock(ServerRunnerStateMachine.class);

		when(runnerInformation.getConnectionManager()).thenReturn(connectionManager);
		when(runnerInformation.getRunnerStateMachine()).thenReturn(stateMachine);
	}

	@Test
	void statusCorrect() {
		assertThat(workingState.getStatus()).isEqualTo(RunnerStatusEnum.WORKING);
	}

	@Test
	void forwardsResults() {
		BenchmarkResults results = new BenchmarkResults(
			new RunnerWorkOrder(UUID.randomUUID(), "hash"),
			"Hello", Instant.now(), Instant.now()
		);
		RunnerState state = workingState.onMessage(
			BenchmarkResults.class.getSimpleName(),
			results,
			runnerInformation
		);

		assertThat(state).isInstanceOf(RunnerIdleState.class);
		verify(stateMachine).onWorkDone(eq(results));
	}

	@Test
	void disconnectsOnOtherMessage() {
		RunnerState newState = workingState.onMessage("hello world", null, runnerInformation);

		assertThat(newState).isEqualTo(workingState);
		verify(connectionManager).disconnect();
	}

}