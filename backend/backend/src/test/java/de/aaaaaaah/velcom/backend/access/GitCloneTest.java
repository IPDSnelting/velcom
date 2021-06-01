package de.aaaaaaah.velcom.backend.access;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

/**
 * Tests jgit's behaviour when cloning repos with submodules.
 */
class GitCloneTest {

	@TempDir
	Path tempDir;

	private Path repoPath;
	private Path submodulePath;

	@BeforeEach
	void setUp() throws Exception {
		((Logger) LoggerFactory.getLogger("org.eclipse.jgit")).setLevel(Level.INFO);

		Files.createDirectories(tempDir);
		repoPath = tempDir.resolve("repo");
		submodulePath = tempDir.resolve("sub_module");

		// Init repos
		Git.init().setDirectory(repoPath.toFile()).call();
		Git.init().setDirectory(submodulePath.toFile()).call();

		// Write file to submodule
		Files.writeString(submodulePath.resolve("test.txt"), "Version 1");

		// Commit it
		Git subGit = Git.open(submodulePath.toFile());
		disableGc(subGit);
		subGit.add().addFilepattern("test.txt").call();
		subGit.commit().setMessage("Hey").setAuthor("Aith", "er").call();

		// Add submodule to repo
		Git repoGit = Git.open(repoPath.toFile());
		disableGc(repoGit);

		repoGit
			.submoduleAdd()
			.setName("Submodule")
			.setPath("submodule")
			.setURI(submodulePath.toUri().toString())
			.call();

		repoGit.commit().setAuthor("Auth", "er").setMessage("Init").call();

		// Modify submodule
		Files.writeString(submodulePath.resolve("test.txt"), "Version 2!");
		subGit.add().addFilepattern("test.txt").call();
		subGit.commit().setMessage("Updated version").setAuthor("Author", "dude").call();

		// Update submodule in original repo
		repoGit.submoduleSync().call();
		SubmoduleWalk submoduleWalk = SubmoduleWalk.forIndex(repoGit.getRepository());
		while (submoduleWalk.next()) {
			Git.wrap(submoduleWalk.getRepository()).pull().setStrategy(MergeStrategy.THEIRS).call();
		}
		repoGit.add().addFilepattern("submodule").call();
		// Commit modification
		repoGit.commit().setAuthor("Auth", "er").setMessage("Updated submodule").call();

		Git.init().setDirectory(tempDir.resolve("bench_repo").toFile()).call();
	}

	private void disableGc(Git repoGit) throws IOException {
		StoredConfig config = repoGit.getRepository().getConfig();
		config.setInt("gc", null, "auto", 0);
		config.save();
	}

	@Test
	void verifySubmodulesClonedCorrectlyInOriginal() throws Exception {
		assertThat(Git.open(repoPath.toFile()).status().call().isClean())
			.withFailMessage("The repo was not clean!")
			.isTrue();
		assertThat(Files.readString(repoPath.resolve("submodule/test.txt")))
			.isEqualTo("Version 2!");
	}

	@Test
	void checkoutCorrectSubmoduleVersion() throws Exception {
		Git repo = Git.open(repoPath.toFile());
		repo.checkout()
			.setName(repo.getRepository().resolve("HEAD^").toObjectId().getName())
			.setForced(true)
			.call();

		repo.submoduleSync().call();
		repo.submoduleUpdate().setStrategy(MergeStrategy.THEIRS).setFetch(true).call();

		assertThat(Files.readString(repoPath.resolve("submodule").resolve("test.txt")))
			.isEqualTo("Version 1");
	}
}
