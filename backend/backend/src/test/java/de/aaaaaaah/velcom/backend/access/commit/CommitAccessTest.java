package de.aaaaaaah.velcom.backend.access.commit;

import static de.aaaaaaah.velcom.backend.access.commit.BenchmarkStatus.BENCHMARK_REQUIRED_MANUAL_PRIORITY;
import static de.aaaaaaah.velcom.backend.access.commit.BenchmarkStatus.NO_BENCHMARK_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.aaaaaaah.velcom.backend.TestCommit;
import de.aaaaaaah.velcom.backend.TestRepo;
import de.aaaaaaah.velcom.backend.access.AccessLayer;
import de.aaaaaaah.velcom.backend.access.repo.RemoteUrl;
import de.aaaaaaah.velcom.backend.access.repo.Repo;
import de.aaaaaaah.velcom.backend.access.repo.RepoAccess;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileSnapshot;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class CommitAccessTest {

	private static final List<TestCommit> COMMITS = List.of(
		new TestCommit("peng"),
		new TestCommit("bumm"),
		new TestCommit("abc", "file.txt", "contnt", "otherbranch")
	);

	@TempDir
	Path testDir;
	Path testRepoDir;
	Path repoStorageDir;
	Path dbPath;
	String cloneUrl;

	TestRepo testRepo;
	List<RevCommit> revCommits = new ArrayList<>();
	List<CommitHash> hashes = new ArrayList<>();

	RepoStorage repoStorage;
	DatabaseStorage dbStorage;

	RepoId repoId;
	Repo repo;
	RepoAccess repoAccess;
	CommitAccess commitAccess;

	@BeforeEach
	void setUp() throws GitAPIException, IOException, SQLException {
		((Logger) LoggerFactory.getLogger(FileSnapshot.class)).setLevel(Level.OFF); // Too much spam

		testRepoDir = testDir.resolve("test_repo");
		repoStorageDir = testDir.resolve("repos");
		dbPath = testDir.resolve("data.db");
		cloneUrl = "file://" + testRepoDir.toString();

		testRepo = new TestRepo(testRepoDir);
		for (TestCommit commit : COMMITS) {
			final RevCommit revCommit = testRepo.commit(commit);
			revCommits.add(revCommit);
			hashes.add(new CommitHash(revCommit.getId().getName()));
		}

		repoStorage = new RepoStorage(repoStorageDir);
		dbStorage = new DatabaseStorage("jdbc:sqlite:file:" + dbPath.toString());

		AccessLayer layer = new AccessLayer();
		repoAccess = new RepoAccess(layer, dbStorage, repoStorage, new RemoteUrl(cloneUrl));
		commitAccess = new CommitAccess(new AccessLayer(), dbStorage, repoStorage);
		layer.registerCommitAccess(commitAccess);
		layer.registerRepoAccess(repoAccess);

		repo = repoAccess.addRepo("cloned_repo", new RemoteUrl(cloneUrl));
		repoId = repo.getId();
	}

	@AfterEach
	void tearDown() {
		if (dbStorage != null) {
			dbStorage.close();
		}
	}

	@Test
	void testGetCommit() {
		CommitHash hash = new CommitHash(revCommits.get(0).getId().getName());

		final Commit commit = commitAccess.getCommit(repoId, hash);

		assertNotNull(commit);
		assertEquals(revCommits.get(0).getId().getName(), commit.getHash().getHash());
		assertEquals(COMMITS.get(0).getMessage(), commit.getMessage());
	}

	@Test
	void testGetCommits() {
		CommitHash firstHash = new CommitHash(revCommits.get(0).getId().getName());
		CommitHash secondHash = new CommitHash(revCommits.get(1).getId().getName());

		List<CommitHash> hashes = List.of(firstHash, secondHash);

		final Collection<Commit> commits = commitAccess.getCommits(repoId, hashes);
		assertEquals(2, commits.size());

		Commit first = commits.stream().filter(c -> c.getHash().equals(firstHash))
			.findAny().orElseThrow();

		Commit second = commits.stream().filter(c -> c.getHash().equals(secondHash))
			.findAny().orElseThrow();

		assertEquals(COMMITS.get(0).getMessage(), first.getMessage());
		assertEquals(COMMITS.get(1).getMessage(), second.getMessage());
	}

	@Test
	void testStatus() {
		CommitHash firstHash = new CommitHash(revCommits.get(0).getId().getName());
		CommitHash secondHash = new CommitHash(revCommits.get(1).getId().getName());
		assertFalse(commitAccess.isKnown(repoId, firstHash));
		assertFalse(commitAccess.hasKnownCommits(repoId));

		Commit commit = commitAccess.getCommit(repoId, firstHash);
		Commit secondCommit = commitAccess.getCommit(repoId, secondHash);
		assertTrue(commitAccess.getKnownCommits(repoId, List.of(commit, secondCommit)).isEmpty());

		commitAccess.setBenchmarkStatus(repoId, firstHash, NO_BENCHMARK_REQUIRED);
		assertEquals(NO_BENCHMARK_REQUIRED, commitAccess.getBenchmarkStatus(repoId, firstHash));
		assertTrue(commitAccess.isKnown(repoId, firstHash));
		assertTrue(commitAccess.hasKnownCommits(repoId));
		assertEquals(Set.of(firstHash), commitAccess.getKnownCommits(repoId, List.of(commit)));

		commitAccess.setBenchmarkStatus(repoId, firstHash, BENCHMARK_REQUIRED_MANUAL_PRIORITY);
		assertEquals(BENCHMARK_REQUIRED_MANUAL_PRIORITY,
			commitAccess.getBenchmarkStatus(repoId, firstHash));
		assertTrue(commitAccess.getAllTasksOfStatus(repoId, NO_BENCHMARK_REQUIRED).isEmpty());
		assertEquals(List.of(commit),
			commitAccess.getAllTasksOfStatus(repoId, BENCHMARK_REQUIRED_MANUAL_PRIORITY));
		assertEquals(List.of(commit), commitAccess.getAllCommitsRequiringBenchmark());

		commitAccess.makeUnknown(repoId, firstHash);
		assertFalse(commitAccess.isKnown(repoId, firstHash));
		assertFalse(commitAccess.hasKnownCommits(repoId));
		assertTrue(commitAccess.getKnownCommits(repoId, List.of(commit, secondCommit)).isEmpty());
	}

	@Test
	void testGetCommitsBetween() throws IOException, GitAPIException, InterruptedException {
		Thread.sleep(1000);
		Instant begin = Instant.now();
		Thread.sleep(1000);

		String h1 = testRepo.commit(new TestCommit("1")).getId().getName();
		Thread.sleep(1000);
		Instant afterFirst = Instant.now();
		Thread.sleep(1000);

		String h2 = testRepo.commit(new TestCommit("2")).getId().getName();
		Thread.sleep(1000);
		Instant afterSecond = Instant.now();
		Thread.sleep(1000);

		String h3 = testRepo.commit(new TestCommit("3")).getId().getName();
		Instant afterThird = Instant.now().plus(Duration.ofSeconds(2));

		// Collect commits and branches
		repoAccess.fetchOrClone(repo.getId());
		List<String> branches = List.of("master");
		Commit c1 = commitAccess.getCommit(repoId, new CommitHash(h1));
		Commit c2 = commitAccess.getCommit(repoId, new CommitHash(h2));
		Commit c3 = commitAccess.getCommit(repoId, new CommitHash(h3));

		assertEquals(List.of(c1),
			commitAccess.getCommitsBetween(repoId, branches, begin, afterFirst));
		assertEquals(List.of(c2, c1),
			commitAccess.getCommitsBetween(repoId, branches, begin, afterSecond));
		assertEquals(List.of(c3, c2, c1),
			commitAccess.getCommitsBetween(repoId, branches, begin, afterThird));
		assertEquals(List.of(c3, c2),
			commitAccess.getCommitsBetween(repoId, branches, afterFirst, afterThird));
		assertEquals(List.of(),
			commitAccess.getCommitsBetween(repoId, branches, afterThird, Instant.MAX));

		assertEquals(List.of(c3, c2, c1),
			commitAccess.getCommitsBetween(repoId, branches, begin, null));
		assertEquals(List.of(c3, c2),
			commitAccess.getCommitsBetween(repoId, branches, afterFirst, null));
		assertEquals(List.of(c3),
			commitAccess.getCommitsBetween(repoId, branches, afterSecond, null));
	}

}