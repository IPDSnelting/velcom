package de.aaaaaaah.velcom.backend.access.repoaccess;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jooq.codegen.db.tables.KnownCommit.KNOWN_COMMIT;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import de.aaaaaaah.velcom.backend.TestDb;
import de.aaaaaaah.velcom.backend.access.caches.AvailableDimensionsCache;
import de.aaaaaaah.velcom.backend.access.caches.LatestRunCache;
import de.aaaaaaah.velcom.backend.access.caches.RunCache;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Branch;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RemoteUrl;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.repoaccess.exceptions.NoSuchRepoException;
import de.aaaaaaah.velcom.backend.storage.db.DBReadAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.jooq.Result;
import org.jooq.codegen.db.tables.records.KnownCommitRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepoWriteAccessTest {

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
			BranchName.fromName("main"),
			new CommitHash("f3f7d9ed781a3a3dd51c7854c0fb4eb1161eab1a"),
			true
		),
		new Branch(
			REPO2_ID,
			BranchName.fromName("test"),
			new CommitHash("018ec13f2c2d2a711c44a4f8b7ff0f29050e6234"),
			false
		)
	);

	private static final RepoId REPO3_ID = new RepoId();
	private static final String REPO3_NAME = "olbof";
	private static final RemoteUrl REPO3_URL =
		new RemoteUrl("https://github.com/IPDSnelting/volcem.git");
	private static final List<Branch> REPO3_BRANCHES = List.of();

	private DatabaseStorage databaseStorage;
	private AvailableDimensionsCache availableDimensionsCache;
	private RunCache runCache;
	private LatestRunCache latestRunCache;
	private RepoWriteAccess access;

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

		databaseStorage = new DatabaseStorage(testDb.closeAndGetJdbcUrl());
		availableDimensionsCache = mock(AvailableDimensionsCache.class);
		runCache = mock(RunCache.class);
		latestRunCache = mock(LatestRunCache.class);
		access = new RepoWriteAccess(databaseStorage, availableDimensionsCache, runCache,
			latestRunCache);
	}

	@Test
	void addRepo() {
		Repo newRepo = access.addRepo("new repo", new RemoteUrl("https://foo.bar/xyz.git"));

		assertThat(newRepo.getName()).isEqualTo("new repo");
		assertThat(newRepo.getRemoteUrl()).isEqualTo(new RemoteUrl("https://foo.bar/xyz.git"));

		assertThat(access.getAllRepos().stream()
			.map(Repo::getId)
			.collect(toList()))
			.containsExactlyInAnyOrder(REPO1_ID, REPO2_ID, REPO3_ID, newRepo.getId());

		Repo newRepo2 = access.getRepo(newRepo.getId());

		assertThat(newRepo2.getId()).isEqualTo(newRepo.getId());
		assertThat(newRepo2.getName()).isEqualTo(newRepo.getName());
		assertThat(newRepo2.getRemoteUrl()).isEqualTo(newRepo.getRemoteUrl());
	}

	@Test
	void deleteRepo() {
		access.deleteRepo(REPO2_ID);

		verify(availableDimensionsCache, atLeastOnce()).invalidate(REPO2_ID);
		verify(runCache, atLeastOnce()).invalidateAll();
		verify(latestRunCache, atLeastOnce()).invalidate(REPO2_ID);

		assertThat(access.getAllRepos().stream().map(Repo::getId))
			.containsExactlyInAnyOrder(REPO1_ID, REPO3_ID);

		assertThatThrownBy(() -> access.getRepo(REPO2_ID))
			.isInstanceOf(NoSuchRepoException.class)
			.extracting("invalidId")
			.isEqualTo(REPO2_ID);

		assertThat(access.getAllBranches(REPO2_ID)).isEmpty();

		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			Result<KnownCommitRecord> repo2Commits = db.selectFrom(KNOWN_COMMIT)
				.where(KNOWN_COMMIT.REPO_ID.eq(REPO2_ID.getIdAsString()))
				.fetch();

			assertThat(repo2Commits).isEmpty();
		}
	}

	@Test
	void updateBranches() {
		access.setTrackedBranches(REPO1_ID, Set.of(BranchName.fromName("test")));

		List<Branch> repo1Branches = access.getAllBranches(REPO1_ID);
		assertThat(repo1Branches.stream()
			.map(Branch::getName)
			.map(BranchName::getName))
			.containsExactlyInAnyOrder("main", "test");
		assertThat(repo1Branches.stream()
			.filter(Branch::isTracked)
			.map(Branch::getName)
			.map(BranchName::getName))
			.containsExactlyInAnyOrder("test");

		// Has it modified other branches on accident?

		List<Branch> repo2Branches = access.getAllBranches(REPO2_ID);
		assertThat(repo2Branches.stream()
			.map(Branch::getName)
			.map(BranchName::getName))
			.containsExactlyInAnyOrder("main", "test");
		assertThat(repo2Branches.stream()
			.filter(Branch::isTracked)
			.map(Branch::getName)
			.map(BranchName::getName))
			.containsExactlyInAnyOrder("main");

		assertThat(access.getAllBranches(REPO3_ID)).isEmpty();
	}

	@Test
	void updateRepo() {
		// Setting nothing changes nothing
		access.updateRepo(REPO1_ID, null, null);
		Repo repo1 = access.getRepo(REPO1_ID);
		assertThat(repo1.getName()).isEqualTo(REPO1_NAME);
		assertThat(repo1.getRemoteUrl()).isEqualTo(REPO1_URL);

		// Setting name
		access.updateRepo(REPO1_ID, "new name", null);
		repo1 = access.getRepo(REPO1_ID);
		assertThat(repo1.getName()).isEqualTo("new name");
		assertThat(repo1.getRemoteUrl()).isEqualTo(REPO1_URL);

		// Setting remote url
		access.updateRepo(REPO1_ID, null, new RemoteUrl("https://flab.argle/"));
		repo1 = access.getRepo(REPO1_ID);
		assertThat(repo1.getName()).isEqualTo("new name");
		assertThat(repo1.getRemoteUrl()).isEqualTo(new RemoteUrl("https://flab.argle/"));

		// Setting both at the same time
		access.updateRepo(REPO1_ID, "even newer name", new RemoteUrl("https://bong.le"));
		repo1 = access.getRepo(REPO1_ID);
		assertThat(repo1.getName()).isEqualTo("even newer name");
		assertThat(repo1.getRemoteUrl()).isEqualTo(new RemoteUrl("https://bong.le"));

		// Other repos have not been changed

		Repo repo2 = access.getRepo(REPO2_ID);
		assertThat(repo2.getName()).isEqualTo(REPO2_NAME);
		assertThat(repo2.getRemoteUrl()).isEqualTo(REPO2_URL);

		Repo repo3 = access.getRepo(REPO3_ID);
		assertThat(repo3.getName()).isEqualTo(REPO3_NAME);
		assertThat(repo3.getRemoteUrl()).isEqualTo(REPO3_URL);
	}
}
