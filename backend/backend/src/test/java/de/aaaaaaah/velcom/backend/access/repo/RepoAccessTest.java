package de.aaaaaaah.velcom.backend.access.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.aaaaaaah.velcom.backend.TestCommit;
import de.aaaaaaah.velcom.backend.TestRepo;
import de.aaaaaaah.velcom.backend.access.AccessLayer;
import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.exception.NoSuchRepoException;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileSnapshot;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class RepoAccessTest {

	static final List<TestCommit> commits = List.of(
		new TestCommit("Add abc", "abc.txt", "Hello World!"),
		new TestCommit("Add blablub", "blablub.txt", "123123123"),
		new TestCommit("Modify abc", "abc.txt", "Hello World! Test 123", "otherbranch"),
		new TestCommit("Work", "workfile.txt", "skgofg")
	);
	static final TestCommit headTestCommit = new TestCommit("Last commit");

	@TempDir
	Path testDir;
	Path repoStorageDir;
	Path testRepoDir;
	Path secondTestRepoDir;
	Path dbDir;
	String cloneUrl;
	String secondCloneUrl;
	String dbUrl;

	TestRepo testRepo;
	TestRepo secondTestRepo;

	RepoStorage repoStorage;
	DatabaseStorage databaseStorage;
	RepoAccess repoAccess;

	RevCommit testRepoHeadRevCommit;

	@BeforeEach
	void setUp() throws IOException, GitAPIException, SQLException {
		((Logger) LoggerFactory.getLogger(FileSnapshot.class)).setLevel(Level.OFF); // Too much spam

		this.repoStorageDir = testDir.resolve("repos");
		this.testRepoDir = testDir.resolve("custom_test_repo");
		this.secondTestRepoDir = testDir.resolve("second_test_repo");
		this.dbDir = testDir.resolve("data.db");
		this.cloneUrl = "file://" + testRepoDir.toString();
		this.secondCloneUrl = "file://" + secondTestRepoDir.toString();
		this.dbUrl = "jdbc:sqlite:file:" + dbDir.toString();

		testRepo = new TestRepo(testRepoDir);
		for (TestCommit commit : commits) {
			testRepo.commit(commit);
		}
		testRepoHeadRevCommit = testRepo.commit(headTestCommit);

		secondTestRepo = new TestRepo(secondTestRepoDir);
		secondTestRepo.commit("bla", "e.txt", "abc");

		this.repoStorage = new RepoStorage(repoStorageDir);
		this.databaseStorage = new DatabaseStorage(dbUrl);

		AccessLayer accessLayer = new AccessLayer();

		RemoteUrl benchRepoUrl = new RemoteUrl(cloneUrl);
		repoAccess = new RepoAccess(accessLayer, databaseStorage, repoStorage, benchRepoUrl);
	}

	@AfterEach
	void breakDown() {
		if (databaseStorage != null) {
			databaseStorage.close();
		}
	}

	@Test
	void testAddRepo() {
		final Repo repo = repoAccess.addRepo("my_repo", new RemoteUrl(cloneUrl));
		assertNotNull(repo);
		assertEquals("my_repo", repo.getName());
		assertEquals(2, repo.getBranches().size());
		assertEquals(1, repo.getTrackedBranches().size());
		assertEquals(repo.getRemoteUrl().getUrl(), cloneUrl);

		final List<String> branchNames = repo.getBranches()
			.stream()
			.map(Branch::getName)
			.map(BranchName::getName)
			.collect(Collectors.toList());

		assertTrue(branchNames.contains("master"));
		assertTrue(branchNames.contains("otherbranch"));
		assertTrue(repoStorage.containsRepository(repo.getId().getDirectoryName()));
	}

	@Test
	void testGetRepo() {
		assertThrows(NoSuchRepoException.class, () -> repoAccess.getRepo(new RepoId()));

		final Repo addedRepo = repoAccess.addRepo("my_repo", new RemoteUrl(cloneUrl));
		assertNotNull(addedRepo);
		assertNotNull(addedRepo.getId());

		final Repo receivedRepo = repoAccess.getRepo(addedRepo.getId());
		assertNotNull(receivedRepo);
		assertEquals(addedRepo, receivedRepo);
		assertEquals(addedRepo.getId(), receivedRepo.getId());
		assertEquals(addedRepo.getName(), receivedRepo.getName());
		assertEquals(addedRepo.getRemoteUrl(), receivedRepo.getRemoteUrl());
		assertEquals(addedRepo.getBranches(), receivedRepo.getBranches());
		assertEquals(addedRepo.getTrackedBranches(), receivedRepo.getTrackedBranches());

		repoAccess.deleteRepo(addedRepo.getId());
		assertThrows(NoSuchRepoException.class, () -> repoAccess.getRepo(addedRepo.getId()));
	}

	@Test
	void testDeleteRepo() {
		final Repo repo = repoAccess.addRepo("my_repo", new RemoteUrl(cloneUrl));

		assertNotNull(repoAccess.getRepo(repo.getId()));
		assertTrue(repoStorage.containsRepository(repo.getId().getDirectoryName()));

		repoAccess.deleteRepo(repo.getId());

		assertThrows(NoSuchRepoException.class, () -> repoAccess.getRepo(repo.getId()));
		assertFalse(repoStorage.containsRepository(repo.getId().getDirectoryName()));
	}

	@Test
	void testClone() throws IOException {
		final Repo repo = repoAccess.addRepo("my_repo", new RemoteUrl(cloneUrl));

		repoStorage.deleteRepository(repo.getId().getDirectoryName());
		assertFalse(repoStorage.containsRepository(repo.getId().getDirectoryName()));

		// fetchOrClone() should clone repo since it is missing in storage
		repoAccess.fetchOrClone(repo.getId());
		assertTrue(repoStorage.containsRepository(repo.getId().getDirectoryName()));
	}

	@Test
	void testFetch() throws IOException, GitAPIException {
		final Repo repo = repoAccess.addRepo("my_repo", new RemoteUrl(cloneUrl));

		final Branch branch = repo.getTrackedBranches().iterator().next();
		assertEquals("master", branch.getName().getName());

		final CommitHash lastCommitHash = repoAccess.getLatestCommitHash(branch);

		// Create a new commit
		final RevCommit revCommit = testRepo.commit("msg", "a.txt", "abc");

		repoAccess.fetchOrClone(repo.getId());

		final CommitHash newCommitHash = repoAccess.getLatestCommitHash(branch);
		assertNotEquals(lastCommitHash, newCommitHash);
		assertEquals(revCommit.getId().getName(), newCommitHash.getHash());
	}

	@Test
	void testBenchmarkRepo() throws IOException, GitAPIException {
		final CommitHash lastCommitHash = repoAccess.getLatestBenchmarkRepoHash();

		assertEquals(testRepoHeadRevCommit.getId().getName(), lastCommitHash.getHash());

		// Create a new commit
		final RevCommit revCommit = testRepo.commit("msg", "a.txt", "abc");

		repoAccess.updateBenchmarkRepo();

		final CommitHash newCommitHash = repoAccess.getLatestBenchmarkRepoHash();
		assertNotEquals(lastCommitHash, newCommitHash);
		assertEquals(revCommit.getId().getName(), newCommitHash.getHash());
	}

	@Test
	void testSetName() {
		final Repo repo = repoAccess.addRepo("my_repo", new RemoteUrl(cloneUrl));
		assertEquals("my_repo", repoAccess.getName(repo.getId()));

		repoAccess.setName(repo.getId(), "new_name");
		assertEquals("new_name", repoAccess.getName(repo.getId()));
	}

	@Test
	void testSetRemoteUrl() throws IOException, GitAPIException {
		assertThrows(NoSuchRepoException.class,
			() -> repoAccess.setRemoteUrl(new RepoId(), new RemoteUrl("abc")));

		final Repo repo = repoAccess.addRepo("my_repo", new RemoteUrl(cloneUrl));
		assertEquals(cloneUrl, repoAccess.getRemoteUrl(repo.getId()).getUrl());

		final Branch branch = repo.getTrackedBranches().iterator().next();
		assertEquals("master", branch.getName().getName());

		final CommitHash firstMasterHash = repoAccess.getLatestCommitHash(branch);

		// Create a commit on testRepo so that if repoAccess were to fetch from it, we would notice
		testRepo.commit("oh", "no.txt", "abc");

		// Nothing should have changed
		final CommitHash secondMasterHash = repoAccess.getLatestCommitHash(branch);
		assertEquals(firstMasterHash, secondMasterHash);

		// set remote url to the same url again so nothing should have changed
		repoAccess.setRemoteUrl(repo.getId(), new RemoteUrl(cloneUrl));

		final CommitHash thirdMasterHash = repoAccess.getLatestCommitHash(branch);
		assertEquals(firstMasterHash, thirdMasterHash);

		// Make a commit on the second repo (we'll need that later)
		final RevCommit commit = secondTestRepo.commit("a", "b.txt", "c");

		// Actually change the remote url now, so repoAccess needs to delete the local
		// repo and clone the new one
		repoAccess.setRemoteUrl(repo.getId(), new RemoteUrl(secondCloneUrl));
		assertEquals(secondCloneUrl, repoAccess.getRemoteUrl(repo.getId()).getUrl());

		// Now the repo should have a new master commit
		final CommitHash fourthMasterHash = repoAccess.getLatestCommitHash(branch);
		assertEquals(commit.getId().getName(), fourthMasterHash.getHash());
	}

	@Test
	void testIsBranchTracked() {
		final Repo repo = repoAccess.addRepo("my_repo", new RemoteUrl(cloneUrl));

		final Branch master = repo.getBranches()
			.stream()
			.filter(b -> b.getName().getName().equals("master"))
			.findAny()
			.orElseThrow();

		final Branch otherBranch = repo.getBranches()
			.stream()
			.filter(b -> b.getName().getName().equals("otherbranch"))
			.findAny()
			.orElseThrow();

		assertTrue(repoAccess.isBranchTracked(repo.getId(), master.getName()));
		assertFalse(repoAccess.isBranchTracked(repo.getId(), otherBranch.getName()));
	}

	@Test
	void testSetTrackedBranches() {
		final Repo repo = repoAccess.addRepo("my_repo", new RemoteUrl(cloneUrl));

		final Branch master = repo.getBranches()
			.stream()
			.filter(b -> b.getName().getName().equals("master"))
			.findAny()
			.orElseThrow();

		final Branch otherBranch = repo.getBranches()
			.stream()
			.filter(b -> b.getName().getName().equals("otherbranch"))
			.findAny()
			.orElseThrow();

		List<BranchName> bothBranches = List.of(master.getName(), otherBranch.getName());

		assertEquals(List.of(master), repoAccess.getTrackedBranches(repo.getId()));

		repoAccess.setTrackedBranches(repo.getId(), bothBranches);

		assertEquals(List.of(master, otherBranch), repoAccess.getTrackedBranches(repo.getId()));
	}

	@Test
	void testGetAllRepos() {
		assertTrue(repoAccess.getAllRepos().isEmpty());

		final Repo repo = repoAccess.addRepo("my_repo", new RemoteUrl(cloneUrl));

		assertEquals(List.of(repo), repoAccess.getAllRepos());

		final Repo otherRepo = repoAccess.addRepo("o_repo", new RemoteUrl(secondCloneUrl));

		assertEquals(List.of(repo, otherRepo), repoAccess.getAllRepos());

		repoAccess.deleteRepo(repo.getId());

		assertEquals(List.of(otherRepo), repoAccess.getAllRepos());

		repoAccess.deleteRepo(otherRepo.getId());

		assertTrue(repoAccess.getAllRepos().isEmpty());
	}

}