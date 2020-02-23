package de.aaaaaaah.velcom.backend.runner.single.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.backend.newaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.backend.runner.single.RunnerConnectionManager;
import de.aaaaaaah.velcom.backend.runner.single.ServerRunnerStateMachine;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.WorkReceived;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RunnerIdleStateTest {

	private RunnerIdleState idleState;
	private ServerRunnerStateMachine stateMachine;
	private ActiveRunnerInformation runnerInformation;

	@BeforeEach
	void setUp() {
		idleState = new RunnerIdleState();
		stateMachine = mock(ServerRunnerStateMachine.class);
		runnerInformation = mock(ActiveRunnerInformation.class);

		when(runnerInformation.getRunnerStateMachine()).thenReturn(stateMachine);
	}

	@Test
	void switchesToExecutingWhenWorkReceived() {
		WorkReceived workReceived = new WorkReceived(new RunnerWorkOrder(UUID.randomUUID(), "hey"));
		when(runnerInformation.getCurrentCommit()).thenReturn(Optional.of(mock(Commit.class)));

		RunnerState newState = idleState.onMessage(
			WorkReceived.class.getSimpleName(),
			workReceived,
			runnerInformation
		);

		assertThat(newState).isInstanceOf(RunnerWorkingState.class);
	}

	@Test
	void setsResults() {
		RunnerWorkOrder workOrder = new RunnerWorkOrder(UUID.randomUUID(), "hash");
		BenchmarkResults results = new BenchmarkResults(
			workOrder, "Test", Instant.now(), Instant.now()
		);

		RunnerState newState = idleState.onMessage(
			BenchmarkResults.class.getSimpleName(),
			results,
			runnerInformation
		);

		verify(stateMachine).onWorkDone(eq(results));
		assertThat(newState).isEqualTo(idleState);
	}

	@Test
	void disconnectsOnOtherMessageType() {
		RunnerConnectionManager connectionManager = mock(RunnerConnectionManager.class);
		when(runnerInformation.getConnectionManager()).thenReturn(connectionManager);

		RunnerState newState = idleState.onMessage("Hello world!", null, runnerInformation);

		assertThat(newState).isEqualTo(idleState);
		verify(connectionManager).disconnect();
	}

	@Test
	void statusIsCorrect() {
		assertThat(idleState.getStatus()).isEqualTo(RunnerStatusEnum.IDLE);
	}
}