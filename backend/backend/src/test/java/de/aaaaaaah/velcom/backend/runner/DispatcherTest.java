package de.aaaaaaah.velcom.backend.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.builder.NewRun;
import de.aaaaaaah.velcom.backend.access.taskaccess.entities.Task;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.runner.single.TeleRunner;
import de.aaaaaaah.velcom.shared.protocol.serialization.Status;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DispatcherTest {

	private Dispatcher dispatcher;

	private Queue queue;
	private Duration runnerGracePeriod;
	private KnownRunner knownRunner;

	@BeforeEach
	void setUp() {
		queue = mock(Queue.class);
		when(queue.startNextTask()).thenReturn(Optional.of(mock(Task.class)));

		runnerGracePeriod = Duration.ofSeconds(1);
		dispatcher = new Dispatcher(queue, runnerGracePeriod);
		knownRunner = new KnownRunner(
			"runner",
			"info",
			"hash",
			Status.IDLE,
			null,
			false,
			null,
			null
		);
	}

	@Test
	void addedRunnerVisibleInAllRunners() {
		TeleRunner runner = getRunner();
		dispatcher.addRunner(runner);

		assertThat(dispatcher.getKnownRunners()).containsExactly(knownRunner);
		assertThat(dispatcher.getTeleRunner(runner.getRunnerName())).get().isSameAs(runner);
	}

	@Test
	void cantAddRunnerWithSameName() {
		dispatcher.addRunner(getRunner());
		assertThatThrownBy(() -> dispatcher.addRunner(getRunner()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("name");
	}

	@Test
	void doesNotReturnWorkForUnregisteredRunner() {
		assertThat(queue.startNextTask()).isPresent();
		assertThat(dispatcher.getWork(getRunner())).isEmpty();
	}

	@Test
	void returnsWorkForRegisteredRunner() {
		TeleRunner runner = getRunner();
		dispatcher.addRunner(runner);

		assertThat(queue.startNextTask()).isPresent();
		assertThat(dispatcher.getWork(runner)).isPresent();
	}

	@Test
	void cleansUpDisconnectedRunner() throws InterruptedException {
		TeleRunner runner = getRunner();
		when(runner.hasConnection()).thenReturn(false);
		when(runner.getLastPing()).then(
			invocation -> Instant.now().minus(runnerGracePeriod).minusMillis(1)
		);

		dispatcher.addRunner(runner);
		assertThat(dispatcher.getTeleRunner(runner.getRunnerName())).get().isSameAs(runner);

		Thread.sleep(2000);
		assertThat(dispatcher.getTeleRunner(runner.getRunnerName())).isEmpty();
	}

	@Test
	void doesNotCleanUpUnresponsiveConnectedRunner() throws InterruptedException {
		TeleRunner runner = getRunner();
		when(runner.hasConnection()).thenReturn(true);
		// Out of the grace period but it still has a connection. We keep it, as the websocket listener
		// probably just reset the connection. We have a dedicated ping timeout.
		when(runner.getLastPing()).then(
			invocation -> Instant.now().minus(runnerGracePeriod).minusMillis(1)
		);

		dispatcher.addRunner(runner);
		assertThat(dispatcher.getTeleRunner(runner.getRunnerName())).get().isSameAs(runner);

		Thread.sleep(2000);
		assertThat(dispatcher.getTeleRunner(runner.getRunnerName())).get().isSameAs(runner);
	}

	@Test
	void doesNotCleanUpConnectedRunner() throws InterruptedException {
		TeleRunner runner = getRunner();
		when(runner.hasConnection()).thenReturn(true);
		when(runner.getLastPing()).then(
			// Juuust inside
			invocation -> Instant.now().minus(runnerGracePeriod)
		);

		dispatcher.addRunner(runner);
		assertThat(dispatcher.getTeleRunner(runner.getRunnerName())).get().isSameAs(runner);

		Thread.sleep(2000);
		assertThat(dispatcher.getTeleRunner(runner.getRunnerName())).get().isSameAs(runner);
	}

	@Test
	void completeWorkWritesThroughToQueue() {
		NewRun newRun = mock(NewRun.class);
		dispatcher.completeTask(newRun);

		verify(queue).completeTask(newRun);
	}

	private TeleRunner getRunner() {
		TeleRunner runner = mock(TeleRunner.class);
		when(runner.getRunnerInformation()).thenReturn(knownRunner);
		when(runner.hasConnection()).thenReturn(true);
		when(runner.getRunnerName()).thenReturn(knownRunner.getName());
		when(runner.isDisposed()).thenReturn(false);
		return runner;
	}
}
