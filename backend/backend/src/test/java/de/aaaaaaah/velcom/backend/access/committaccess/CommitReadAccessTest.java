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
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommitReadAccessTest {

	/*
	 * The commits for this test have the following structure.
	 *
	 *               H
	 *              /
	 *        C -- D -- E <= T
	 *       /      \
	 * A -- B ------ F -- G <= U
	 *                \
	 *                 I
	 *
	 * Commit E is on a tracked branch named T while commit G is on an untracked branch named U.
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

	private static final BranchName BRANCH_T = BranchName.fromName("T");
	private static final BranchName BRANCH_U = BranchName.fromName("U");

	private CommitReadAccess access;

	@BeforeEach
	void setUp(@TempDir Path tempDir) {
		TestDb testDb = new TestDb(tempDir);

		testDb.addRepo(REPO_ID);
		testDb.addCommit(REPO_ID, COMM_A_HASH, true, true, true,
			"aA", Instant.ofEpochSecond(1600010001), "cA", Instant.ofEpochSecond(1600010002), "A");
		testDb.addCommit(REPO_ID, COMM_B_HASH, true, true, true,
			"aB", Instant.ofEpochSecond(1600020001), "cB", Instant.ofEpochSecond(1600020002), "B");
		testDb.addCommit(REPO_ID, COMM_C_HASH, true, true, true,
			"aC", Instant.ofEpochSecond(1600030001), "cC", Instant.ofEpochSecond(1600030002), "C");
		testDb.addCommit(REPO_ID, COMM_D_HASH, true, true, true,
			"aD", Instant.ofEpochSecond(1600040001), "cD", Instant.ofEpochSecond(1600040002), "D");
		testDb.addCommit(REPO_ID, COMM_E_HASH, true, true, true,
			"aE", Instant.ofEpochSecond(1600050001), "cE", Instant.ofEpochSecond(1600050002), "E");
		testDb.addCommit(REPO_ID, COMM_F_HASH, true, false, true,
			"aF", Instant.ofEpochSecond(1600060001), "cF", Instant.ofEpochSecond(1600060002), "F");
		testDb.addCommit(REPO_ID, COMM_G_HASH, true, false, true,
			"aG", Instant.ofEpochSecond(1600070001), "cG", Instant.ofEpochSecond(1600070002), "G");
		testDb.addCommit(REPO_ID, COMM_H_HASH, false, false, true,
			"aH", Instant.ofEpochSecond(1600080001), "cH", Instant.ofEpochSecond(1600080002), "H");
		testDb.addCommit(REPO_ID, COMM_I_HASH, false, false, true,
			"aI", Instant.ofEpochSecond(1600090001), "cI", Instant.ofEpochSecond(1600090002), "I");
		testDb.addCommitRel(REPO_ID, COMM_A_HASH, COMM_B_HASH);
		testDb.addCommitRel(REPO_ID, COMM_B_HASH, COMM_C_HASH);
		testDb.addCommitRel(REPO_ID, COMM_B_HASH, COMM_F_HASH);
		testDb.addCommitRel(REPO_ID, COMM_C_HASH, COMM_D_HASH);
		testDb.addCommitRel(REPO_ID, COMM_D_HASH, COMM_E_HASH);
		testDb.addCommitRel(REPO_ID, COMM_D_HASH, COMM_F_HASH);
		testDb.addCommitRel(REPO_ID, COMM_D_HASH, COMM_H_HASH);
		testDb.addCommitRel(REPO_ID, COMM_F_HASH, COMM_G_HASH);
		testDb.addCommitRel(REPO_ID, COMM_F_HASH, COMM_I_HASH);
		testDb.addBranch(REPO_ID, BRANCH_T, COMM_E_HASH, true);
		testDb.addBranch(REPO_ID, BRANCH_U, COMM_G_HASH, false);

		DatabaseStorage databaseStorage = new DatabaseStorage(testDb.closeAndGetJdbcUrl());
		access = new CommitReadAccess(databaseStorage);
	}

	private void checkCommit(Commit commit, RepoId repoId, CommitHash commitHash, boolean isReachable,
		boolean isTracked, String author, int authorDate, String committer, int committerDate,
		String message) {

		assertThat(commit.getRepoId()).isEqualTo(repoId);
		assertThat(commit.getHash()).isEqualTo(commitHash);
		assertThat(commit.isReachable()).isEqualTo(isReachable);
		assertThat(commit.isTracked()).isEqualTo(isTracked);
		assertThat(commit.getAuthor()).isEqualTo(author);
		assertThat(commit.getAuthorDate()).isEqualTo(Instant.ofEpochSecond(authorDate));
		assertThat(commit.getCommitter()).isEqualTo(committer);
		assertThat(commit.getCommitterDate()).isEqualTo(Instant.ofEpochSecond(committerDate));
		assertThat(commit.getMessage()).isEqualTo(message);
	}

	private void checkFullCommit(FullCommit commit, RepoId repoId, CommitHash commitHash,
		boolean isReachable, boolean isTracked, String author, int authorDate, String committer,
		int committerDate, String message, List<CommitHash> parents, List<CommitHash> children) {

		checkCommit(commit, repoId, commitHash, isReachable, isTracked, author, authorDate, committer,
			committerDate, message);

		assertThat(commit.getParentHashes()).containsExactlyInAnyOrderElementsOf(parents);
		assertThat(commit.getChildHashes()).containsExactlyInAnyOrderElementsOf(children);
	}

	@Test
	void getCommit() {
		checkCommit(access.getCommit(REPO_ID, COMM_A_HASH), REPO_ID, COMM_A_HASH, true, true,
			"aA", 1600010001, "cA", 1600010002, "A");
		checkCommit(access.getCommit(REPO_ID, COMM_B_HASH), REPO_ID, COMM_B_HASH, true, true,
			"aB", 1600020001, "cB", 1600020002, "B");
		checkCommit(access.getCommit(REPO_ID, COMM_C_HASH), REPO_ID, COMM_C_HASH, true, true,
			"aC", 1600030001, "cC", 1600030002, "C");
		checkCommit(access.getCommit(REPO_ID, COMM_D_HASH), REPO_ID, COMM_D_HASH, true, true,
			"aD", 1600040001, "cD", 1600040002, "D");
		checkCommit(access.getCommit(REPO_ID, COMM_E_HASH), REPO_ID, COMM_E_HASH, true, true,
			"aE", 1600050001, "cE", 1600050002, "E");
		checkCommit(access.getCommit(REPO_ID, COMM_F_HASH), REPO_ID, COMM_F_HASH, true, false,
			"aF", 1600060001, "cF", 1600060002, "F");
		checkCommit(access.getCommit(REPO_ID, COMM_G_HASH), REPO_ID, COMM_G_HASH, true, false,
			"aG", 1600070001, "cG", 1600070002, "G");
		checkCommit(access.getCommit(REPO_ID, COMM_H_HASH), REPO_ID, COMM_H_HASH, false, false,
			"aH", 1600080001, "cH", 1600080002, "H");
		checkCommit(access.getCommit(REPO_ID, COMM_I_HASH), REPO_ID, COMM_I_HASH, false, false,
			"aI", 1600090001, "cI", 1600090002, "I");

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

		checkCommit(commitMap.get(COMM_B_HASH), REPO_ID, COMM_B_HASH, true, true,
			"aB", 1600020001, "cB", 1600020002, "B");
		checkCommit(commitMap.get(COMM_G_HASH), REPO_ID, COMM_G_HASH, true, false,
			"aG", 1600070001, "cG", 1600070002, "G");
		checkCommit(commitMap.get(COMM_I_HASH), REPO_ID, COMM_I_HASH, false, false,
			"aI", 1600090001, "cI", 1600090002, "I");

		assertThat(access.getCommits(nonexistentId, List.of(COMM_A_HASH, COMM_E_HASH, COMM_I_HASH)))
			.isEmpty();
	}

	@Test
	void getFullCommit() {
		checkFullCommit(access.getFullCommit(REPO_ID, COMM_A_HASH), REPO_ID, COMM_A_HASH, true, true,
			"aA", 1600010001, "cA", 1600010002, "A",
			List.of(), List.of(COMM_B_HASH));
		checkFullCommit(access.getFullCommit(REPO_ID, COMM_B_HASH), REPO_ID, COMM_B_HASH, true, true,
			"aB", 1600020001, "cB", 1600020002, "B",
			List.of(COMM_A_HASH), List.of(COMM_C_HASH, COMM_F_HASH));
		checkFullCommit(access.getFullCommit(REPO_ID, COMM_C_HASH), REPO_ID, COMM_C_HASH, true, true,
			"aC", 1600030001, "cC", 1600030002, "C",
			List.of(COMM_B_HASH), List.of(COMM_D_HASH));
		checkFullCommit(access.getFullCommit(REPO_ID, COMM_D_HASH), REPO_ID, COMM_D_HASH, true, true,
			"aD", 1600040001, "cD", 1600040002, "D",
			List.of(COMM_C_HASH), List.of(COMM_E_HASH, COMM_F_HASH, COMM_H_HASH));
		checkFullCommit(access.getFullCommit(REPO_ID, COMM_E_HASH), REPO_ID, COMM_E_HASH, true, true,
			"aE", 1600050001, "cE", 1600050002, "E",
			List.of(COMM_D_HASH), List.of());
		checkFullCommit(access.getFullCommit(REPO_ID, COMM_F_HASH), REPO_ID, COMM_F_HASH, true, false,
			"aF", 1600060001, "cF", 1600060002, "F",
			List.of(COMM_B_HASH, COMM_D_HASH), List.of(COMM_G_HASH, COMM_I_HASH));
		checkFullCommit(access.getFullCommit(REPO_ID, COMM_G_HASH), REPO_ID, COMM_G_HASH, true, false,
			"aG", 1600070001, "cG", 1600070002, "G",
			List.of(COMM_F_HASH), List.of());
		checkFullCommit(access.getFullCommit(REPO_ID, COMM_H_HASH), REPO_ID, COMM_H_HASH, false, false,
			"aH", 1600080001, "cH", 1600080002, "H",
			List.of(COMM_D_HASH), List.of());
		checkFullCommit(access.getFullCommit(REPO_ID, COMM_I_HASH), REPO_ID, COMM_I_HASH, false, false,
			"aI", 1600090001, "cI", 1600090002, "I",
			List.of(COMM_F_HASH), List.of());

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

		// Missing COMM_I_HASH and including one nonexistent hash
		List<FullCommit> commits = access.promoteCommits(access.getCommits(REPO_ID,
			List.of(COMM_A_HASH, COMM_B_HASH, COMM_C_HASH, COMM_D_HASH, COMM_E_HASH, COMM_F_HASH,
				COMM_G_HASH, COMM_H_HASH, nonexistentHash)));

		assertThat(commits.stream()
			.map(Commit::getHash))
			.containsExactlyInAnyOrder(COMM_A_HASH, COMM_B_HASH, COMM_C_HASH, COMM_D_HASH, COMM_E_HASH,
				COMM_F_HASH, COMM_G_HASH, COMM_H_HASH);

		Map<CommitHash, FullCommit> commitMap = commits.stream()
			.collect(toMap(Commit::getHash, it -> it));

		checkFullCommit(commitMap.get(COMM_A_HASH), REPO_ID, COMM_A_HASH, true, true,
			"aA", 1600010001, "cA", 1600010002, "A",
			List.of(), List.of(COMM_B_HASH));
		checkFullCommit(commitMap.get(COMM_B_HASH), REPO_ID, COMM_B_HASH, true, true,
			"aB", 1600020001, "cB", 1600020002, "B",
			List.of(COMM_A_HASH), List.of(COMM_C_HASH, COMM_F_HASH));
		checkFullCommit(commitMap.get(COMM_C_HASH), REPO_ID, COMM_C_HASH, true, true,
			"aC", 1600030001, "cC", 1600030002, "C",
			List.of(COMM_B_HASH), List.of(COMM_D_HASH));
		checkFullCommit(commitMap.get(COMM_D_HASH), REPO_ID, COMM_D_HASH, true, true,
			"aD", 1600040001, "cD", 1600040002, "D",
			List.of(COMM_C_HASH), List.of(COMM_E_HASH, COMM_F_HASH, COMM_H_HASH));
		checkFullCommit(commitMap.get(COMM_E_HASH), REPO_ID, COMM_E_HASH, true, true,
			"aE", 1600050001, "cE", 1600050002, "E",
			List.of(COMM_D_HASH), List.of());
		checkFullCommit(commitMap.get(COMM_F_HASH), REPO_ID, COMM_F_HASH, true, false,
			"aF", 1600060001, "cF", 1600060002, "F",
			List.of(COMM_B_HASH, COMM_D_HASH), List.of(COMM_G_HASH, COMM_I_HASH));
		checkFullCommit(commitMap.get(COMM_G_HASH), REPO_ID, COMM_G_HASH, true, false,
			"aG", 1600070001, "cG", 1600070002, "G",
			List.of(COMM_F_HASH), List.of());
		checkFullCommit(commitMap.get(COMM_H_HASH), REPO_ID, COMM_H_HASH, false, false,
			"aH", 1600080001, "cH", 1600080002, "H",
			List.of(COMM_D_HASH), List.of());
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

	@Test
	void getTrackedCommitsBetween() {
		// Check that untracked and unreachable commits are not included

		List<Commit> commits = access.getTrackedCommitsBetween(REPO_ID,
			Instant.ofEpochSecond(1600030000), Instant.ofEpochSecond(1600080005));
		assertThat(commits.stream()
			.map(Commit::getHash))
			.containsExactlyInAnyOrder(COMM_C_HASH, COMM_D_HASH, COMM_E_HASH);

		// Check off-by-one errors and that committer and not author time is used

		commits = access.getTrackedCommitsBetween(REPO_ID,
			Instant.ofEpochSecond(1600040003), Instant.ofEpochSecond(1600050002));
		assertThat(commits.stream()
			.map(Commit::getHash))
			.containsExactlyInAnyOrder(COMM_E_HASH);

		commits = access.getTrackedCommitsBetween(REPO_ID,
			Instant.ofEpochSecond(1600040002), Instant.ofEpochSecond(1600050001));
		assertThat(commits.stream()
			.map(Commit::getHash))
			.containsExactlyInAnyOrder(COMM_D_HASH);
	}

	@Test
	void getFirstParentOfBranch() {
		assertThat(access.getFirstParentsOfBranch(REPO_ID, BRANCH_T, COMM_A_HASH))
			.isEmpty();
		assertThat(access.getFirstParentsOfBranch(REPO_ID, BRANCH_T, COMM_B_HASH))
			.isEmpty();
		assertThat(access.getFirstParentsOfBranch(REPO_ID, BRANCH_T, COMM_C_HASH))
			.isEmpty();
		assertThat(access.getFirstParentsOfBranch(REPO_ID, BRANCH_T, COMM_D_HASH))
			.isEmpty();
		assertThat(access.getFirstParentsOfBranch(REPO_ID, BRANCH_T, COMM_E_HASH))
			.isEmpty();
		assertThat(access.getFirstParentsOfBranch(REPO_ID, BRANCH_T, COMM_F_HASH))
			.containsExactlyInAnyOrder(COMM_B_HASH, COMM_D_HASH);
		assertThat(access.getFirstParentsOfBranch(REPO_ID, BRANCH_T, COMM_G_HASH))
			.containsExactlyInAnyOrder(COMM_B_HASH, COMM_D_HASH);
		assertThat(access.getFirstParentsOfBranch(REPO_ID, BRANCH_T, COMM_H_HASH))
			.containsExactlyInAnyOrder(COMM_D_HASH);
		assertThat(access.getFirstParentsOfBranch(REPO_ID, BRANCH_T, COMM_I_HASH))
			.containsExactlyInAnyOrder(COMM_B_HASH, COMM_D_HASH);

		assertThat(access.getFirstParentsOfBranch(REPO_ID, BRANCH_U, COMM_A_HASH))
			.isEmpty();
		assertThat(access.getFirstParentsOfBranch(REPO_ID, BRANCH_U, COMM_B_HASH))
			.isEmpty();
		assertThat(access.getFirstParentsOfBranch(REPO_ID, BRANCH_U, COMM_C_HASH))
			.isEmpty();
		assertThat(access.getFirstParentsOfBranch(REPO_ID, BRANCH_U, COMM_D_HASH))
			.isEmpty();
		assertThat(access.getFirstParentsOfBranch(REPO_ID, BRANCH_U, COMM_E_HASH))
			.containsExactlyInAnyOrder(COMM_D_HASH);
		assertThat(access.getFirstParentsOfBranch(REPO_ID, BRANCH_U, COMM_F_HASH))
			.isEmpty();
		assertThat(access.getFirstParentsOfBranch(REPO_ID, BRANCH_U, COMM_G_HASH))
			.isEmpty();
		assertThat(access.getFirstParentsOfBranch(REPO_ID, BRANCH_U, COMM_H_HASH))
			.containsExactlyInAnyOrder(COMM_D_HASH);
		assertThat(access.getFirstParentsOfBranch(REPO_ID, BRANCH_U, COMM_I_HASH))
			.containsExactlyInAnyOrder(COMM_F_HASH);
	}

	@Test
	void getCommitsBetween() {
		// From an untracked branch

		List<Commit> commits = access.getCommitsBetween(REPO_ID, List.of(BranchName.fromName("U")),
			Instant.ofEpochSecond(1600020000), Instant.ofEpochSecond(1600060005));
		assertThat(commits.stream()
			.map(Commit::getHash))
			.containsExactlyInAnyOrder(COMM_B_HASH, COMM_C_HASH, COMM_D_HASH, COMM_F_HASH);

		commits = access.getCommitsBetween(REPO_ID, List.of(BranchName.fromName("U")),
			Instant.ofEpochSecond(1600020000), Instant.ofEpochSecond(1600090005));
		assertThat(commits.stream()
			.map(Commit::getHash))
			.containsExactlyInAnyOrder(COMM_B_HASH, COMM_C_HASH, COMM_D_HASH, COMM_F_HASH, COMM_G_HASH);

		// From a tracked branch

		commits = access.getCommitsBetween(REPO_ID, List.of(BranchName.fromName("T")),
			Instant.ofEpochSecond(1600020000), Instant.ofEpochSecond(1600040005));
		assertThat(commits.stream()
			.map(Commit::getHash))
			.containsExactlyInAnyOrder(COMM_B_HASH, COMM_C_HASH, COMM_D_HASH);

		commits = access.getCommitsBetween(REPO_ID, List.of(BranchName.fromName("T")),
			Instant.ofEpochSecond(1600020000), Instant.ofEpochSecond(1600090005));
		assertThat(commits.stream()
			.map(Commit::getHash))
			.containsExactlyInAnyOrder(COMM_B_HASH, COMM_C_HASH, COMM_D_HASH, COMM_E_HASH);

		// From both branches

		commits = access.getCommitsBetween(REPO_ID,
			List.of(BranchName.fromName("T"), BranchName.fromName("U")),
			Instant.ofEpochSecond(1600020000), Instant.ofEpochSecond(1600060005));
		assertThat(commits.stream()
			.map(Commit::getHash))
			.containsExactlyInAnyOrder(COMM_B_HASH, COMM_C_HASH, COMM_D_HASH, COMM_E_HASH, COMM_F_HASH);

		commits = access.getCommitsBetween(REPO_ID,
			List.of(BranchName.fromName("T"), BranchName.fromName("U")),
			Instant.ofEpochSecond(1600020000), Instant.ofEpochSecond(1600090005));
		assertThat(commits.stream()
			.map(Commit::getHash))
			.containsExactlyInAnyOrder(COMM_B_HASH, COMM_C_HASH, COMM_D_HASH, COMM_E_HASH, COMM_F_HASH,
				COMM_G_HASH);

		// Check off-by-one errors and that committer and not author time is used

		commits = access.getCommitsBetween(REPO_ID, List.of(BranchName.fromName("T")),
			Instant.ofEpochSecond(1600040002), Instant.ofEpochSecond(1600050001));
		assertThat(commits.stream()
			.map(Commit::getHash))
			.containsExactlyInAnyOrder(COMM_D_HASH);

		commits = access.getCommitsBetween(REPO_ID, List.of(BranchName.fromName("T")),
			Instant.ofEpochSecond(1600040001), Instant.ofEpochSecond(1600050002));
		assertThat(commits.stream()
			.map(Commit::getHash))
			.containsExactlyInAnyOrder(COMM_D_HASH, COMM_E_HASH);

		commits = access.getCommitsBetween(REPO_ID, List.of(BranchName.fromName("T")),
			Instant.ofEpochSecond(1600040003), Instant.ofEpochSecond(1600050000));
		assertThat(commits).isEmpty();
	}
}
