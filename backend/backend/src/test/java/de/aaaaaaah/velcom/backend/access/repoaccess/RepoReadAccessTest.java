package de.aaaaaaah.velcom.backend.access.repoaccess;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.aaaaaaah.velcom.backend.TestDb;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Branch;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RemoteUrl;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.SearchBranchDescription;
import de.aaaaaaah.velcom.backend.access.repoaccess.exceptions.NoSuchRepoException;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepoReadAccessTest {

	private static final RepoId REPO1_ID = new RepoId();
	private static final String REPO1_NAME = "floob";
	private static final RemoteUrl REPO1_URL =
		new RemoteUrl("https://github.com/IPDSnelting/velcom.git");
	private static final List<Branch> REPO1_BRANCHES = List.of(
		new Branch(
			REPO1_ID,
			BranchName.fromName("main"),
			new CommitHash("07dcbfbee1c5c833614d00ce70c15621c939806c"),
			true
		),
		new Branch(
			REPO1_ID,
			BranchName.fromName("test"),
			new CommitHash("57b0e77894a2b7270ade8767b355ed8a283fffb0"),
			false
		)
	);

	private static final RepoId REPO2_ID = new RepoId();
	private static final String REPO2_NAME = "bloof";
	private static final RemoteUrl REPO2_URL =
		new RemoteUrl("https://github.com/IPDSnelting/comvel.git");
	private static final List<Branch> REPO2_BRANCHES = List.of(
		new Branch(
			REPO2_ID,
			BranchName.fromName("red"),
			new CommitHash("f3f7d9ed781a3a3dd51c7854c0fb4eb1161eab1a"),
			true
		),
		new Branch(
			REPO2_ID,
			BranchName.fromName("green"),
			new CommitHash("018ec13f2c2d2a711c44a4f8b7ff0f29050e6234"),
			true
		),
		new Branch(
			REPO2_ID,
			BranchName.fromName("blue"),
			new CommitHash("0b4f005028fb79d1e8f6c1f6e84dcf804fcf5103"),
			false
		)
	);

	private static final RepoId REPO3_ID = new RepoId();
	private static final String REPO3_NAME = "olbof";
	private static final RemoteUrl REPO3_URL =
		new RemoteUrl("https://github.com/IPDSnelting/volcem.git");
	private static final List<Branch> REPO3_BRANCHES = List.of();

	private RepoReadAccess access;

	@BeforeEach
	void setUp(@TempDir Path tempDir) {
		TestDb testDb = new TestDb(tempDir);

		// Insert repos
		testDb.addRepo(REPO1_ID, REPO1_NAME, REPO1_URL);
		testDb.addRepo(REPO2_ID, REPO2_NAME, REPO2_URL);
		testDb.addRepo(REPO3_ID, REPO3_NAME, REPO3_URL);

		// Insert dummy commits
		Stream.concat(REPO1_BRANCHES.stream(), REPO2_BRANCHES.stream())
			.forEach(branch -> testDb.addCommit(branch.getRepoId(), branch.getLatestCommitHash()));

		// Insert branches
		Stream.concat(REPO1_BRANCHES.stream(), REPO2_BRANCHES.stream())
			.forEach(branch -> testDb.addBranch(
				branch.getRepoId(),
				branch.getName(),
				branch.getLatestCommitHash(),
				branch.isTracked()
			));

		DatabaseStorage databaseStorage = new DatabaseStorage(testDb.closeAndGetJdbcUrl());
		access = new RepoReadAccess(databaseStorage);
	}

	@Test
	void getExistingAndNonexistentRepos() {
		Repo repo1 = access.getRepo(REPO1_ID);
		assertThat(repo1.getId()).isEqualTo(REPO1_ID);
		assertThat(repo1.getName()).isEqualTo(REPO1_NAME);
		assertThat(repo1.getRemoteUrl()).isEqualTo(REPO1_URL);

		Repo repo2 = access.getRepo(REPO2_ID);
		assertThat(repo2.getId()).isEqualTo(REPO2_ID);
		assertThat(repo2.getName()).isEqualTo(REPO2_NAME);
		assertThat(repo2.getRemoteUrl()).isEqualTo(REPO2_URL);

		Repo repo3 = access.getRepo(REPO3_ID);
		assertThat(repo3.getId()).isEqualTo(REPO3_ID);
		assertThat(repo3.getName()).isEqualTo(REPO3_NAME);
		assertThat(repo3.getRemoteUrl()).isEqualTo(REPO3_URL);

		assertThatCode(() -> access.guardRepoExists(REPO1_ID)).doesNotThrowAnyException();
		assertThatCode(() -> access.guardRepoExists(REPO2_ID)).doesNotThrowAnyException();
		assertThatCode(() -> access.guardRepoExists(REPO3_ID)).doesNotThrowAnyException();

		RepoId randomId = new RepoId();

		assertThatThrownBy(() -> access.getRepo(randomId))
			.isInstanceOf(NoSuchRepoException.class)
			.extracting("invalidId")
			.isEqualTo(randomId);

		assertThatThrownBy(() -> access.guardRepoExists(randomId))
			.isInstanceOf(NoSuchRepoException.class)
			.extracting("invalidId")
			.isEqualTo(randomId);
	}

	@Test
	void getAllRepos() {
		Map<RepoId, Repo> repos = access.getAllRepos().stream()
			.collect(toMap(
				Repo::getId,
				it -> it,
				(l, r) -> {
					throw new AssertionError("repo ids are unique");
				}
			));

		assertThat(repos).containsOnlyKeys(REPO1_ID, REPO2_ID, REPO3_ID);

		assertThat(repos.get(REPO1_ID).getId()).isEqualTo(REPO1_ID);
		assertThat(repos.get(REPO1_ID).getName()).isEqualTo(REPO1_NAME);
		assertThat(repos.get(REPO1_ID).getRemoteUrl()).isEqualTo(REPO1_URL);

		assertThat(repos.get(REPO2_ID).getId()).isEqualTo(REPO2_ID);
		assertThat(repos.get(REPO2_ID).getName()).isEqualTo(REPO2_NAME);
		assertThat(repos.get(REPO2_ID).getRemoteUrl()).isEqualTo(REPO2_URL);

		assertThat(repos.get(REPO3_ID).getId()).isEqualTo(REPO3_ID);
		assertThat(repos.get(REPO3_ID).getName()).isEqualTo(REPO3_NAME);
		assertThat(repos.get(REPO3_ID).getRemoteUrl()).isEqualTo(REPO3_URL);
	}

	@Test
	void getAllBranches() {
		assertThat(access.getAllBranches(REPO1_ID))
			.containsExactlyInAnyOrderElementsOf(REPO1_BRANCHES);

		assertThat(access.getAllBranches(REPO2_ID))
			.containsExactlyInAnyOrderElementsOf(REPO2_BRANCHES);

		assertThat(access.getAllBranches(REPO3_ID))
			.containsExactlyInAnyOrderElementsOf(REPO3_BRANCHES);

		assertThat(access.getAllBranches(new RepoId()))
			.isEmpty();
	}

	@Test
	void searchBranches() {
		assertThat(access.searchBranches(5, null, "n"))
			.containsExactlyInAnyOrder(
				new SearchBranchDescription(REPO1_ID, BranchName.fromName("main"),
					new CommitHash("07dcbfbee1c5c833614d00ce70c15621c939806c"), "message", false),
				new SearchBranchDescription(REPO2_ID, BranchName.fromName("green"),
					new CommitHash("018ec13f2c2d2a711c44a4f8b7ff0f29050e6234"), "message", false)
			);

		assertThat(access.searchBranches(5, REPO1_ID, "e"))
			.containsExactlyInAnyOrder(
				new SearchBranchDescription(REPO1_ID, BranchName.fromName("test"),
					new CommitHash("57b0e77894a2b7270ade8767b355ed8a283fffb0"), "message", false)
			);

		assertThat(access.searchBranches(2, null, "e"))
			.containsExactly(
				new SearchBranchDescription(REPO2_ID, BranchName.fromName("blue"),
					new CommitHash("0b4f005028fb79d1e8f6c1f6e84dcf804fcf5103"), "message", false),
				new SearchBranchDescription(REPO2_ID, BranchName.fromName("green"),
					new CommitHash("018ec13f2c2d2a711c44a4f8b7ff0f29050e6234"), "message", false)
			);
	}
}
