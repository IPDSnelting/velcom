package de.aaaaaaah.velcom.backend.storage.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.aaaaaaah.velcom.backend.TestCommit;
import de.aaaaaaah.velcom.backend.TestRepo;
import de.aaaaaaah.velcom.backend.storage.repo.exception.AddRepositoryException;
import de.aaaaaaah.velcom.backend.storage.repo.exception.NoSuchRepositoryException;
import de.aaaaaaah.velcom.backend.storage.repo.exception.RepositoryAcquisitionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepoStorageTest {

	static final List<TestCommit> commits = List.of(
		new TestCommit("Add abc", "abc.txt", "Hello World!"),
		new TestCommit("Add blablub", "blablub.txt", "123123123"),
		new TestCommit("Modify abc", "abc.txt", "Hello World! Test 123")
	);
	static final TestCommit headTestCommit = commits.get(commits.size() - 1);

	@TempDir
	Path testDir;
	Path repoStorageDir;
	Path testRepoDir;

	String cloneUrl;
	String dirName = "tiny_repo";

	RepoStorage repoStorage;

	@BeforeEach
	void setUp() throws IOException, GitAPIException {
		this.repoStorageDir = testDir.resolve("repos");
		this.testRepoDir = testDir.resolve("custom_test_repo");
		this.repoStorage = new RepoStorage(repoStorageDir);
		this.cloneUrl = "file://" + testRepoDir.toString();

		new TestRepo(testRepoDir, commits);
	}

	@Test
	void testAddRepository() throws AddRepositoryException, IOException {
		Path tinyRepoPath = repoStorage.addRepository(dirName, cloneUrl);

		assertTrue(Files.exists(tinyRepoPath));
		assertTrue(Files.isDirectory(tinyRepoPath));
		assertTrue(repoStorage.containsRepository(dirName));
		assertEquals(dirName, tinyRepoPath.getFileName().toString());

		// Make sure testRepoPath is actually a repository
		try (Git git = Git.open(tinyRepoPath.toFile())) {
			Repository repo = git.getRepository();

			final ObjectId headPtr = repo.resolve("HEAD");

			// And that at least the head commit exists and seems right
			try (RevWalk walk = new RevWalk(repo)) {
				final RevCommit commit = walk.parseCommit(headPtr);

				assertEquals(headTestCommit.getMessage(), commit.getFullMessage());
			}
		}
	}

	@Test
	void testContainsAndDelete() throws AddRepositoryException, IOException {
		assertFalse(repoStorage.containsRepository(dirName));

		Path path = repoStorage.addRepository(dirName, cloneUrl);

		assertEquals(repoStorageDir.resolve(dirName), path);
		assertTrue(Files.exists(path));
		assertTrue(Files.isDirectory(path));
		assertTrue(repoStorage.containsRepository(dirName));

		repoStorage.deleteRepository(dirName);

		assertTrue(Files.notExists(path));
		assertFalse(repoStorage.containsRepository(dirName));
	}

	@Test
	void testGetRepoDirectories() throws AddRepositoryException, IOException {
		assertTrue(repoStorage.getRepoDirectories().isEmpty());
		repoStorage.addRepository(dirName, cloneUrl);
		assertEquals(repoStorage.getRepoDirectories().size(), 1);

		repoStorage.addRepository(dirName + "_2", cloneUrl);

		assertEquals(repoStorage.getRepoDirectories().size(), 2);
		assertTrue(
			repoStorage.getRepoDirectories().stream()
				.anyMatch(p -> p.getFileName().endsWith(dirName))
		);
		assertTrue(
			repoStorage.getRepoDirectories().stream()
				.anyMatch(p -> p.getFileName().endsWith(dirName + "_2"))
		);

		repoStorage.deleteRepository(dirName);

		assertEquals(repoStorage.getRepoDirectories().size(), 1);
		assertFalse(
			repoStorage.getRepoDirectories().stream()
				.anyMatch(p -> p.getFileName().endsWith(dirName))
		);
		assertTrue(
			repoStorage.getRepoDirectories().stream()
				.anyMatch(p -> p.getFileName().endsWith(dirName + "_2"))
		);

		repoStorage.deleteRepository(dirName + "_2");

		assertTrue(repoStorage.getRepoDirectories().isEmpty());
	}

	@Test
	void testGetRepoDir() throws AddRepositoryException, IOException {
		assertThrows(NoSuchRepositoryException.class, () -> repoStorage.getRepoDir(dirName));
		repoStorage.addRepository(dirName, cloneUrl);

		Path repoDir = repoStorage.getRepoDir(dirName);
		assertEquals(repoStorageDir.resolve(dirName), repoDir);
		assertTrue(Files.exists(repoDir));
		assertTrue(Files.isDirectory(repoDir));

		repoStorage.deleteRepository(dirName);
		assertThrows(NoSuchRepositoryException.class, () -> repoStorage.getRepoDir(dirName));
	}

	@Test
	public void testAcquire()
		throws AddRepositoryException, RepositoryAcquisitionException, IOException {

		repoStorage.addRepository(dirName, cloneUrl);

		try (Repository repo = repoStorage.acquireRepository(dirName)) {
			final ObjectId headPtr = repo.resolve("HEAD");

			try (RevWalk walk = new RevWalk(repo)) {
				final RevCommit commit = walk.parseCommit(headPtr);

				assertEquals(headTestCommit.getMessage(), commit.getFullMessage());
			}
		}

		repoStorage.deleteRepository(dirName);
		assertThrows(NoSuchRepositoryException.class, () -> repoStorage.acquireRepository(dirName));
	}

	@Test
	public void testAcquireWithLambda()
		throws AddRepositoryException, RepositoryAcquisitionException, IOException {

		repoStorage.addRepository(dirName, cloneUrl);

		repoStorage.acquireRepository(dirName, repo -> {
			final ObjectId headPtr = repo.resolve("HEAD");

			try (RevWalk walk = new RevWalk(repo)) {
				final RevCommit commit = walk.parseCommit(headPtr);

				assertEquals(headTestCommit.getMessage(), commit.getFullMessage());
			}
		});

		repoStorage.deleteRepository(dirName);
		assertThrows(NoSuchRepositoryException.class, () -> {
			repoStorage.acquireRepository(dirName, repo -> {
				throw new AssertionError();
			});
		});
	}

}