package de.aaaaaaah.velcom.backend.access.committaccess;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.aaaaaaah.velcom.backend.TestDb;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.FullCommit;
import de.aaaaaaah.velcom.backend.access.committaccess.exceptions.NoSuchCommitException;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommitReadAccessTest {

	/*
	 * The commits for this test have the following structure.
	 *
	 *               H
	 *              /
	 *        C -- D -- E
	 *       /      \
	 * A -- B ------ F -- G
	 *                \
	 *                 I
	 *
	 * Commit E is on a tracked branch while commit G is on an untracked branch.
	 * Thus, commits A, B, C, D and E are tracked while commits F and G are untracked.
	 * Commits H and I are not reachable from any branch.
	 */

	private static final RepoId REPO_ID = new RepoId();

	private static final CommitHash COMM_A_HASH =
		new CommitHash("07dcbfbee1c5c833614d00ce70c15621c939806c");
	private static final CommitHash COMM_B_HASH =
		new CommitHash("57b0e77894a2b7270ade8767b355ed8a283fffb0");
	private static final CommitHash COMM_C_HASH =
		new CommitHash("a23577b29bfe6e384bba835b4453d0fc7f33855c");
	private static final CommitHash COMM_D_HASH =
		new CommitHash("2acba5b560711dc4c8e53c356238862c07712eca");
	private static final CommitHash COMM_E_HASH =
		new CommitHash("5dd4f1f6f0b3d5d5d830c6e4789d6f161496fa81");
	private static final CommitHash COMM_F_HASH =
		new CommitHash("50061f6cb32961b06db0ade9d5114dc1ec3690bd");
	private static final CommitHash COMM_G_HASH =
		new CommitHash("8ae1895217862be76b98bc46fb94cea9e006e83a");
	private static final CommitHash COMM_H_HASH =
		new CommitHash("c7eed2922f1a406edec6c7e043309a92e6a16a89");
	private static final CommitHash COMM_I_HASH =
		new CommitHash("e9a98785fabfa10307ee4ab637537a67c86f0e67");

	private CommitReadAccess access;

	@BeforeEach
	void setUp(@TempDir Path tempDir) {
		TestDb testDb = new TestDb(tempDir);

		testDb.addRepo(REPO_ID);
		testDb.addCommit(REPO_ID, COMM_A_HASH, true, true, "A");
		testDb.addCommit(REPO_ID, COMM_B_HASH, true, true, "B");
		testDb.addCommit(REPO_ID, COMM_C_HASH, true, true, "C");
		testDb.addCommit(REPO_ID, COMM_D_HASH, true, true, "D");
		testDb.addCommit(REPO_ID, COMM_E_HASH, true, true, "E");
		testDb.addCommit(REPO_ID, COMM_F_HASH, true, false, "F");
		testDb.addCommit(REPO_ID, COMM_G_HASH, true, false, "G");
		testDb.addCommit(REPO_ID, COMM_H_HASH, false, false, "H");
		testDb.addCommit(REPO_ID, COMM_I_HASH, false, false, "I");
		testDb.addCommitRel(REPO_ID, COMM_A_HASH, COMM_B_HASH);
		testDb.addCommitRel(REPO_ID, COMM_B_HASH, COMM_C_HASH);
		testDb.addCommitRel(REPO_ID, COMM_B_HASH, COMM_F_HASH);
		testDb.addCommitRel(REPO_ID, COMM_C_HASH, COMM_D_HASH);
		testDb.addCommitRel(REPO_ID, COMM_D_HASH, COMM_E_HASH);
		testDb.addCommitRel(REPO_ID, COMM_D_HASH, COMM_F_HASH);
		testDb.addCommitRel(REPO_ID, COMM_D_HASH, COMM_H_HASH);
		testDb.addCommitRel(REPO_ID, COMM_F_HASH, COMM_G_HASH);
		testDb.addCommitRel(REPO_ID, COMM_F_HASH, COMM_I_HASH);

		DatabaseStorage databaseStorage = new DatabaseStorage(testDb.closeAndGetJdbcUrl());
		access = new CommitReadAccess(databaseStorage);
	}

	@Test
	void getCommit() {
		// TODO: 2020-12-24 Also check author and committer info?

		Commit commitA = access.getCommit(REPO_ID, COMM_A_HASH);
		assertThat(commitA.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitA.getHash()).isEqualTo(COMM_A_HASH);
		assertThat(commitA.getMessage()).isEqualTo("A");
		assertThat(commitA.isReachable()).isTrue();
		assertThat(commitA.isTracked()).isTrue();

		Commit commitB = access.getCommit(REPO_ID, COMM_B_HASH);
		assertThat(commitB.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitB.getHash()).isEqualTo(COMM_B_HASH);
		assertThat(commitB.getMessage()).isEqualTo("B");
		assertThat(commitB.isReachable()).isTrue();
		assertThat(commitB.isTracked()).isTrue();

		Commit commitC = access.getCommit(REPO_ID, COMM_C_HASH);
		assertThat(commitC.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitC.getHash()).isEqualTo(COMM_C_HASH);
		assertThat(commitC.getMessage()).isEqualTo("C");
		assertThat(commitC.isReachable()).isTrue();
		assertThat(commitC.isTracked()).isTrue();

		Commit commitD = access.getCommit(REPO_ID, COMM_D_HASH);
		assertThat(commitD.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitD.getHash()).isEqualTo(COMM_D_HASH);
		assertThat(commitD.getMessage()).isEqualTo("D");
		assertThat(commitD.isReachable()).isTrue();
		assertThat(commitD.isTracked()).isTrue();

		Commit commitE = access.getCommit(REPO_ID, COMM_E_HASH);
		assertThat(commitE.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitE.getHash()).isEqualTo(COMM_E_HASH);
		assertThat(commitE.getMessage()).isEqualTo("E");
		assertThat(commitE.isReachable()).isTrue();
		assertThat(commitE.isTracked()).isTrue();

		Commit commitF = access.getCommit(REPO_ID, COMM_F_HASH);
		assertThat(commitF.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitF.getHash()).isEqualTo(COMM_F_HASH);
		assertThat(commitF.getMessage()).isEqualTo("F");
		assertThat(commitF.isReachable()).isTrue();
		assertThat(commitF.isTracked()).isFalse();

		Commit commitG = access.getCommit(REPO_ID, COMM_G_HASH);
		assertThat(commitG.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitG.getHash()).isEqualTo(COMM_G_HASH);
		assertThat(commitG.getMessage()).isEqualTo("G");
		assertThat(commitG.isReachable()).isTrue();
		assertThat(commitG.isTracked()).isFalse();

		Commit commitH = access.getCommit(REPO_ID, COMM_H_HASH);
		assertThat(commitH.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitH.getHash()).isEqualTo(COMM_H_HASH);
		assertThat(commitH.getMessage()).isEqualTo("H");
		assertThat(commitH.isReachable()).isFalse();
		assertThat(commitH.isTracked()).isFalse();

		Commit commitI = access.getCommit(REPO_ID, COMM_I_HASH);
		assertThat(commitI.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitI.getHash()).isEqualTo(COMM_I_HASH);
		assertThat(commitI.getMessage()).isEqualTo("I");
		assertThat(commitI.isReachable()).isFalse();
		assertThat(commitI.isTracked()).isFalse();

		RepoId nonexistentId = new RepoId();
		CommitHash nonexistentHash = new CommitHash("f49028fa485c0bcda6104c8c8b4e97addbde7079");

		assertThatThrownBy(() -> access.getCommit(REPO_ID, nonexistentHash))
			.isInstanceOf(NoSuchCommitException.class)
			.satisfies(throwable -> {
				NoSuchCommitException e = (NoSuchCommitException) throwable;
				assertThat(e.getRepoId()).isEqualTo(REPO_ID);
				assertThat(e.getCommitHash()).isEqualTo(nonexistentHash);
			});

		assertThatThrownBy(() -> access.getCommit(nonexistentId, COMM_A_HASH))
			.isInstanceOf(NoSuchCommitException.class)
			.satisfies(throwable -> {
				NoSuchCommitException e = (NoSuchCommitException) throwable;
				assertThat(e.getRepoId()).isEqualTo(nonexistentId);
				assertThat(e.getCommitHash()).isEqualTo(COMM_A_HASH);
			});
	}

	@Test
	void guarding() {
		RepoId nonexistentId = new RepoId();
		CommitHash nonexistentHash = new CommitHash("f49028fa485c0bcda6104c8c8b4e97addbde7079");

		assertThatCode(() -> access.guardCommitExists(REPO_ID, COMM_A_HASH)).doesNotThrowAnyException();
		assertThatCode(() -> access.guardCommitExists(REPO_ID, COMM_B_HASH)).doesNotThrowAnyException();
		assertThatCode(() -> access.guardCommitExists(REPO_ID, COMM_C_HASH)).doesNotThrowAnyException();
		assertThatCode(() -> access.guardCommitExists(REPO_ID, COMM_D_HASH)).doesNotThrowAnyException();
		assertThatCode(() -> access.guardCommitExists(REPO_ID, COMM_E_HASH)).doesNotThrowAnyException();
		assertThatCode(() -> access.guardCommitExists(REPO_ID, COMM_F_HASH)).doesNotThrowAnyException();
		assertThatCode(() -> access.guardCommitExists(REPO_ID, COMM_G_HASH)).doesNotThrowAnyException();
		assertThatCode(() -> access.guardCommitExists(REPO_ID, COMM_H_HASH)).doesNotThrowAnyException();
		assertThatCode(() -> access.guardCommitExists(REPO_ID, COMM_I_HASH)).doesNotThrowAnyException();

		assertThatThrownBy(() -> access.guardCommitExists(REPO_ID, nonexistentHash))
			.isInstanceOf(NoSuchCommitException.class)
			.satisfies(throwable -> {
				NoSuchCommitException e = (NoSuchCommitException) throwable;
				assertThat(e.getRepoId()).isEqualTo(REPO_ID);
				assertThat(e.getCommitHash()).isEqualTo(nonexistentHash);
			});

		assertThatThrownBy(() -> access.guardCommitExists(nonexistentId, COMM_A_HASH))
			.isInstanceOf(NoSuchCommitException.class)
			.satisfies(throwable -> {
				NoSuchCommitException e = (NoSuchCommitException) throwable;
				assertThat(e.getRepoId()).isEqualTo(nonexistentId);
				assertThat(e.getCommitHash()).isEqualTo(COMM_A_HASH);
			});
	}

	@Test
	void getCommits() {
		RepoId nonexistentId = new RepoId();
		CommitHash nonexistentHash = new CommitHash("f49028fa485c0bcda6104c8c8b4e97addbde7079");

		List<Commit> commits = access
			.getCommits(REPO_ID, List.of(COMM_B_HASH, COMM_G_HASH, COMM_I_HASH, nonexistentHash));

		assertThat(commits.stream()
			.map(Commit::getHash))
			.containsExactlyInAnyOrder(COMM_B_HASH, COMM_G_HASH, COMM_I_HASH);

		assertThat(commits.stream()
			.map(Commit::getRepoId)
			.collect(toSet()))
			.containsExactlyInAnyOrder(REPO_ID);

		Map<CommitHash, Commit> commitMap = commits.stream()
			.collect(toMap(Commit::getHash, it -> it));

		Commit commitB = commitMap.get(COMM_B_HASH);
		assertThat(commitB.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitB.getHash()).isEqualTo(COMM_B_HASH);
		assertThat(commitB.getMessage()).isEqualTo("B");
		assertThat(commitB.isReachable()).isTrue();
		assertThat(commitB.isTracked()).isTrue();

		Commit commitG = commitMap.get(COMM_G_HASH);
		assertThat(commitG.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitG.getHash()).isEqualTo(COMM_G_HASH);
		assertThat(commitG.getMessage()).isEqualTo("G");
		assertThat(commitG.isReachable()).isTrue();
		assertThat(commitG.isTracked()).isFalse();

		Commit commitI = commitMap.get(COMM_I_HASH);
		assertThat(commitI.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitI.getHash()).isEqualTo(COMM_I_HASH);
		assertThat(commitI.getMessage()).isEqualTo("I");
		assertThat(commitI.isReachable()).isFalse();
		assertThat(commitI.isTracked()).isFalse();

		assertThat(access.getCommits(nonexistentId, List.of(COMM_A_HASH, COMM_E_HASH, COMM_I_HASH)))
			.isEmpty();
	}

	@Test
	void getFullCommit() {
		FullCommit commitA = access.getFullCommit(REPO_ID, COMM_A_HASH);
		assertThat(commitA.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitA.getHash()).isEqualTo(COMM_A_HASH);
		assertThat(commitA.getMessage()).isEqualTo("A");
		assertThat(commitA.isReachable()).isTrue();
		assertThat(commitA.isTracked()).isTrue();
		assertThat(commitA.getParentHashes()).containsExactlyInAnyOrder();
		assertThat(commitA.getChildHashes()).containsExactlyInAnyOrder(COMM_B_HASH);

		FullCommit commitB = access.getFullCommit(REPO_ID, COMM_B_HASH);
		assertThat(commitB.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitB.getHash()).isEqualTo(COMM_B_HASH);
		assertThat(commitB.getMessage()).isEqualTo("B");
		assertThat(commitB.isReachable()).isTrue();
		assertThat(commitB.isTracked()).isTrue();
		assertThat(commitB.getParentHashes()).containsExactlyInAnyOrder(COMM_A_HASH);
		assertThat(commitB.getChildHashes()).containsExactlyInAnyOrder(COMM_C_HASH, COMM_F_HASH);

		FullCommit commitC = access.getFullCommit(REPO_ID, COMM_C_HASH);
		assertThat(commitC.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitC.getHash()).isEqualTo(COMM_C_HASH);
		assertThat(commitC.getMessage()).isEqualTo("C");
		assertThat(commitC.isReachable()).isTrue();
		assertThat(commitC.isTracked()).isTrue();
		assertThat(commitC.getParentHashes()).containsExactlyInAnyOrder(COMM_B_HASH);
		assertThat(commitC.getChildHashes()).containsExactlyInAnyOrder(COMM_D_HASH);

		FullCommit commitD = access.getFullCommit(REPO_ID, COMM_D_HASH);
		assertThat(commitD.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitD.getHash()).isEqualTo(COMM_D_HASH);
		assertThat(commitD.getMessage()).isEqualTo("D");
		assertThat(commitD.isReachable()).isTrue();
		assertThat(commitD.isTracked()).isTrue();
		assertThat(commitD.getParentHashes()).containsExactlyInAnyOrder(COMM_C_HASH);
		assertThat(commitD.getChildHashes())
			.containsExactlyInAnyOrder(COMM_E_HASH, COMM_F_HASH, COMM_H_HASH);

		FullCommit commitE = access.getFullCommit(REPO_ID, COMM_E_HASH);
		assertThat(commitE.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitE.getHash()).isEqualTo(COMM_E_HASH);
		assertThat(commitE.getMessage()).isEqualTo("E");
		assertThat(commitE.isReachable()).isTrue();
		assertThat(commitE.isTracked()).isTrue();
		assertThat(commitE.getParentHashes()).containsExactlyInAnyOrder(COMM_D_HASH);
		assertThat(commitE.getChildHashes()).containsExactlyInAnyOrder();

		FullCommit commitF = access.getFullCommit(REPO_ID, COMM_F_HASH);
		assertThat(commitF.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitF.getHash()).isEqualTo(COMM_F_HASH);
		assertThat(commitF.getMessage()).isEqualTo("F");
		assertThat(commitF.isReachable()).isTrue();
		assertThat(commitF.isTracked()).isFalse();
		assertThat(commitF.getParentHashes()).containsExactlyInAnyOrder(COMM_B_HASH, COMM_D_HASH);
		assertThat(commitF.getChildHashes()).containsExactlyInAnyOrder(COMM_G_HASH, COMM_I_HASH);

		FullCommit commitG = access.getFullCommit(REPO_ID, COMM_G_HASH);
		assertThat(commitG.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitG.getHash()).isEqualTo(COMM_G_HASH);
		assertThat(commitG.getMessage()).isEqualTo("G");
		assertThat(commitG.isReachable()).isTrue();
		assertThat(commitG.isTracked()).isFalse();
		assertThat(commitG.getParentHashes()).containsExactlyInAnyOrder(COMM_F_HASH);
		assertThat(commitG.getChildHashes()).containsExactlyInAnyOrder();

		FullCommit commitH = access.getFullCommit(REPO_ID, COMM_H_HASH);
		assertThat(commitH.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitH.getHash()).isEqualTo(COMM_H_HASH);
		assertThat(commitH.getMessage()).isEqualTo("H");
		assertThat(commitH.isReachable()).isFalse();
		assertThat(commitH.isTracked()).isFalse();
		assertThat(commitH.getParentHashes()).containsExactlyInAnyOrder(COMM_D_HASH);
		assertThat(commitH.getChildHashes()).containsExactlyInAnyOrder();

		FullCommit commitI = access.getFullCommit(REPO_ID, COMM_I_HASH);
		assertThat(commitI.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitI.getHash()).isEqualTo(COMM_I_HASH);
		assertThat(commitI.getMessage()).isEqualTo("I");
		assertThat(commitI.isReachable()).isFalse();
		assertThat(commitI.isTracked()).isFalse();
		assertThat(commitI.getParentHashes()).containsExactlyInAnyOrder(COMM_F_HASH);
		assertThat(commitI.getChildHashes()).containsExactlyInAnyOrder();

		RepoId nonexistentId = new RepoId();
		CommitHash nonexistentHash = new CommitHash("f49028fa485c0bcda6104c8c8b4e97addbde7079");

		assertThatThrownBy(() -> access.getFullCommit(REPO_ID, nonexistentHash))
			.isInstanceOf(NoSuchCommitException.class)
			.satisfies(throwable -> {
				NoSuchCommitException e = (NoSuchCommitException) throwable;
				assertThat(e.getRepoId()).isEqualTo(REPO_ID);
				assertThat(e.getCommitHash()).isEqualTo(nonexistentHash);
			});

		assertThatThrownBy(() -> access.getFullCommit(nonexistentId, COMM_A_HASH))
			.isInstanceOf(NoSuchCommitException.class)
			.satisfies(throwable -> {
				NoSuchCommitException e = (NoSuchCommitException) throwable;
				assertThat(e.getRepoId()).isEqualTo(nonexistentId);
				assertThat(e.getCommitHash()).isEqualTo(COMM_A_HASH);
			});
	}

	@Test
	void promoteCommits() {
		CommitHash nonexistentHash = new CommitHash("f49028fa485c0bcda6104c8c8b4e97addbde7079");

		List<FullCommit> commits = access.promoteCommits(access.getCommits(REPO_ID,
			List.of(COMM_A_HASH, COMM_B_HASH, COMM_C_HASH, COMM_D_HASH, COMM_E_HASH, COMM_F_HASH,
				COMM_G_HASH, COMM_H_HASH, nonexistentHash)));

		assertThat(commits.stream()
			.map(Commit::getHash))
			.containsExactlyInAnyOrder(COMM_A_HASH, COMM_B_HASH, COMM_C_HASH, COMM_D_HASH, COMM_E_HASH,
				COMM_F_HASH, COMM_G_HASH, COMM_H_HASH);

		Map<CommitHash, FullCommit> commitMap = commits.stream()
			.collect(toMap(Commit::getHash, it -> it));

		FullCommit commitA = commitMap.get(COMM_A_HASH);
		assertThat(commitA.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitA.getHash()).isEqualTo(COMM_A_HASH);
		assertThat(commitA.getMessage()).isEqualTo("A");
		assertThat(commitA.isReachable()).isTrue();
		assertThat(commitA.isTracked()).isTrue();
		assertThat(commitA.getParentHashes()).isEmpty();
		assertThat(commitA.getChildHashes()).containsExactlyInAnyOrder(COMM_B_HASH);

		FullCommit commitB = commitMap.get(COMM_B_HASH);
		assertThat(commitB.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitB.getHash()).isEqualTo(COMM_B_HASH);
		assertThat(commitB.getMessage()).isEqualTo("B");
		assertThat(commitB.isReachable()).isTrue();
		assertThat(commitB.isTracked()).isTrue();
		assertThat(commitB.getParentHashes()).containsExactlyInAnyOrder(COMM_A_HASH);
		assertThat(commitB.getChildHashes()).containsExactlyInAnyOrder(COMM_C_HASH, COMM_F_HASH);

		FullCommit commitC = commitMap.get(COMM_C_HASH);
		assertThat(commitC.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitC.getHash()).isEqualTo(COMM_C_HASH);
		assertThat(commitC.getMessage()).isEqualTo("C");
		assertThat(commitC.isReachable()).isTrue();
		assertThat(commitC.isTracked()).isTrue();
		assertThat(commitC.getParentHashes()).containsExactlyInAnyOrder(COMM_B_HASH);
		assertThat(commitC.getChildHashes()).containsExactlyInAnyOrder(COMM_D_HASH);

		FullCommit commitD = commitMap.get(COMM_D_HASH);
		assertThat(commitD.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitD.getHash()).isEqualTo(COMM_D_HASH);
		assertThat(commitD.getMessage()).isEqualTo("D");
		assertThat(commitD.isReachable()).isTrue();
		assertThat(commitD.isTracked()).isTrue();
		assertThat(commitD.getParentHashes()).containsExactlyInAnyOrder(COMM_C_HASH);
		assertThat(commitD.getChildHashes())
			.containsExactlyInAnyOrder(COMM_E_HASH, COMM_F_HASH, COMM_H_HASH);

		FullCommit commitE = commitMap.get(COMM_E_HASH);
		assertThat(commitE.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitE.getHash()).isEqualTo(COMM_E_HASH);
		assertThat(commitE.getMessage()).isEqualTo("E");
		assertThat(commitE.isReachable()).isTrue();
		assertThat(commitE.isTracked()).isTrue();
		assertThat(commitE.getParentHashes()).containsExactlyInAnyOrder(COMM_D_HASH);
		assertThat(commitE.getChildHashes()).isEmpty();

		FullCommit commitF = commitMap.get(COMM_F_HASH);
		assertThat(commitF.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitF.getHash()).isEqualTo(COMM_F_HASH);
		assertThat(commitF.getMessage()).isEqualTo("F");
		assertThat(commitF.isReachable()).isTrue();
		assertThat(commitF.isTracked()).isFalse();
		assertThat(commitF.getParentHashes()).containsExactlyInAnyOrder(COMM_B_HASH, COMM_D_HASH);
		assertThat(commitF.getChildHashes()).containsExactlyInAnyOrder(COMM_G_HASH, COMM_I_HASH);

		FullCommit commitG = commitMap.get(COMM_G_HASH);
		assertThat(commitG.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitG.getHash()).isEqualTo(COMM_G_HASH);
		assertThat(commitG.getMessage()).isEqualTo("G");
		assertThat(commitG.isReachable()).isTrue();
		assertThat(commitG.isTracked()).isFalse();
		assertThat(commitG.getParentHashes()).containsExactlyInAnyOrder(COMM_F_HASH);
		assertThat(commitG.getChildHashes()).isEmpty();

		FullCommit commitH = commitMap.get(COMM_H_HASH);
		assertThat(commitH.getRepoId()).isEqualTo(REPO_ID);
		assertThat(commitH.getHash()).isEqualTo(COMM_H_HASH);
		assertThat(commitH.getMessage()).isEqualTo("H");
		assertThat(commitH.isReachable()).isFalse();
		assertThat(commitH.isTracked()).isFalse();
		assertThat(commitH.getParentHashes()).containsExactlyInAnyOrder(COMM_D_HASH);
		assertThat(commitH.getChildHashes()).isEmpty();
	}

	@Test
	void getParentsAndChildren() {
		assertThat(access.getParentHashes(REPO_ID, COMM_A_HASH)).isEmpty();
		assertThat(access.getParentHashes(REPO_ID, COMM_B_HASH)).containsExactlyInAnyOrder(COMM_A_HASH);
		assertThat(access.getParentHashes(REPO_ID, COMM_C_HASH)).containsExactlyInAnyOrder(COMM_B_HASH);
		assertThat(access.getParentHashes(REPO_ID, COMM_D_HASH)).containsExactlyInAnyOrder(COMM_C_HASH);
		assertThat(access.getParentHashes(REPO_ID, COMM_E_HASH)).containsExactlyInAnyOrder(COMM_D_HASH);
		assertThat(access.getParentHashes(REPO_ID, COMM_F_HASH))
			.containsExactlyInAnyOrder(COMM_B_HASH, COMM_D_HASH);
		assertThat(access.getParentHashes(REPO_ID, COMM_G_HASH)).containsExactlyInAnyOrder(COMM_F_HASH);
		assertThat(access.getParentHashes(REPO_ID, COMM_H_HASH)).containsExactlyInAnyOrder(COMM_D_HASH);
		assertThat(access.getParentHashes(REPO_ID, COMM_I_HASH)).containsExactlyInAnyOrder(COMM_F_HASH);

		assertThat(access.getChildHashes(REPO_ID, COMM_A_HASH)).containsExactlyInAnyOrder(COMM_B_HASH);
		assertThat(access.getChildHashes(REPO_ID, COMM_B_HASH))
			.containsExactlyInAnyOrder(COMM_C_HASH, COMM_F_HASH);
		assertThat(access.getChildHashes(REPO_ID, COMM_C_HASH)).containsExactlyInAnyOrder(COMM_D_HASH);
		assertThat(access.getChildHashes(REPO_ID, COMM_D_HASH))
			.containsExactlyInAnyOrder(COMM_E_HASH, COMM_F_HASH, COMM_H_HASH);
		assertThat(access.getChildHashes(REPO_ID, COMM_E_HASH)).isEmpty();
		assertThat(access.getChildHashes(REPO_ID, COMM_F_HASH))
			.containsExactlyInAnyOrder(COMM_G_HASH, COMM_I_HASH);
		assertThat(access.getChildHashes(REPO_ID, COMM_G_HASH)).isEmpty();
		assertThat(access.getChildHashes(REPO_ID, COMM_H_HASH)).isEmpty();
		assertThat(access.getChildHashes(REPO_ID, COMM_I_HASH)).isEmpty();
	}

	@Test
	void getDescendantCommits() {
		assertThat(access.getDescendantCommits(REPO_ID, COMM_A_HASH))
			.containsExactlyInAnyOrder(COMM_A_HASH, COMM_B_HASH, COMM_C_HASH, COMM_D_HASH, COMM_E_HASH);
		assertThat(access.getDescendantCommits(REPO_ID, COMM_B_HASH))
			.containsExactlyInAnyOrder(COMM_B_HASH, COMM_C_HASH, COMM_D_HASH, COMM_E_HASH);
		assertThat(access.getDescendantCommits(REPO_ID, COMM_C_HASH))
			.containsExactlyInAnyOrder(COMM_C_HASH, COMM_D_HASH, COMM_E_HASH);
		assertThat(access.getDescendantCommits(REPO_ID, COMM_D_HASH))
			.containsExactlyInAnyOrder(COMM_D_HASH, COMM_E_HASH);
		assertThat(access.getDescendantCommits(REPO_ID, COMM_E_HASH))
			.containsExactlyInAnyOrder(COMM_E_HASH);
		assertThat(access.getDescendantCommits(REPO_ID, COMM_F_HASH)).isEmpty();
		assertThat(access.getDescendantCommits(REPO_ID, COMM_G_HASH)).isEmpty();
		assertThat(access.getDescendantCommits(REPO_ID, COMM_H_HASH)).isEmpty();
		assertThat(access.getDescendantCommits(REPO_ID, COMM_I_HASH)).isEmpty();

		RepoId nonexistentId = new RepoId();
		CommitHash nonexistentHash = new CommitHash("f49028fa485c0bcda6104c8c8b4e97addbde7079");

		assertThat(access.getDescendantCommits(nonexistentId, COMM_B_HASH)).isEmpty();
		assertThat(access.getDescendantCommits(REPO_ID, nonexistentHash)).isEmpty();
	}

	// TODO: 2020-12-24 Test getCommitsBetween
	// TODO: 2020-12-24 Test getTrackedCommitsBetween
}
