package de.aaaaaaah.velcom.backend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

public class TestRepo {

	private final Path repoDir;
	private final Set<String> branches = new HashSet<>();
	private String currentBranch = "master";

	public TestRepo(Path repoDir) throws GitAPIException, IOException {
		this.repoDir = repoDir;
		this.branches.add("master");

		if (!Files.exists(repoDir)) {
			Files.createDirectory(repoDir);
		}
		Git.init().setBare(false).setDirectory(repoDir.toFile()).call();
	}

	public TestRepo(Path repoDir, List<TestCommit> commits) throws GitAPIException, IOException {
		this(repoDir);
		for (TestCommit commit : commits) {
			this.commit(commit);
		}
	}

	public RevCommit commit(TestCommit commit) throws IOException, GitAPIException {
		if (commit.getBranch() == null) {
			return commit(commit.getMessage(), commit.getFile(), commit.getContent());
		} else {
			return commit(commit.getMessage(), commit.getFile(), commit.getContent(),
				commit.getBranch());
		}
	}

	public RevCommit commit(String message, String file, String content)
		throws IOException, GitAPIException {
		return this.commit(message, file, content, "master");
	}

	public RevCommit commit(String message, String file, String content, String branch)
		throws IOException, GitAPIException {

		try (Git git = Git.open(repoDir.toFile())) {
			if (!currentBranch.equals(branch)) {
				if (branches.contains(branch)) {
					// branch already exists
					git.checkout().setName(branch).call();
					currentBranch = branch;
				} else {
					// new branch
					git.checkout().setName(branch).setCreateBranch(true).call();
					branches.add(branch);
					currentBranch = branch;
				}
			}

			Files.write(repoDir.resolve(file), content.getBytes(), StandardOpenOption.CREATE);

			git.add().addFilepattern(file).call();

			return git.commit()
				.setAuthor("Peter", "peter@email.com")
				.setCommitter("Peter", "peter@email.com")
				.setMessage(message)
				.call();
		}
	}

}
