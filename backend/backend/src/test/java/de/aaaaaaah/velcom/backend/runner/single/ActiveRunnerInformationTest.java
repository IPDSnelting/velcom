package de.aaaaaaah.velcom.backend.runner.single;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import de.aaaaaaah.velcom.backend.newaccess.entities.Commit;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActiveRunnerInformationTest {

	private ActiveRunnerInformation runnerInformation;
	private ServerRunnerStateMachine stateMachine;
	private RunnerConnectionManager connectionManager;

	@BeforeEach
	void setUp() {
		stateMachine = mock(ServerRunnerStateMachine.class);
		connectionManager = mock(RunnerConnectionManager.class);
		runnerInformation = new ActiveRunnerInformation(
			connectionManager,
			stateMachine
		);
	}

	@Test
	void callsIdleListener() {
		Runnable idleListener = mock(Runnable.class);
		runnerInformation.setOnIdle(idleListener);

		runnerInformation.setIdle();

		verify(idleListener).run();
	}

	@Test
	void callsDisconnectedListener() {
		IntConsumer disconnectedListener = mock(IntConsumer.class);
		runnerInformation.setOnDisconnected(disconnectedListener);

		runnerInformation.setDisconnected(20);

		verify(disconnectedListener).accept(20);
	}

	@Test
	void callsRunnerInformationListener() {
		@SuppressWarnings("unchecked")
		Consumer<RunnerInformation> listener = mock(Consumer.class);
		runnerInformation.setOnRunnerInformation(listener);

		RunnerInformation information = mock(RunnerInformation.class);
		runnerInformation.setRunnerInformation(information);

		verify(listener).accept(information);
	}

	@Test
	void callsResultListener() {
		@SuppressWarnings("unchecked")
		Consumer<BenchmarkResults> listener = mock(Consumer.class);
		runnerInformation.setResultListener(listener);

		BenchmarkResults results = new BenchmarkResults(
			new RunnerWorkOrder(UUID.randomUUID(), "hash"),
			"error", Instant.now(), Instant.MAX
		);
		runnerInformation.setResults(results);

		verify(listener).accept(results);
	}

	@Test
	void resultsClearsCommit() {
		Commit commit = mock(Commit.class);
		runnerInformation.setCurrentCommit(commit);

		assertThat(runnerInformation.getCurrentCommit()).hasValue(commit);

		BenchmarkResults results = new BenchmarkResults(
			new RunnerWorkOrder(UUID.randomUUID(), "hash"),
			"error", Instant.now(), Instant.MAX
		);
		runnerInformation.setResults(results);

		assertThat(runnerInformation.getCurrentCommit()).isEmpty();
	}
}