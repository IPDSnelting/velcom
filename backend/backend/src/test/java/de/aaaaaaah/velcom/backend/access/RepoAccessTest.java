package de.aaaaaaah.velcom.backend.access;

import static java.io.OutputStream.nullOutputStream;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.aaaaaaah.velcom.backend.TestCommit;
import de.aaaaaaah.velcom.backend.TestRepo;
import de.aaaaaaah.velcom.backend.access.archive.ArchiveException;
import de.aaaaaaah.velcom.backend.access.entities.Branch;
import de.aaaaaaah.velcom.backend.access.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RemoteUrl;
import de.aaaaaaah.velcom.backend.access.entities.Repo;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.exceptions.NoSuchRepoException;
import de.aaaaaaah.velcom.backend.access.exceptions.RepoAccessException;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileSnapshot;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;

public class RepoAccessTest {

	private static final String FIRST_NAME = "first_repo";
	private static final String SECOND_NAME = "second_repo";

	private static final TestCommit[] FIRST_REPO_COMMITS = {
		new TestCommit("Add abc"),
		new TestCommit("Add bla", "otherbranch"),
		new TestCommit("Modify abc"),
	};

	private static final TestCommit[] SECOND_REPO_COMMITS = {
		new TestCommit("Initial commit"),
		new TestCommit("Add bla", "anotherbranch"),
		new TestCommit("Modify abc", "thirdbranch"),
		new TestCommit("another commit back on master")
	};

	@TempDir
	Path testDir;

	Path dbPath;
	Path repoStoragePath;
	Path firstRemoteRepoPath;
	Path secondRemoteRepoPath;

	RemoteUrl firstRemoteUrl;
	RemoteUrl secondRemoteUrl;

	TestRepo firstRemoteRepo;
	TestRepo secondRemoteRepo;

	RepoStorage repoStorage;
	DatabaseStorage dbStorage;

	RepoWriteAccess repoAccess;

	@BeforeEach
	void setUp() throws GitAPIException, IOException, SQLException {
		((Logger) LoggerFactory.getLogger(FileSnapshot.class)).setLevel(Level.OFF); // Too much spam

		dbPath = testDir.resolve("data").resolve("data.db");
		repoStoragePath = testDir.resolve("data").resolve("repos");
		firstRemoteRepoPath = testDir.resolve("first_remote_repo");
		secondRemoteRepoPath = testDir.resolve("second_remote_repo");

		firstRemoteUrl = new RemoteUrl("file://" + firstRemoteRepoPath);
		secondRemoteUrl = new RemoteUrl("file://" + secondRemoteRepoPath);

		firstRemoteRepo = new TestRepo(firstRemoteRepoPath, FIRST_REPO_COMMITS);
		secondRemoteRepo = new TestRepo(secondRemoteRepoPath, SECOND_REPO_COMMITS);

		repoStorage = new RepoStorage(repoStoragePath);
		dbStorage = new DatabaseStorage("jdbc:sqlite:file:" + dbPath);

		repoAccess = new RepoWriteAccess(
			dbStorage, repoStorage, firstRemoteUrl, testDir.resolve("archives_unused")
		);
	}

	@AfterEach
	void breakDown() {
		if (dbStorage != null) {
			dbStorage.close();
		}
	}

	@Test
	public void testAddRepo() {
		Repo repo = repoAccess.addRepo(FIRST_NAME, firstRemoteUrl);
		assertEquals(FIRST_NAME, repo.getName());
		assertEquals(firstRemoteUrl, repo.getRemoteUrl());

		Collection<Branch> trackedBranches = repo.getTrackedBranches();
		assertEquals(1, trackedBranches.size());
		assertEquals("master", trackedBranches.iterator().next().getName().getName());
		assertEquals(repo.getRepoId(), trackedBranches.iterator().next().getRepoId());
	}

