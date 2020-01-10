package de.aaaaaaah.backend.storage.repo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.repo.exception.AddRepositoryException;
import de.aaaaaaah.velcom.backend.util.DirectoryRemover;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class RepoStorageTest {

	private Path storageDir;
	private RepoStorage storage;

	@BeforeEach
	void setUp() throws IOException {

		this.storageDir = Paths.get("data/repos_test");
		Files.createDirectory(this.storageDir);

		this.storage = new RepoStorage(this.storageDir);
	}

	@AfterEach
	void tearDown() throws IOException {
		DirectoryRemover.deleteDirectoryRecursive(this.storageDir);
	}

	@Test
	@Disabled
	public void testReachableCommitsWithMultipleBranches() throws GitAPIException, IOException,
		AddRepositoryException {

		URI url = URI.create("https://github.com/kwerber/tiny_repo");

		Path repoDir = storage.addRepository("test_repo", url);
		assertTrue(Files.exists(repoDir));

		try (Git git = Git.open(repoDir.toFile())) {
			Repository repository = git.getRepository();

			List<ObjectId> branches = new ArrayList<>();

			git.branchList().call().forEach(ref -> branches.add(ref.getObjectId()));

			try (RevWalk walk = new RevWalk(repository)) {
				walk.setRetainBody(false); // Ignore commit body

				// Get RevCommit for each branch
				List<RevCommit> branchCommits = new ArrayList<>();
				for (ObjectId branchId : branches) {
					branchCommits.add(walk.parseCommit(branchId));
				}

				// Start at all branch rev commits
				walk.markStart(branchCommits);

				RevCommit current;

				while ((current = walk.next()) != null) {
					System.out.println("-> " + current.getId().getName());
				}
			}
		}
	}

	@Test
	@Disabled
	public void testReachableCommits() throws GitAPIException, IOException, AddRepositoryException {
		URI url = URI.create("https://github.com/kwerber/tiny_repo");

		Path repoDir = storage.addRepository("test_repo", url);
		assertTrue(Files.exists(repoDir));

		try (Git git = Git.open(repoDir.toFile())) {
			Repository repository = git.getRepository();

			// Iterate over every branch
			for (Ref ref : git.branchList().call()) {
				ObjectId objectId = ref.getObjectId();

				System.out.println("Got branch: " + ref.getName());

				try (RevWalk walk = new RevWalk(repository)) {
					walk.setRetainBody(false);

					RevCommit branchCommit = walk.parseCommit(objectId);

					// Iterate over every commit on this branch...
					walk.markStart(branchCommit);

					RevCommit current;

					while ((current = walk.next()) != null) {
						System.out.println("-> " + current.getId().getName());
					}
				}
			}
		}
	}

	@Test
	@Disabled
	public void testCommitIteration() throws IOException, GitAPIException, AddRepositoryException {
		URI url = URI.create("https://github.com/kwerber/tiny_repo");

		Path repoDir = storage.addRepository("test_repo", url);
		assertTrue(Files.exists(repoDir));

		try (Git git = Git.open(repoDir.toFile())) {
			// Iterate over every branch
			for (Ref ref : git.branchList().call()) {
				ObjectId objectId = ref.getObjectId();

				System.out.println("Got branch: " + ref.getName());

				// Iterate over every commit on this branch...
				Iterable<RevCommit> commitIterable = git.log().add(objectId).call();

				commitIterable.forEach(revCommit ->
					System.out.println("Got commit: " + revCommit.getShortMessage()));
			}
		}
	}

	@Test
	@Disabled
	public void testCommitAccess() throws IOException, AddRepositoryException {
		URI url = URI.create("https://github.com/kwerber/tiny_repo");

		Path repoDir = storage.addRepository("test_repo", url);
		assertTrue(Files.exists(repoDir));

		try (Git git = Git.open(repoDir.toFile())) {
			Repository repo = git.getRepository();

			ObjectId commitPtr = repo.resolve("63eed6f5853e61812a761dd21eb3ecfe71a0530c");

			try (RevWalk revWalk = new RevWalk(repo)) {
				RevCommit commit = revWalk.parseCommit(commitPtr);

				System.out.println(commit);
				System.out.println(commit.getFullMessage());
				System.out.println(commit.getAuthorIdent().getName()
					+ " <" + commit.getAuthorIdent().getEmailAddress() + ">");
				System.out.println(commit.getAuthorIdent().getWhen());
				System.out.println(commit.getCommitterIdent().getName()
					+ " <" + commit.getCommitterIdent().getEmailAddress() + ">");
				System.out.println(commit.getCommitterIdent().getWhen());

			}
		}
	}

	@Test
	@Disabled
	public void testLocalClone() throws IOException, GitAPIException, AddRepositoryException {
		URI url = URI.create("https://github.com/kwerber/tiny_repo");

		Path repoDir = storage.addRepository("test_repo", url);

		URI uri = repoDir.toUri();

		try (Git clonedGit = Git.cloneRepository()
			.setBare(false)
			.setURI(uri.toString())
			.setDirectory(repoDir.resolve("clones").resolve("test").toFile())
			.call()) {

			clonedGit.checkout().setName("f3c12144172b81880e39c274f74daa53d76797a5").call();
		}
	}

}
