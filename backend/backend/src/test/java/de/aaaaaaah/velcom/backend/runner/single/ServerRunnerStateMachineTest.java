package de.aaaaaaah.velcom.backend.runner.single;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.backend.newaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.newaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.runner.single.state.RunnerState;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.ResetOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.UpdateBenchmarkRepoOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ServerRunnerStateMachineTest {

	private ServerRunnerStateMachine stateMachine;
	private ActiveRunnerInformation runnerInformation;
	private RunnerConnectionManager connectionManager;

	@BeforeEach
	void setUp() {
		stateMachine = new ServerRunnerStateMachine();
		runnerInformation = mock(ActiveRunnerInformation.class);
		connectionManager = mock(RunnerConnectionManager.class);

		stateMachine.onConnectionOpened(runnerInformation);
		when(runnerInformation.getConnectionManager()).thenReturn(connectionManager);
	}

	@Test
	void onWorkDoneSetsResults() {
		UUID repoId = UUID.randomUUID();
		String commitHash = "hash";
		Commit commit = mock(Commit.class);
		when(commit.getRepoId()).thenReturn(new RepoId(repoId));
		when(commit.getHash()).thenReturn(new CommitHash(commitHash));

		BenchmarkResults results = new BenchmarkResults(
			new RunnerWorkOrder(repoId, commitHash),
			"Error!",
			Instant.now(),
			Instant.now()
		);

		when(runnerInformation.getCurrentCommit()).thenReturn(Optional.of(commit));

		stateMachine.onWorkDone(results);

		verify(runnerInformation).setResults(eq(results));
		verify(runnerInformation).clearCurrentCommit();
	}

	@Test
	void resetSendsReset() throws IOException {
		stateMachine.resetRunner("Test");

		verify(connectionManager).sendEntity(isA(ResetOrder.class));
		verify(runnerInformation).clearCurrentCommit();
	}

	@Test
	void resetDoesNotSwitchState() throws IOException {
		RunnerState previousState = stateMachine.getState();
		stateMachine.resetRunner("Test");

		assertThat(stateMachine.getState()).isSameAs(previousState);
	}

	@Test
	void startFailsIfRunnerIsNotIdle() {
		when(runnerInformation.getState()).thenReturn(RunnerStatusEnum.WORKING);

		assertThatThrownBy(
			() -> stateMachine.startWork(
				mock(Commit.class), mock(RunnerWorkOrder.class), OutputStream::close
			)
		);
	}

	@Test
	void startSetsCommit() throws IOException {
		when(runnerInformation.getState()).thenReturn(RunnerStatusEnum.PREPARING_WORK);
		Commit commit = mock(Commit.class);

		stateMachine.startWork(
			commit, mock(RunnerWorkOrder.class), it -> {
			}
		);

		verify(runnerInformation).setCurrentCommit(eq(commit));
	}

	@Test
	void startSendsOnCorrectOutputStream() throws IOException {
		OutputStream outputStream = mock(OutputStream.class);
		Commit commit = mock(Commit.class);
		when(runnerInformation.getState()).thenReturn(RunnerStatusEnum.PREPARING_WORK);
		when(connectionManager.createBinaryOutputStream()).thenReturn(outputStream);

		AtomicBoolean called = new AtomicBoolean();
		stateMachine.startWork(
			commit, mock(RunnerWorkOrder.class), it -> {
				called.set(true);
				assertThat(it).isEqualTo(outputStream);
			}
		);

		assertThat(called).withFailMessage("Never called output stream writer!").isTrue();
	}

	@Test
	void markAsMineSetsCurrentCommit() {
		Commit commit = mock(Commit.class);
		stateMachine.markAsMyCommit(commit);

		verify(runnerInformation).setCurrentCommit(eq(commit));
	}

	@Test
	void sendBenchRepoSendsUpdateOrder() throws IOException {
		when(runnerInformation.getRunnerInformation()).thenReturn(Optional.empty());

		stateMachine.sendBenchmarkRepo(outputStream -> {
		}, "hash!");

		ArgumentCaptor<SentEntity> captor = ArgumentCaptor.forClass(
			SentEntity.class
		);
		verify(connectionManager).sendEntity(captor.capture());

		assertThat(captor.getValue())
			.isInstanceOf(UpdateBenchmarkRepoOrder.class);
		assertThat(((UpdateBenchmarkRepoOrder) captor.getValue()).getCommitHash())
			.isEqualTo("hash!");
	}

	@Test
	void sendBenchRepoSendsUpdatesHeadHash() throws IOException {
		RunnerInformation information = new RunnerInformation(
			"Test", "OS", 20, 1000, RunnerStatusEnum.IDLE,
			"hash prev"
		);
		when(runnerInformation.getRunnerInformation()).thenReturn(Optional.of(information));

		stateMachine.sendBenchmarkRepo(outputStream -> {
		}, "hash!");

		verify(runnerInformation).setRunnerInformation(eq(
			new RunnerInformation(
				information.getName(), information.getOperatingSystem(), information.getCoreCount(),
				information.getAvailableMemory(), information.getRunnerState(), "hash!"
			)
		));
	}
}