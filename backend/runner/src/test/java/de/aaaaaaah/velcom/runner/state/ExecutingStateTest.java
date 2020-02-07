package de.aaaaaaah.velcom.runner.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import de.aaaaaaah.velcom.runner.entity.RunnerConfiguration;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExecutingStateTest {

	private ExecutingState executingState;

	@BeforeEach
	void setUp() {
		executingState = new ExecutingState();
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
}