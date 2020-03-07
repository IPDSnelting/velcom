package de.aaaaaaah.velcom.backend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

public class TestRepo {

	private final Path repoDir;
	private final Set<String> branches = new HashSet<>();
	private final Map<TestCommit, String> commitHashMap = new HashMap<>();
	private final Map<TestCommit, RevCommit> revCommitMap = new HashMap<>();
	private final Map<String, RevCommit> revByHashMap = new HashMap<>();
	private String currentBranch = "master";

	public TestRepo(Path repoDir) throws GitAPIException, IOException {
		Objects.requireNonNull(repoDir);
		this.repoDir = repoDir;
		this.branches.add("master");

		if (!Files.exists(repoDir)) {
			Files.createDirectory(repoDir);
		}
		Git.init().setBare(false).setDirectory(repoDir.toFile()).call();
	}

	public TestRepo(Path repoDir, TestCommit[] commits) throws GitAPIException, IOException {
		this(repoDir);
		for (TestCommit commit : commits) {
			this.commit(commit);
		}
	}

	public TestRepo(Path repoDir, List<TestCommit> commits) throws GitAPIException, IOException {
		this(repoDir);
		for (TestCommit commit : commits) {
			this.commit(commit);
		}
	}

	public Optional<String> getCommitHash(TestCommit testCommit) {
		return Optional.ofNullable(commitHashMap.get(testCommit));
	}

	public Optional<RevCommit> getRevCommit(TestCommit testCommit) {
		return Optional.ofNullable(revCommitMap.get(testCommit));
	}

	public Optional<RevCommit> getRevCommit(String commitHash) {
		return Optional.ofNullable(revByHashMap.get(commitHash));
	}

	public void commit(TestCommit[] commits) throws IOException, GitAPIException {
		for (TestCommit commit : commits) {
			this.commit(commit);
		}
	}

	public RevCommit commit(TestCommit commit)
		throws IOException, GitAPIException {

		final String branch = commit.getBranch().orElse("master");
		final PersonIdent author;

		if (commit.getAuthorDate().isPresent()) {
			Date date = Date.from(commit.getAuthorDate().get());
			author = new PersonIdent("peter", "peter@email.com", date, TimeZone.getDefault());
		} else {
			author = new PersonIdent("peter", "peter@gmail.com");
		}

		// Checkout correct branch
		try (Git git = Git.open(repoDir.toFile())) {
			if (!currentBranch.equals(branch)) {
				if (branches.contains(branch)) {
					// branch already exists
					git.checkout().setName(branch).call();
				} else {
					// new branch
					git.checkout().setName(branch).setCreateBranch(true).call();
					branches.add(branch);
				}
				currentBranch = branch;
			}

			// Write file and stage it
			Files.write(
				repoDir.resolve(commit.getFile()),
				commit.getContent().getBytes(),
				StandardOpenOption.CREATE
			);

			git.add().addFilepattern(commit.getFile()).call();

			// Create the commit
			RevCommit revCommit = git.commit()
				.setAuthor(author)
				.setCommitter("peter", "peter@email.com")
				.setMessage(commit.getMessage())
				.call();

			this.commitHashMap.put(commit, revCommit.getId().getName());
			this.revCommitMap.put(commit, revCommit);
			this.revByHashMap.put(revCommit.getId().getName(), revCommit);

			return revCommit;
		}
	}

}
