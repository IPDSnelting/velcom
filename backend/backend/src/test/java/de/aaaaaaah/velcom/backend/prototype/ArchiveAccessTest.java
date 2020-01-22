package de.aaaaaaah.velcom.backend.prototype;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.archive.Archiver;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.repo.exception.AddRepositoryException;
import de.aaaaaaah.velcom.backend.util.DirectoryRemover;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.jgit.internal.storage.file.FileSnapshot;
import org.eclipse.jgit.internal.storage.file.PackFile;
import org.eclipse.jgit.transport.PacketLineIn;
import org.eclipse.jgit.transport.PacketLineOut;
import org.eclipse.jgit.util.FS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class ArchiveAccessTest {

	private static final Path STORAGE_DIR = Paths.get("data/repos_test");

	RepoStorage repoStorage;
	Archiver archiveAccess;

	@BeforeEach
	void setUp() throws IOException {
		if (Files.exists(STORAGE_DIR)) {
			DirectoryRemover.deleteDirectoryRecursive(STORAGE_DIR);
		}

		Files.createDirectory(STORAGE_DIR);

		((Logger) LoggerFactory.getLogger(PacketLineIn.class)).setLevel(Level.OFF);
		((Logger) LoggerFactory.getLogger(FileSnapshot.class)).setLevel(Level.OFF);
		((Logger) LoggerFactory.getLogger(PackFile.class)).setLevel(Level.OFF);
		((Logger) LoggerFactory.getLogger(PacketLineOut.class)).setLevel(Level.OFF);
		((Logger) LoggerFactory.getLogger(FS.class)).setLevel(Level.OFF);

		this.repoStorage = new RepoStorage(STORAGE_DIR);

		this.archiveAccess = new Archiver(this.repoStorage);
	}

	@AfterEach
	void tearDown() throws IOException {
		DirectoryRemover.deleteDirectoryRecursive(STORAGE_DIR);
	}

	@Test
	@Disabled
	void testArchive() throws IOException, AddRepositoryException {
		String url = "https://github.com/leanprover/lean.git";
		CommitHash commitHash = new CommitHash("72a965986fa5aeae54062e98efb3140b2c4e79fd");
		//String url = "https://github.com/kwerber/tiny_repo.git";
		//CommitHash commitHash = new CommitHash("30ca3b15ef3b37f77ff034d897f6d4b26bc34120");

		String dirName = "test_archive_dir";
		repoStorage.addRepository(dirName, url);

		Path archiveFilePath = STORAGE_DIR.resolve("archive_result.tar");

		try (OutputStream out = Files.newOutputStream(archiveFilePath)) {
			long start = System.currentTimeMillis();

			archiveAccess.archive(dirName, commitHash, out, false);

			long end = System.currentTimeMillis();

			System.err.println("Archive process took " + (end - start) + " milliseconds!");
		}
	}

	@Test
	@Disabled
	public void testArchiveOnOtherBranch() throws AddRepositoryException, IOException {
		String url = "https://github.com/kwerber/tiny_repo.git";

		CommitHash commitHash = new CommitHash("30ca3b15ef3b37f77ff034d897f6d4b26bc34120");
		// ^ commit is not on master

		String dirName = "test_archive_on_another_branch_dir";
		repoStorage.addRepository(dirName, url);

		Path archiveFilePath = STORAGE_DIR.resolve("archive_ob_result.tar");

		try (OutputStream out = Files.newOutputStream(archiveFilePath)) {
			long start = System.currentTimeMillis();

			archiveAccess.archive(dirName, commitHash, out, true);

			long end = System.currentTimeMillis();

			System.err.println("Archive process took " + (end - start) + " milliseconds!");
		}
	}

}
