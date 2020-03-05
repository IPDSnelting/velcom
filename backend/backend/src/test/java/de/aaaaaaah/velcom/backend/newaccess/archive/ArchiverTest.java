package de.aaaaaaah.velcom.backend.newaccess.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.aaaaaaah.velcom.backend.newaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.util.DirectoryRemover;
import de.aaaaaaah.velcom.runner.shared.util.compression.TarHelper;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;

class ArchiverTest {

	@TempDir
	Path tempDir;

	private Path repoPath;
	private Path garbageDir;
	private Path archivesRootDir;

	private Archiver archiver;
	public static final String SUBMODULE_PATH = "submodule";

	@BeforeEach
	void setUp() throws Exception {
		((Logger) LoggerFactory.getLogger("org.eclipse.jgit")).setLevel(Level.INFO);

		Files.createDirectories(tempDir);
		repoPath = tempDir.resolve("repo");
		Path submodulePath = tempDir.resolve("sub_module");
		garbageDir = tempDir.resolve("garbage");
		Files.createDirectory(garbageDir);
		archivesRootDir = tempDir.resolve("archives_root");
		Files.createDirectory(archivesRootDir);

		// Init repos
		Git.init().setDirectory(repoPath.toFile()).call();
		Git.init().setDirectory(submodulePath.toFile()).call();

		// Write file to submodule
		Files.writeString(submodulePath.resolve("test.txt"), "Version 1");

		// commit it
		Git subGit = Git.open(submodulePath.toFile());
		subGit.add().addFilepattern("test.txt").call();
		subGit.commit().setMessage("Hey").setAuthor("Aith", "er").call();

		// Add submodule to repo
		Git repoGit = Git.open(repoPath.toFile());
		repoGit
			.submoduleAdd()
			.setName("submodule")
			.setPath(SUBMODULE_PATH)
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
		repoGit.add().addFilepattern(SUBMODULE_PATH).call();
		// commit modification
		repoGit.commit().setAuthor("Auth", "er").setMessage("Updated submodule").call();

		Git.init().setDirectory(tempDir.resolve("bench_repo").toFile()).call();

		RepoStorage repoStorage = mock(RepoStorage.class);
		when(repoStorage.acquireRepository("repo")).thenReturn(repoGit.getRepository());
		when(repoStorage.getRepoDir("repo")).thenReturn(repoPath.toAbsolutePath());
		this.archiver = new Archiver(repoStorage, archivesRootDir);
	}

	@AfterEach
	void tearDown() throws IOException {
		DirectoryRemover.deleteDirectoryRecursive(garbageDir);
	}

	@Test
	void verifySubmodulesClonedCorrectlyInOriginal() throws Exception {
		assertThat(Git.open(repoPath.toFile()).status().call().isClean())
			.withFailMessage("The repo was not clean!")
			.isTrue();
		assertThat(Files.readString(repoPath.resolve(SUBMODULE_PATH + "/test.txt")))
			.isEqualTo("Version 2!");
	}

	@ParameterizedTest
	@CsvSource({
		"HEAD, Version 2!",
		"HEAD^, Version 1"
	})
	void cloneWorksForHead(String commit, String version) throws Exception {
		Path out = garbageDir.resolve("out");
		ObjectId head = Git.open(repoPath.toFile()).getRepository().resolve(commit);
		try (OutputStream outputStream = Files.newOutputStream(out)) {
			archiver.archive("repo", new CommitHash(head.getName()), outputStream, true);
		}

		Path outDir = out.resolveSibling("out_dir");
		TarHelper.untar(out, outDir);

		assertThat(Git.open(outDir.toFile()).status().call().isClean())
			.withFailMessage("The repo was not clean!")
			.isTrue();
		assertThat(Files.readString(outDir.resolve(SUBMODULE_PATH + "/test.txt")))
			.isEqualTo(version);
	}
}