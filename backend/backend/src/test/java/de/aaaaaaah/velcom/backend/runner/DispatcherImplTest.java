package de.aaaaaaah.velcom.backend.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.backend.access.benchmark.BenchmarkAccess;
import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.RepoAccess;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.backend.runner.single.RunnerConnectionManager;
import de.aaaaaaah.velcom.backend.runner.single.ServerRunnerStateMachine;
import de.aaaaaaah.velcom.backend.runner.single.state.RunnerIdleState;
import de.aaaaaaah.velcom.backend.runner.single.state.RunnerState;
import de.aaaaaaah.velcom.backend.runner.single.state.RunnerWorkingState;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.StatusCodeMappings;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults.Benchmark;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults.Metric;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults.MetricInterpretation;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DispatcherImplTest {

	private static final CommitHash COMMIT_HASH = new CommitHash("hash");
	private static final RepoId REPO_ID = new RepoId(UUID.randomUUID());
	private static final int GRACE_PERIOD_SECONDS = 1;
	private static final CommitHash BENCH_REPO_HASH = new CommitHash("other");

	private DispatcherImpl dispatcher;
	private Queue queue;
	private BenchmarkAccess benchmarkAccess;

	@BeforeEach
	void setUp() {
		queue = mock(Queue.class);
		RepoAccess repoAccess = mock(RepoAccess.class);
		benchmarkAccess = mock(BenchmarkAccess.class);

		when(repoAccess.getLatestBenchmarkRepoHash()).thenReturn(BENCH_REPO_HASH);

		dispatcher = new DispatcherImpl(
			queue,
			repoAccess,
			benchmarkAccess,
			Duration.ofSeconds(GRACE_PERIOD_SECONDS)
		);
	}

	@Test
	void knownRunnersIsInitiallyEmpty() {
		assertThat(dispatcher.getKnownRunners()).isEmpty();
	}

	@Test
	void addRunner() {
		ActiveRunnerInformation information = spy(new ActiveRunnerInformation(
			mock(RunnerConnectionManager.class), mock(ServerRunnerStateMachine.class)
		));
		dispatcher.addRunner(information);

		verify(information).setResultListener(any());
		verify(information).setOnIdle(any());
		verify(information).setOnDisconnected(any());

		information.setRunnerInformation(getRunnerInformation());

		assertThat(dispatcher.getKnownRunners()).containsExactly(
			KnownRunner.fromRunnerInformation(getRunnerInformation(), null)
		);
	}

	@Test
	void resetsAddedRunnerWithoutCommit() throws IOException {
		ServerRunnerStateMachine stateMachine = mock(ServerRunnerStateMachine.class);
		ActiveRunnerInformation information = spy(new ActiveRunnerInformation(
			mock(RunnerConnectionManager.class), stateMachine
		));
		dispatcher.addRunner(information);

		information.setRunnerInformation(getRunnerInformation(RunnerStatusEnum.WORKING));

		verify(stateMachine).resetRunner(anyString());
	}

	@Test
	void doNotResetRunnerIfCommitIsStillScheduled() throws IOException {
		ServerRunnerStateMachine stateMachine = mock(ServerRunnerStateMachine.class);
		ActiveRunnerInformation information = spy(new ActiveRunnerInformation(
			mock(RunnerConnectionManager.class), stateMachine
		));
		Commit commit = mock(Commit.class);
		when(commit.getHash()).thenReturn(new CommitHash("hash"));
		when(information.getCurrentCommit()).thenReturn(Optional.of(commit));

		// Will reset it here
		dispatcher.addRunner(information);
		information.setRunnerInformation(getRunnerInformation(RunnerStatusEnum.WORKING));
		verify(stateMachine, times(1)).resetRunner(anyString());

		information = spy(new ActiveRunnerInformation(
			mock(RunnerConnectionManager.class), stateMachine
		));
		when(information.getCurrentCommit()).thenReturn(Optional.of(commit));

		// Will not reset it here
		dispatcher.addRunner(information);

		information.setRunnerInformation(getRunnerInformation(RunnerStatusEnum.WORKING));

		verify(stateMachine, times(1)).resetRunner(anyString());
	}

	@Test
	void abortResetsRunner() throws IOException {
		ActiveRunnerInformation information = addAndGetRunner(new RunnerWorkingState());
		ServerRunnerStateMachine stateMachine = information.getRunnerStateMachine();

		assertThat(dispatcher.abort(COMMIT_HASH, REPO_ID)).isTrue();

		verify(stateMachine).resetRunner(anyString());
	}

	@Test
	void gracefullyDisconnectedRunnersAreRemovedInstantly() {
		ActiveRunnerInformation runner = addAndGetRunner(new RunnerIdleState());

		runner.setDisconnected(StatusCodeMappings.CLIENT_ORDERLY_DISCONNECT);

		verify(runner.getConnectionManager()).disconnect();
	}

	@Test
	void ungracefullyDisconnectedRunnersAreLeftAloneForAWhile() throws InterruptedException {
		ActiveRunnerInformation runner = addAndGetRunner(new RunnerIdleState());

		runner.setDisconnected(1000);
		doReturn(RunnerStatusEnum.DISCONNECTED).when(runner).getState();

		verify(runner.getConnectionManager(), times(0)).disconnect();

		Thread.sleep(2 * GRACE_PERIOD_SECONDS * 1000);
		verify(runner.getConnectionManager()).disconnect();
	}

	@Test
	void storesResultsInDb() {
		ActiveRunnerInformation runner = addAndGetRunner(new RunnerIdleState());

		runner.getRunnerStateMachine().onWorkDone(new BenchmarkResults(
			new RunnerWorkOrder(REPO_ID.getId(), COMMIT_HASH.getHash()),
			List.of(
				new Benchmark(
					"Test",
					List.of(new Metric(
						"Metric", "unit", MetricInterpretation.NEUTRAL, List.of(20d), null)
					))
			),
			Instant.now(),
			Instant.now()
		));

		verify(benchmarkAccess).addRun(any());
		verify(queue).finishTask(REPO_ID, COMMIT_HASH);
	}

	@Test
	void storesFailedResultsInDb() {
		ActiveRunnerInformation runner = addAndGetRunner(new RunnerIdleState());

		runner.getRunnerStateMachine().onWorkDone(new BenchmarkResults(
			new RunnerWorkOrder(REPO_ID.getId(), COMMIT_HASH.getHash()),
			"error", Instant.now(), Instant.now()
		));

		verify(benchmarkAccess).addRun(any());
		verify(queue).finishTask(REPO_ID, COMMIT_HASH);
	}

	@Test
	void dispatchesCommitToRunner() throws InterruptedException, IOException {
		ActiveRunnerInformation runner = addAndGetRunner(new RunnerIdleState());

		Commit commit = mock(Commit.class);
		when(commit.getHash()).thenReturn(COMMIT_HASH);
		when(commit.getRepoId()).thenReturn(REPO_ID);
		when(queue.getNextTask()).thenReturn(Optional.of(commit));

		runner.setIdle();

		// TODO: 08.02.20 This is ugly!
		Thread.sleep(500);

		verify(runner.getRunnerStateMachine(), atLeastOnce()).markAsMyCommit(commit);
		verify(runner.getRunnerStateMachine()).sendBenchmarkRepo(
			any(), eq(BENCH_REPO_HASH.getHash())
		);
		verify(runner.getRunnerStateMachine()).startWork(
			eq(commit), any(), any()
		);
	}

	private ActiveRunnerInformation addAndGetRunner(RunnerState state) {
		ServerRunnerStateMachine stateMachine = spy(new ServerRunnerStateMachine());
		RunnerConnectionManager connectionManager = mock(RunnerConnectionManager.class);
		//noinspection ResultOfMethodCallIgnored
		doReturn(state).when(stateMachine).getState();
		ActiveRunnerInformation information = spy(new ActiveRunnerInformation(
			connectionManager, stateMachine
		));

		stateMachine.onConnectionOpened(information);

		Commit commit = mock(Commit.class);
		when(commit.getHash()).thenReturn(COMMIT_HASH);
		when(commit.getRepoId()).thenReturn(REPO_ID);
		when(information.getCurrentCommit()).thenReturn(Optional.of(commit));

		dispatcher.addRunner(information);

		information.setRunnerInformation(getRunnerInformation());
		return information;
	}

	private RunnerInformation getRunnerInformation() {
		return getRunnerInformation(RunnerStatusEnum.IDLE);
	}

	private RunnerInformation getRunnerInformation(RunnerStatusEnum status) {
		return new RunnerInformation(
			"Name", "OS", 20, 210, status, "hash"
		);
	}

}