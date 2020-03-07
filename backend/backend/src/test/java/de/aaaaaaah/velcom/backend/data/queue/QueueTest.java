package de.aaaaaaah.velcom.backend.data.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.backend.access.KnownCommitWriteAccess;
import de.aaaaaaah.velcom.backend.access.entities.BenchmarkStatus;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueueTest {

	private KnownCommitWriteAccess access;
	private QueuePolicy policy;
	private Queue queue;

	@BeforeEach
	void setup() {
		access = mock(KnownCommitWriteAccess.class);
		policy = mock(QueuePolicy.class);
		queue = new Queue(access, policy);
	}

	@Test
	void saveCommitsInAccess() {
		RepoId r1 = new RepoId(UUID.randomUUID());
		RepoId r2 = new RepoId(UUID.randomUUID());
		RepoId r3 = new RepoId(UUID.randomUUID());
		RepoId r4 = new RepoId(UUID.randomUUID());

		CommitHash h1 = new CommitHash("hash1");
		CommitHash h2 = new CommitHash("hash2");
		CommitHash h3 = new CommitHash("hash3");
		CommitHash h4 = new CommitHash("hash4");

		Commit c1 = mock(Commit.class);
		Commit c2 = mock(Commit.class);
		Commit c3 = mock(Commit.class);
		Commit c4 = mock(Commit.class);

		when(c1.getRepoId()).thenReturn(r1);
		when(c2.getRepoId()).thenReturn(r2);
		when(c3.getRepoId()).thenReturn(r3);
		when(c4.getRepoId()).thenReturn(r4);

		when(c1.getHash()).thenReturn(h1);
		when(c2.getHash()).thenReturn(h2);
		when(c3.getHash()).thenReturn(h3);
		when(c4.getHash()).thenReturn(h4);

		when(policy.addTask(c1)).thenReturn(true);
		when(policy.addTask(c2)).thenReturn(false);
		when(policy.addManualTask(c3)).thenReturn(true);
		when(policy.addManualTask(c4)).thenReturn(false);

		queue.addTask(c1);
		verify(access).setBenchmarkStatus(r1, h1, BenchmarkStatus.BENCHMARK_REQUIRED);
		verifyNoMoreInteractions(access);

		queue.addTask(c2);
		verifyNoMoreInteractions(access);

		queue.addManualTask(c3);
		verify(access).setBenchmarkStatus(r3, h3,
			BenchmarkStatus.BENCHMARK_REQUIRED_MANUAL_PRIORITY);
		verifyNoMoreInteractions(access);

		queue.addManualTask(c4);
		verifyNoMoreInteractions(access);
	}

	@Test
	void addCommit() {
		RepoId r1 = new RepoId(UUID.randomUUID());
		RepoId r2 = new RepoId(UUID.randomUUID());
		RepoId r3 = new RepoId(UUID.randomUUID());
		CommitHash h1 = new CommitHash("hash1");
		CommitHash h2 = new CommitHash("hash2");
		CommitHash h3 = new CommitHash("hash3");

		Commit c1 = mock(Commit.class);
		Commit c2 = mock(Commit.class);
		Commit c3 = mock(Commit.class);
		when(c1.getRepoId()).thenReturn(r1);
		when(c2.getRepoId()).thenReturn(r2);
		when(c3.getRepoId()).thenReturn(r3);
		when(c1.getHash()).thenReturn(h1);
		when(c2.getHash()).thenReturn(h2);
		when(c3.getHash()).thenReturn(h3);

		when(policy.addTask(c1)).thenReturn(true);
		when(policy.addTask(c2)).thenReturn(true);
		when(policy.addManualTask(c3)).thenReturn(true);

		when(access.getBenchmarkStatus(r1, h1)).thenReturn(BenchmarkStatus.NO_BENCHMARK_REQUIRED);
		when(access.getBenchmarkStatus(r2, h2)).thenReturn(BenchmarkStatus.BENCHMARK_REQUIRED);
		when(access.getBenchmarkStatus(r3, h3)).thenReturn(
			BenchmarkStatus.BENCHMARK_REQUIRED_MANUAL_PRIORITY);

		queue.addCommit(c1);
		verify(access).getBenchmarkStatus(r1, h1);
		verify(access).setBenchmarkStatus(r1, h1, BenchmarkStatus.BENCHMARK_REQUIRED);
		verifyNoMoreInteractions(access);

		queue.addCommit(c2);
		verify(access).getBenchmarkStatus(r2, h2);
		verify(access).setBenchmarkStatus(r2, h2, BenchmarkStatus.BENCHMARK_REQUIRED);
		verifyNoMoreInteractions(access);

		queue.addCommit(c3);
		verify(access).getBenchmarkStatus(r3, h3);
		verify(access).setBenchmarkStatus(r3, h3,
			BenchmarkStatus.BENCHMARK_REQUIRED_MANUAL_PRIORITY);
		verifyNoMoreInteractions(access);
	}

	@Test
	void functionsArePassedThrough() {
		RepoId r1 = new RepoId(UUID.randomUUID());
		CommitHash h1 = new CommitHash("hash1");

		Commit c1 = mock(Commit.class);
		when(c1.getRepoId()).thenReturn(r1);
		when(c1.getHash()).thenReturn(h1);

		when(policy.getNextTask()).thenReturn(Optional.of(c1));
		assertEquals(Optional.of(c1), queue.getNextTask());
		verify(policy).getNextTask();
		verifyNoMoreInteractions(access);
		verifyNoMoreInteractions(policy);

		queue.finishTask(r1, h1);
		verify(access).setBenchmarkStatus(r1, h1, BenchmarkStatus.NO_BENCHMARK_REQUIRED);
		verifyNoMoreInteractions(access);
		verifyNoMoreInteractions(policy);

		when(policy.viewAllCurrentTasks()).thenReturn(List.of(c1));
		assertEquals(List.of(c1), queue.viewAllCurrentTasks());
		verify(policy).viewAllCurrentTasks();
		verifyNoMoreInteractions(access);
		verifyNoMoreInteractions(policy);

		queue.abortTask(r1, h1);
		verify(access, times(2)).setBenchmarkStatus(r1, h1, BenchmarkStatus.NO_BENCHMARK_REQUIRED);
		verify(policy).abortTask(r1, h1);
		verifyNoMoreInteractions(access);
		verifyNoMoreInteractions(policy);
	}

}