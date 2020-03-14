package de.aaaaaaah.velcom.runner.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.runner.entity.RunnerConfiguration;
import de.aaaaaaah.velcom.runner.entity.WorkExecutor;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import java.nio.file.Path;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExecutingStateTest {

	private ExecutingState executingState;
	private RunnerWorkOrder workOrder;
	private Path workPath;

	@BeforeEach
	void setUp() {
		workPath = Path.of("/hello");
		workOrder = new RunnerWorkOrder(UUID.randomUUID(), "hash");
		executingState = new ExecutingState(workPath, workOrder);
	}

	@Test
	void receivingWorkResultsInError() {
		Assertions.assertThatThrownBy(() -> executingState.onWorkArrived(
			mock(RunnerWorkOrder.class),
			mock(RunnerConfiguration.class)
		));
	}

	@Test
	void hasCorrectStatus() {
		assertThat(executingState.getStatus()).isEqualTo(RunnerStatusEnum.WORKING);
	}

	@Test
	void startsExecutionInSelected() throws InterruptedException {
		RunnerConfiguration configuration = mock(RunnerConfiguration.class);
		WorkExecutor executor = mock(WorkExecutor.class);
		when(configuration.getWorkExecutor()).thenReturn(executor);

		executingState.onSelected(configuration);

		Thread.sleep(100);

		verify(executor).startExecution(workPath, workOrder, configuration);
	}
}