package de.aaaaaaah.velcom.backend.prototype;

import static org.junit.jupiter.api.Assertions.assertTrue;

import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.repo.exception.AddRepositoryException;
import de.aaaaaaah.velcom.backend.storage.repo.exception.RepositoryAcquisitionException;
import de.aaaaaaah.velcom.shared.util.FileHelper;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.StopWalkException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.TrackingRefUpdate;
import org.eclipse.jgit.transport.sshd.DefaultProxyDataFactory;
import org.eclipse.jgit.transport.sshd.JGitKeyCache;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
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
		FileHelper.deleteDirectoryOrFile(this.storageDir);
	}

	@Test
	@Disabled
	public void testSsh() throws GitAPIException {
		SshdSessionFactory factory = new SshdSessionFactory(
			new JGitKeyCache(),
			new DefaultProxyDataFactory()
		);

		SshSessionFactory.setInstance(factory);

		Git git = Git.cloneRepository()
			.setURI("git@git.scc.kit.edu:IPDSnelting/codespeed-runner.git")
			.setDirectory(this.storageDir.resolve("bla").toFile())
			.call();

		git.close();

		System.out.println("done");
	}

	@Test
	@Disabled
	public void testFetch()
		throws AddRepositoryException, RepositoryAcquisitionException, InterruptedException, GitAPIException {

		String url = "https://github.com/kwerber/tiny_repo";

		Path repoDir = storage.addRepository("test_repo", url);

		Thread.sleep(2000);

		try (Repository repo = storage.acquireRepository("test_repo")) {
			ProgressMonitor monitor = new ProgressMonitor() {
				@Override
				public void start(int i) {
					System.err.println("start(" + i + ")");
				}

				@Override
				public void beginTask(String s, int i) {
					System.err.println("beginTask(" + s + ", " + i + ")");
				}

				@Override
				public void update(int i) {
					System.err.println("update(" + i + ")");
				}

				@Override
				public void endTask() {
					System.err.println("endTask()");
				}

				@Override
				public boolean isCancelled() {
					System.err.println("isCancelled()");
					return false;
				}
			};

			FetchResult result = Git.wrap(repo).fetch().setProgressMonitor(monitor).call();

			for (Ref advertisedRef : result.getAdvertisedRefs()) {
				System.out.println("Advertised ref: " + advertisedRef.getName());
			}

			for (TrackingRefUpdate trackingRefUpdate : result.getTrackingRefUpdates()) {
				System.out.println("-- TrackingRefUpdate --------------------------");
				System.out.println(trackingRefUpdate.getLocalName());
				System.out.println(trackingRefUpdate.getRemoteName());
				System.out.println(trackingRefUpdate.getNewObjectId().name());
				System.out.println(trackingRefUpdate.getOldObjectId().name());
				System.out.println(trackingRefUpdate.getResult());
			}
		}

		System.out.println("done");
	}

	@Test
	@Disabled
	public void testReachableCommitsWithMultipleBranches() throws GitAPIException, IOException,
		AddRepositoryException {

		String url = "https://github.com/kwerber/tiny_repo";

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
					System.out.println("-----------------");
					System.out.println(branchId.getName());
					RevCommit revCommit = walk.parseCommit(branchId);
					System.out.println(revCommit.getId().getName());

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
	public void testWalkWithoutBody() throws AddRepositoryException, IOException, GitAPIException {
		String url = "https://github.com/kwerber/tiny_repo";

		Path repoDir = storage.addRepository("test_repo", url);
		assertTrue(Files.exists(repoDir));

		Collection<BranchName> branches = List.of(
			BranchName.fromName("master"),
			BranchName.fromName("otherbranch")
		);

		try (Git git = Git.open(repoDir.toFile())) {
			Repository repo = git.getRepository();

			RevWalk walk = new RevWalk(repo);

			walk.setRetainBody(true);

			// Start the walk from the specified branches
			List<CommitHash> commitList = new ArrayList<>();

			for (Ref branchRef : git.branchList().call()) {
				BranchName branchName = BranchName.fromFullName(branchRef.getName());

				if (branches.contains(branchName)) {
					ObjectId branchObjectId = branchRef.getObjectId();
					RevCommit revCommit = walk.lookupCommit(branchObjectId);

					walk.markStart(revCommit);
				}
			}

			for (RevCommit revCommit : walk) {
				String hashStr = revCommit.getId().getName();
				commitList.add(new CommitHash(hashStr));

				String fullMessage = revCommit.getFullMessage();
				System.out.println(fullMessage);
			}

		}
	}

	@Test
	@Disabled
	public void testReachableCommits() throws GitAPIException, IOException, AddRepositoryException {
		String url = "https://github.com/kwerber/tiny_repo";

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
		String url = "https://github.com/kwerber/tiny_repo";

		Path repoDir = storage.addRepository("test_repo", url);
		assertTrue(Files.exists(repoDir));

		try (Git git = Git.open(repoDir.toFile())) {
			// Iterate over every branch
			for (Ref ref : git.branchList().call()) {
				ObjectId objectId = ref.getObjectId();

				System.out.println("Got branch: " + ref.getName());

				// Iterate over every commit on this branch...
				Iterable<RevCommit> commitIterable = git.log()
					.setRevFilter(new RevFilter() {
						@Override
						public boolean include(RevWalk walker, RevCommit cmit)
							throws StopWalkException, MissingObjectException, IncorrectObjectTypeException, IOException {
							return false;
						}

						@Override
						public RevFilter clone() {
							return null;
						}
					})
					.add(objectId).call();

				commitIterable.forEach(revCommit ->
					System.out.println("Got commit: " + revCommit.getShortMessage()));
			}
		}
	}

	@Test
	@Disabled
	public void testCommitAccess() throws IOException, AddRepositoryException {
		String url = "https://github.com/kwerber/tiny_repo";

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

				System.out.println(commit.getParentCount());

				for (RevCommit parentCommit : commit.getParents()) {
					System.out.println(parentCommit.getId().getName());
				}
			}
		}
	}

	@Test
	@Disabled
	public void testLocalClone() throws GitAPIException, AddRepositoryException {
		String url = "https://github.com/kwerber/tiny_repo";

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