	@Test
	public void testGetRepo() {
		Repo repo = repoAccess.addRepo(FIRST_NAME, firstRemoteUrl);
		Repo sameRepo = repoAccess.getRepo(repo.getRepoId());

		assertEquals(repo.getName(), sameRepo.getName());
		assertEquals(repo.getRemoteUrl(), sameRepo.getRemoteUrl());
		assertEquals(repo.getRepoId(), sameRepo.getRepoId());
		assertEquals(repo.getTrackedBranches(), sameRepo.getTrackedBranches());
	}

	@Test
	public void testGetAllRepos() {
		Repo firstRepo = repoAccess.addRepo(FIRST_NAME, firstRemoteUrl);
		Repo secondRepo = repoAccess.addRepo(SECOND_NAME, secondRemoteUrl);

		Collection<Repo> allRepos = repoAccess.getAllRepos();
		assertEquals(2, allRepos.size());
		assertTrue(allRepos.contains(firstRepo));
		assertTrue(allRepos.contains(secondRepo));
	}

	@Test
	public void testGetAllRepoIds() {
		Repo firstRepo = repoAccess.addRepo(FIRST_NAME, firstRemoteUrl);
		Repo secondRepo = repoAccess.addRepo(SECOND_NAME, secondRemoteUrl);

		Collection<RepoId> allRepoIds = repoAccess.getAllRepoIds();
		assertEquals(2, allRepoIds.size());
		assertTrue(allRepoIds.contains(firstRepo.getRepoId()));
		assertTrue(allRepoIds.contains(secondRepo.getRepoId()));
	}

	@ParameterizedTest
	@CsvSource({
		"master",
		"anotherbranch",
		"thirdbranch",
		"master/anotherbranch",
		"anotherbranch/thirdbranch",
		"master/anotherbranch/thirdbranch"
	})
	public void testTrackedBranches(String branchNameList) {
		String[] branchNameArray = branchNameList.split("/");
		List<BranchName> targetBranchNames = Arrays.stream(branchNameArray)
			.map(BranchName::fromName)
			.collect(toList());

		Repo repo = repoAccess.addRepo(SECOND_NAME, secondRemoteUrl);

		// 1.) Before doing anything, check if the initial results are actually correct
		Collection<Branch> firstResult = repoAccess.getTrackedBranches(repo.getRepoId());
		assertEquals(1, firstResult.size());
		assertEquals("master", firstResult.iterator().next().getName().getName());
		assertEquals(repo.getRepoId(), firstResult.iterator().next().getRepoId());

		// 2.) Now change the tracked branches and see what happens
		repoAccess.setTrackedBranches(repo.getRepoId(), targetBranchNames);

		// Check getter on repo instance (should still be old result since Repo is immutable)
		Collection<Branch> secondResult = repo.getTrackedBranches();
		assertThat(secondResult).containsExactlyInAnyOrderElementsOf(firstResult);

		// Check new tracked branches collection
		Collection<Branch> thirdResult = repoAccess.getTrackedBranches(repo.getRepoId());

		List<BranchName> thirdResultBranchNames = thirdResult.stream()
			.map(Branch::getName).collect(toList());

		assertThat(thirdResultBranchNames).containsExactlyInAnyOrderElementsOf(targetBranchNames);

		// Check that new repo instances also reflect this behaviour
		Repo sameRepo = repoAccess.getRepo(repo.getRepoId());
		assertThat(sameRepo.getTrackedBranches()).containsExactlyInAnyOrderElementsOf(thirdResult);
	}

