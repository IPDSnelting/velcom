package de.aaaaaaah.velcom.backend.data.queue;

import de.aaaaaaah.velcom.backend.newaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.newaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PolicyManualFiloTest {

	private QueuePolicy policy;
	private RepoId r1;
	private RepoId r2;
	private Commit c1;
	private Commit c2;
	private Commit c3;

	@BeforeEach
	void setup() {
		policy = new PolicyManualFilo();

		r1 = new RepoId(UUID.randomUUID());
		r2 = new RepoId(UUID.randomUUID());

		c1 = new Commit(
			r1,
			new CommitHash("hash1"), List.of(),
			"author", Instant.now(),
			"committer", Instant.now(),
			"message"
		);

		c2 = new Commit(
			r1,
			new CommitHash("hash2"), List.of(),
			"author", Instant.now(),
			"committer", Instant.now(),
			"message"
		);

		c3 = new Commit(
			r2,
			new CommitHash("hash3"), List.of(),
			"author", Instant.now(),
			"committer", Instant.now(),
			"message"
		);

	}

	@Test
	void manualCommitHasPriority() {
		policy.addTask(c1);
		policy.addManualTask(c2);
		policy.addTask(c3);

		Assertions.assertEquals(
			List.of(c2, c3, c1),
			policy.viewAllCurrentTasks()
		);
	}

	@Test
	void abortingRemovesOnlyCommitsOfSpecificRepo() {
		policy.addTask(c1);
		policy.addManualTask(c2);
		policy.addTask(c3);
		policy.abortAllTasksOfRepo(r1);

		Assertions.assertEquals(
			List.of(c3),
			policy.viewAllCurrentTasks()
		);
	}

}