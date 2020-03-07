package de.aaaaaaah.velcom.backend.runner.single.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.backend.access.RepoWriteAccess;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.backend.runner.single.RunnerConnectionManager;
import de.aaaaaaah.velcom.backend.runner.single.ServerRunnerStateMachine;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.WorkReceived;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PreparingRunnerForWorkStateTest {

	public static final CommitHash COMMIT_HASH = new CommitHash("hash");
	public static final RepoId REPO_ID = new RepoId(UUID.randomUUID());
	private PreparingRunnerForWorkState state;
	private ServerRunnerStateMachine stateMachine;
	private ActiveRunnerInformation runnerInformation;
	private Commit commit;
	private RepoWriteAccess repoWriteAccess;
	private RunnerConnectionManager connectionManager;

	@BeforeEach
	void setUp() {
		commit = mock(Commit.class);
		repoWriteAccess = mock(RepoWriteAccess.class);
		state = new PreparingRunnerForWorkState(
			commit,
			repoWriteAccess
		);
		stateMachine = mock(ServerRunnerStateMachine.class);
		runnerInformation = mock(ActiveRunnerInformation.class);
		connectionManager = mock(RunnerConnectionManager.class);

		when(runnerInformation.getRunnerStateMachine()).thenReturn(stateMachine);
		when(runnerInformation.getConnectionManager())
			.thenReturn(connectionManager);

		when(commit.getHash()).thenReturn(COMMIT_HASH);
		when(commit.getRepoId()).thenReturn(REPO_ID);

		when(repoWriteAccess.getLatestBenchmarkRepoHash()).thenReturn(new CommitHash("hash"));
	}

	@Test
	void switchesToExecutingWhenWorkReceived() {
		WorkReceived workReceived = new WorkReceived(new RunnerWorkOrder(UUID.randomUUID(), "hey"));
		when(runnerInformation.getCurrentCommit()).thenReturn(Optional.of(mock(Commit.class)));

		RunnerState newState = state.onMessage(
			WorkReceived.class.getSimpleName(),
			workReceived,
			runnerInformation
		);

		assertThat(newState).isInstanceOf(RunnerWorkingState.class);
	}

	@Test
	void disconnectsOnOtherMessageType() {
		RunnerConnectionManager connectionManager = mock(RunnerConnectionManager.class);
		when(runnerInformation.getConnectionManager()).thenReturn(connectionManager);

		RunnerState newState = state.onMessage("Hello world!", null, runnerInformation);

		assertThat(newState).isEqualTo(state);
		verify(connectionManager).disconnect(anyInt(), anyString());
	}

	@Test
	void updatesBenchRepo() throws IOException {
		when(runnerInformation.getRunnerInformation()).thenReturn(Optional.of(
			new RunnerInformation(
				"test", "os", 20, 10,
				RunnerStatusEnum.PREPARING_WORK, "beta"
			)
		));
		when(runnerInformation.getCurrentCommit()).thenReturn(Optional.empty());
		state.onSelected(runnerInformation);

		verify(repoWriteAccess, atLeastOnce()).getLatestBenchmarkRepoHash();
		verify(stateMachine).sendBenchmarkRepo(any(), eq("hash"));
	}

	@Test
	void disconnectsIfNoInformationKnown() {
		when(runnerInformation.getRunnerInformation()).thenReturn(Optional.empty());
		when(runnerInformation.getCurrentCommit()).thenReturn(Optional.empty());
		state.onSelected(runnerInformation);

		verify(connectionManager).disconnect();
	}

	@Test
	void startsWork() throws IOException {
		when(runnerInformation.getRunnerInformation()).thenReturn(Optional.of(
			new RunnerInformation(
				"name", "os", 10, 20,
				RunnerStatusEnum.PREPARING_WORK, "hash"
			)
		));
		when(runnerInformation.getCurrentCommit()).thenReturn(Optional.of(commit));
		state.onSelected(runnerInformation);

		verify(stateMachine).startWork(
			eq(commit),
			eq(new RunnerWorkOrder(REPO_ID.getId(), COMMIT_HASH.getHash())),
			any()
		);
	}

	@Test
	void forwardsResultsAndIdlesIfNoWork() {
		// No work to do
		when(runnerInformation.getCurrentCommit()).thenReturn(Optional.empty());

		BenchmarkResults results = new BenchmarkResults(
			new RunnerWorkOrder(REPO_ID.getId(), COMMIT_HASH.getHash()),
			"Error", Instant.now(), Instant.now()
		);
		RunnerState newState = state.onMessage(
			BenchmarkResults.class.getSimpleName(),
			results,
			runnerInformation
		);
		assertThat(newState).isInstanceOf(RunnerIdleState.class);
	}

	@Test
	void forwardsResultsAndKeepsWorkingIfWork() {
		// Work!
		when(runnerInformation.getCurrentCommit()).thenReturn(Optional.of(commit));

		BenchmarkResults results = new BenchmarkResults(
			new RunnerWorkOrder(REPO_ID.getId(), COMMIT_HASH.getHash()),
			"Error", Instant.now(), Instant.now()
		);
		RunnerState newState = state.onMessage(
			BenchmarkResults.class.getSimpleName(),
			results,
			runnerInformation
		);
		assertThat(newState).isSameAs(state);
	}

	@Test
	void correctStatus() {
		assertThat(state.getStatus()).isEqualTo(RunnerStatusEnum.PREPARING_WORK);
	}
}