	@Test
	public void testGetBranches() throws IOException, GitAPIException {
		Repo repo = repoAccess.addRepo(SECOND_NAME, secondRemoteUrl);

		Collection<String> branches = repoAccess.getBranches(repo.getRepoId()).stream()
			.map(Branch::getName)
			.map(BranchName::getName)
			.collect(toList());

		assertThat(branches).containsExactlyInAnyOrder("master", "anotherbranch", "thirdbranch");

		// Make a new commit and update that one
		secondRemoteRepo.commit(new TestCommit("new commit", "fourthbranch"));

		// Update local repo
		repoAccess.updateRepo(repo.getRepoId());

		// Now check again if the new branch appeared
		branches = repoAccess.getBranches(repo.getRepoId()).stream()
			.map(Branch::getName)
			.map(BranchName::getName)
			.collect(toList());

		assertThat(branches).containsExactlyInAnyOrder(
			"master", "anotherbranch", "thirdbranch", "fourthbranch"
		);
	}

	@Test
	public void testGetLatestCommitHash() throws IOException, GitAPIException {
		Repo repo = repoAccess.addRepo(SECOND_NAME, secondRemoteUrl);

		Branch branch = new Branch(repo.getRepoId(), BranchName.fromName("master"));
		CommitHash branchHash = repoAccess.getLatestCommitHash(branch);
		String actualHash = secondRemoteRepo.getCommitHash(SECOND_REPO_COMMITS[3]).orElseThrow();
		assertEquals(actualHash, branchHash.getHash());

		branch = new Branch(repo.getRepoId(), BranchName.fromName("thirdbranch"));
		branchHash = repoAccess.getLatestCommitHash(branch);
		actualHash = secondRemoteRepo.getCommitHash(SECOND_REPO_COMMITS[2]).orElseThrow();
		assertEquals(actualHash, branchHash.getHash());

		branch = new Branch(repo.getRepoId(), BranchName.fromName("anotherbranch"));
		branchHash = repoAccess.getLatestCommitHash(branch);
		actualHash = secondRemoteRepo.getCommitHash(SECOND_REPO_COMMITS[1]).orElseThrow();
		assertEquals(actualHash, branchHash.getHash());

		// Make a new commit
		RevCommit revCommit = secondRemoteRepo.commit(
			new TestCommit("bla bla", "anotherbranch")
		);

		actualHash = revCommit.getId().getName();

		repoAccess.updateRepo(repo.getRepoId());

		branchHash = repoAccess.getLatestCommitHash(branch);
		assertEquals(actualHash, branchHash.getHash());
	}

	@Test
	public void testGetRemoteUrl() {
		Repo firstRepo = repoAccess.addRepo(FIRST_NAME, firstRemoteUrl);
		Repo secondRepo = repoAccess.addRepo(SECOND_NAME, secondRemoteUrl);

		assertEquals(firstRemoteUrl, repoAccess.getRemoteUrl(firstRepo.getRepoId()));
		assertEquals(secondRemoteUrl, repoAccess.getRemoteUrl(secondRepo.getRepoId()));
	}

	@Test
	public void testSetName() {
		Repo repo = repoAccess.addRepo(FIRST_NAME, firstRemoteUrl);
		assertEquals(repo.getName(), FIRST_NAME);

		repoAccess.setName(repo.getRepoId(), "newName");
		assertEquals(repo.getName(), FIRST_NAME); // repo instance is immutable

		repo = repoAccess.getRepo(repo.getRepoId());
		assertEquals(repo.getName(), "newName");
	}

	@Test
	public void testSetRemoteUrl() {
		Repo repo = repoAccess.addRepo(FIRST_NAME, firstRemoteUrl);
		assertEquals(repo.getRemoteUrl(), firstRemoteUrl);

		Branch branch = new Branch(repo.getRepoId(), BranchName.fromName("master"));
		CommitHash hashBefore = repoAccess.getLatestCommitHash(branch);

		repoAccess.setRemoteUrl(repo.getRepoId(), secondRemoteUrl);
		assertEquals(repo.getRemoteUrl(), firstRemoteUrl); // repo instance is immutable

		repo = repoAccess.getRepo(repo.getRepoId());
		assertEquals(repo.getRemoteUrl(), secondRemoteUrl);

		// Make sure that latest commit hash has changed
		CommitHash hashAfter = repoAccess.getLatestCommitHash(branch);
		assertNotEquals(hashBefore, hashAfter);

		TestCommit latestMasterCommit = SECOND_REPO_COMMITS[SECOND_REPO_COMMITS.length - 1];
		String actualHash = secondRemoteRepo.getCommitHash(latestMasterCommit).orElseThrow();
		assertEquals(actualHash, hashAfter.getHash());
	}

