package de.aaaaaaah.velcom.backend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

public class TestRepo {

	private final Path repoDir;

	public TestRepo(Path repoDir) throws GitAPIException, IOException {
		this.repoDir = repoDir;
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
		return commit(commit.getMessage(), commit.getFile(), commit.getContent());
	}

	public RevCommit commit(String message, String file, String content)
		throws IOException, GitAPIException {

		try (Git git = Git.open(repoDir.toFile())) {
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
