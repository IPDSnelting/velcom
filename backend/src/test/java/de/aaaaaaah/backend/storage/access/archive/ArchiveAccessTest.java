package de.aaaaaaah.backend.storage.access.archive;

import de.aaaaaaah.designproto.backend.access.commit.CommitHash;
import de.aaaaaaah.designproto.backend.access.repo.RepoId;
import de.aaaaaaah.designproto.backend.access.repo.archive.ArchiveException;
import de.aaaaaaah.designproto.backend.access.repo.archive.Archiver;
import de.aaaaaaah.designproto.backend.storage.repo.RepoStorage;
import de.aaaaaaah.designproto.backend.storage.repo.exception.AddRepositoryException;
import de.aaaaaaah.designproto.backend.util.DirectoryRemover;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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

		this.repoStorage = new RepoStorage(STORAGE_DIR);

		this.archiveAccess = new Archiver(this.repoStorage);
	}

	@AfterEach
	void tearDown() throws IOException {
		DirectoryRemover.deleteDirectoryRecursive(STORAGE_DIR);
	}

	@Test
	@Disabled
	void testArchive() throws IOException, AddRepositoryException, ArchiveException {
		URI url = URI.create("https://github.com/leanprover/lean.git");
		CommitHash commitHash = new CommitHash("72a965986fa5aeae54062e98efb3140b2c4e79fd");

		RepoId repoId = new RepoId();
		String dirName = repoId.getDirectoryName();
		repoStorage.addRepository(dirName, url);

		Path archiveFilePath = STORAGE_DIR.resolve("archive_result.tar");

		try (OutputStream out = Files.newOutputStream(archiveFilePath)) {
			long start = System.currentTimeMillis();

			archiveAccess.archive(dirName, commitHash, out, false);

			long end = System.currentTimeMillis();

			System.err.println("Archive process took " + (end - start) + " milliseconds!");
		}
	}

}