	@Test
	public void testDeleteRepo() {
		Repo firstRepo = repoAccess.addRepo(FIRST_NAME, firstRemoteUrl);
		Repo secondRepo = repoAccess.addRepo(SECOND_NAME, secondRemoteUrl);

		RepoId id = firstRepo.getRepoId();
		BranchName bName = BranchName.fromName("master");
		Branch branch = new Branch(firstRepo.getRepoId(), bName);
		CommitHash latestHash = repoAccess.getLatestCommitHash(branch);

		assertThat(repoAccess.getAllRepos()).containsExactlyInAnyOrder(
			firstRepo, secondRepo
		);
		assertThat(repoAccess.getAllRepoIds()).containsExactlyInAnyOrder(
			firstRepo.getRepoId(), secondRepo.getRepoId()
		);

		repoAccess.deleteRepo(firstRepo.getRepoId());

		assertThat(repoAccess.getAllRepos()).containsExactlyInAnyOrder(
			secondRepo
		);
		assertThat(repoAccess.getAllRepoIds()).containsExactlyInAnyOrder(
			secondRepo.getRepoId()
		);

		repoAccess.getRepo(secondRepo.getRepoId());

		assertThrows(NoSuchRepoException.class,
			() -> repoAccess.getRepo(id));
		assertTrue(repoAccess.getTrackedBranches(id).isEmpty());
		assertThrows(RepoAccessException.class,
			() -> repoAccess.getBranches(id));
		assertThrows(RepoAccessException.class,
			() -> repoAccess.getLatestCommitHash(branch));
		assertThrows(NoSuchRepoException.class,
			() -> repoAccess.getRemoteUrl(id));
		//assertThrows(NoSuchRepoException.class,
		//	() -> repoAccess.setName(id, "some_name"));
		assertThrows(NoSuchRepoException.class,
			() -> repoAccess.setRemoteUrl(id, secondRemoteUrl));
		//assertThrows(NoSuchRepoException.class,
		//	() -> repoAccess.setTrackedBranches(id, List.of(bName)));
		assertThrows(NoSuchRepoException.class,
			() -> repoAccess.updateRepo(id));
		assertThrows(ArchiveException.class,
			() -> repoAccess.streamNormalRepoArchive(id, latestHash, nullOutputStream()));
	}

	@Test
	public void testUpdateRepo() throws IOException, GitAPIException {
		Repo repo = repoAccess.addRepo(FIRST_NAME, firstRemoteUrl);
		Branch master = new Branch(repo.getRepoId(), BranchName.fromName("master"));

		// Create a commit
		String secondHash = firstRemoteRepo.commit(new TestCommit("bla")).getId().getName();

		// Delete local directory and let it re-clone it completely
		repoStorage.deleteRepository(repo.getRepoId().getDirectoryName());
		assertFalse(repoStorage.containsRepository(repo.getRepoId().getDirectoryName()));

		repoAccess.updateRepo(repo.getRepoId());
		assertTrue(repoStorage.containsRepository(repo.getRepoId().getDirectoryName()));
		String hashAfterReClone = repoAccess.getLatestCommitHash(master).getHash();

		assertEquals(secondHash, hashAfterReClone);

		// Now let's test default update behaviour
		String thirdHash = firstRemoteRepo.commit(new TestCommit("peng")).getId().getName();

		repoAccess.updateRepo(repo.getRepoId());

		String hashAfterFetch = repoAccess.getLatestCommitHash(master).getHash();
		assertEquals(thirdHash, hashAfterFetch);
	}

}
