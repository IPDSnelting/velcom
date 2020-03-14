package de.aaaaaaah.velcom.runner.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.runner.entity.RunnerConfiguration;
import de.aaaaaaah.velcom.runner.entity.WorkExecutor;
import de.aaaaaaah.velcom.runner.protocol.SocketConnectionManager;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.WorkReceived;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IdleStateTest {

	private IdleState idleState;
	private RunnerConfiguration configuration;
	private SocketConnectionManager connectionManager;
	private WorkExecutor workExecutor;

	@BeforeEach
	void setUp() {
		idleState = new IdleState();
		configuration = mock(RunnerConfiguration.class);
		connectionManager = mock(SocketConnectionManager.class);
		workExecutor = mock(WorkExecutor.class);

		when(configuration.getConnectionManager()).thenReturn(connectionManager);
		when(configuration.getWorkExecutor()).thenReturn(workExecutor);
	}

	@Test
	void doesNotSwitchOnWorkReceive() {
		assertThat(idleState.onWorkArrived(
			mock(RunnerWorkOrder.class),
			mock(RunnerConfiguration.class)
		)).isEqualTo(idleState);
	}

	@Test
	void statusChangesOnWorkReceive() {
		idleState.onWorkArrived(
			mock(RunnerWorkOrder.class),
			mock(RunnerConfiguration.class)
		);
		assertThat(idleState.getStatus()).isEqualTo(RunnerStatusEnum.PREPARING_WORK);
	}

	@Test
	void switchesOnWorkBinaryReceive() throws InterruptedException, IOException {
		Path path = Path.of("hello");

		RunnerWorkOrder workOrder = new RunnerWorkOrder(UUID.randomUUID(), "Hey");

		idleState.onWorkArrived(workOrder, configuration);

		assertThat(idleState.onFileReceived(path, configuration))
			.isInstanceOf(ExecutingState.class);

		Thread.sleep(200);

		verify(connectionManager).sendEntity(eq(new WorkReceived(workOrder)));
		verify(workExecutor, never()).startExecution(any(), any(), any());
	}

	@Test
	void correctStatus() {
		assertThat(idleState.getStatus()).isEqualTo(RunnerStatusEnum.IDLE);
	}
}