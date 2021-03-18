package de.aaaaaaah.velcom.backend.runner.single;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.builder.NewRun;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunError;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunErrorType;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.taskaccess.entities.Task;
import de.aaaaaaah.velcom.backend.access.taskaccess.entities.TaskId;
import de.aaaaaaah.velcom.backend.access.taskaccess.entities.TaskPriority;
import de.aaaaaaah.velcom.backend.data.benchrepo.BenchRepo;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.runner.Dispatcher;
import de.aaaaaaah.velcom.backend.runner.KnownRunner;
import de.aaaaaaah.velcom.backend.runner.single.state.AwaitSendWorkEnd;
import de.aaaaaaah.velcom.backend.runner.single.state.TeleRunnerState;
import de.aaaaaaah.velcom.shared.protocol.StatusCode;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Benchmark;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Metric;
import de.aaaaaaah.velcom.shared.protocol.serialization.Serializer;
import de.aaaaaaah.velcom.shared.protocol.serialization.Status;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.ClientBoundPacketType;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.GetResultReply;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.GetStatusReply;
import de.aaaaaaah.velcom.shared.protocol.statemachine.StateMachine;
import de.aaaaaaah.velcom.shared.util.Either;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TeleRunnerTest {

	private TeleRunner runner;
	private Dispatcher dispatcher;
	private Queue queue;
	private BenchRepo benchRepo;

	@BeforeEach
	void setUp() {
		dispatcher = mock(Dispatcher.class);
		queue = mock(Queue.class);
		benchRepo = mock(BenchRepo.class);

		when(dispatcher.getQueue()).thenReturn(queue);
		this.runner = new TeleRunner(
			"Runner",
			new Serializer(),
			dispatcher,
			benchRepo
		);
	}

	@Test
	void registersWithDispatcherWhenReceivingInfo() {
		verify(dispatcher, never()).addRunner(runner);

		runner.setRunnerInformation(mock(GetStatusReply.class));

		verify(dispatcher).addRunner(runner);
	}

	@Test
	void doesNotRegisterMultipleTimes() {
		verify(dispatcher, never()).addRunner(runner);

		runner.setRunnerInformation(mock(GetStatusReply.class));
		runner.setRunnerInformation(mock(GetStatusReply.class));

		verify(dispatcher, atMostOnce()).addRunner(runner);
	}

	@Test
	void updatesPingWhenCreatingConnection() {
		assertThat(runner.createConnection()).isNotNull();
		assertThat(ChronoUnit.MILLIS.between(runner.getLastPing(), Instant.now())).isLessThan(4000);
	}

	@Test
	void doesNotCreateMultipleConnections() {
		assertThat(runner.createConnection()).isNotNull();
		assertThatThrownBy(() -> runner.createConnection()).isNotNull();

		assertThat(runner.hasConnection()).isTrue();
	}

	@Test
	void resetsConnectionWhenClosed() {
		RunnerConnection connection = runner.createConnection();
		assertThat(connection).isNotNull();

		connection.close(StatusCode.INTERNAL_ERROR);
		connection.onWebSocketClose(200, "");

		assertThat(runner.hasConnection()).isFalse();
	}

	@Test
	void disposeReleasesResources() {
		assertThat(runner.createConnection()).isNotNull();
		assertThat(runner.hasConnection()).isTrue();

		runner.dispose();

		assertThat(runner.hasConnection()).isTrue();
		assertThat(runner.isDisposed()).isTrue();
		assertThatThrownBy(() -> runner.createConnection()).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void updatesRunnerInformation() {
		GetStatusReply first = new GetStatusReply(
			"hey", "there", "my", false, Status.IDLE, null, null
		);
		runner.setRunnerInformation(first);

		assertThat(runner.getRunnerInformation()).isEqualTo(new KnownRunner(
			runner.getRunnerName(), "hey", "there", Status.IDLE, null, true, null, null, List.of()));

		GetStatusReply second = new GetStatusReply(
			"hey2", "there2", "my2", false, Status.IDLE, null, null
		);
		runner.setRunnerInformation(second);

		assertThat(runner.getRunnerInformation()).isEqualTo(new KnownRunner(
			runner.getRunnerName(), "hey2", "there2", Status.IDLE, null, true, null, null, List.of()));
	}

	// FIXME: This is questionable below here

	@Test
	void discardsResultIfItHadNoTask() {
		GetResultReply resultReply = new GetResultReply(
			UUID.randomUUID(), true, mock(Result.class), null, Instant.now(), Instant.now()
		);
		runner.handleResults(resultReply);

		verify(queue).abortTask(new TaskId(resultReply.getRunId()));
	}

	@Test
	void passesSuccessResultOn() throws NoSuchFieldException, IllegalAccessException {
		Result result = new Result(
			List.of(new Benchmark(
				"test", List.of(new Metric("hello", "argh", null, null, null))
			)), null
		);
		UUID runId = UUID.randomUUID();
		NewRun run = handleResult(runId, result);

		assertThat(run.getId()).isEqualTo(new RunId(runId));
		assertThat(run.getResult().getRight()).isPresent();
	}

	@Test
	void passesFailedResultOn() throws NoSuchFieldException, IllegalAccessException {
		Result result = new Result(
			null, "I am a big error"
		);
		UUID runId = UUID.randomUUID();
		NewRun run = handleResult(runId, result);

		assertThat(run.getId()).isEqualTo(new RunId(runId));
		assertThat(run.getResult().getLeft())
			.get()
			.extracting(RunError::getType)
			.isEqualTo(RunErrorType.BENCH_SCRIPT_ERROR);
		assertThat(run.getResult().getLeft())
			.get()
			.extracting(RunError::getMessage)
			.isEqualTo(result.getError().orElseThrow());
	}

	// FIXME: This is really questionable below here

	@Test
	void sendsWork() throws NoSuchFieldException, IllegalAccessException {
		Task task = buildTask(UUID.randomUUID());
		setTask(task);

		RunnerConnection connection = mock(RunnerConnection.class);
		@SuppressWarnings("unchecked")
		StateMachine<TeleRunnerState> stateMachine = mock(StateMachine.class);
		when(connection.getStateMachine()).thenReturn(stateMachine);

		setConnection(connection);
		runner.setRunnerInformation(new GetStatusReply(
			"Hello", "my version", "currentHash", false, Status.IDLE, null, null
		));
		when(benchRepo.getCurrentHash()).thenReturn(Optional.of(new CommitHash("otherHash")));

		runner.sendAvailableWork(mock(AwaitSendWorkEnd.class));
		verify(connection, times(2)).createBinaryOutputStream();
		verify(connection)
			.send(argThat(argument -> argument.getType() == ClientBoundPacketType.REQUEST_RUN_REPLY));
	}

	private NewRun handleResult(UUID runId, Result result)
		throws NoSuchFieldException, IllegalAccessException {
		GetResultReply resultReply = new GetResultReply(
			runId, true, result, null, Instant.now(), Instant.now()
		);
		Task task = buildTask(runId);
		setTask(task);

		runner.setRunnerInformation(new GetStatusReply(
			"hey", "there", null, false, Status.IDLE, null, null
		));
		runner.handleResults(resultReply);

		ArgumentCaptor<NewRun> captor = ArgumentCaptor.forClass(NewRun.class);
		verify(dispatcher).completeTask(captor.capture());

		return captor.getValue();
	}

	private Task buildTask(UUID runId) {
		return new Task(
			new TaskId(runId), "Peter", TaskPriority.LISTENER,
			Instant.now(), Instant.now(),
			Either.ofLeft(new CommitSource(
				new RepoId(UUID.randomUUID()), new CommitHash("hello")
			)),
			false
		);
	}

	private void setTask(Task task) throws NoSuchFieldException, IllegalAccessException {
		Field myCurrentTask = runner.getClass().getDeclaredField("myCurrentTask");
		myCurrentTask.setAccessible(true);
		//noinspection unchecked
		AtomicReference<Task> reference = (AtomicReference<Task>) myCurrentTask.get(runner);
		reference.set(task);
	}

	private void setConnection(RunnerConnection connection)
		throws NoSuchFieldException, IllegalAccessException {
		Field myCurrentTask = runner.getClass().getDeclaredField("connection");
		myCurrentTask.setAccessible(true);
		myCurrentTask.set(runner, connection);
	}
}
